/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * Noise_IK_25519_ChaChaPoly_SHA256 的最小握手骨架（JCA 实现）。
 *
 * 注意：本轮为协议骨架，未与真实 Host 做联调、也未跑 ds-harness-remote 的 golden 加密向量。
 * 平台不支持所需算法时抛出 UNSUPPORTED_CRYPTO 并给出明确提示，绝不静默降级为明文。
 * 协议名与 prologue 格式参考 dsh-Go packages/dsh_remote（AGPL-3.0）/ ds-harness-remote（MIT）公开事实。
 */
package io.github.mangi.eta.agent.remote

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class NoiseIk private constructor(
    private val staticPrivate: PrivateKey,
    val staticPublic: PublicKey,
) {
    /** 客户端静态公钥的 48-bit pin 指纹，格式 ABCD EF01 2345。 */
    val staticPin: String = fingerprint(staticPublic.encoded)

    fun seal(peerStatic: PublicKey, prologue: ByteArray, plaintext: ByteArray): ByteArray {
        val ephemeral = KeyPairGenerator.getInstance("X25519").generateKeyPair()
        val agreement = KeyAgreement.getInstance("X25519")
        agreement.init(ephemeral.private)
        agreement.doPhase(peerStatic, true)
        val shared = agreement.generateSecret()
        val key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(shared + prologue), "ChaCha20")
        val nonce = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding")
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        } catch (_: Exception) {
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(nonce))
        }
        cipher.updateAAD(prologue)
        val ciphertext = cipher.doFinal(plaintext)
        return nonce + ephemeral.public.encoded + ciphertext
    }

    fun open(peerStatic: PublicKey, prologue: ByteArray, payload: ByteArray): ByteArray {
        require(payload.size > 12 + 32) {
            throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Noise 密文过短")
        }
        val nonce = payload.copyOfRange(0, 12)
        val ephEncoded = payload.copyOfRange(12, 12 + 32)
        val ciphertext = payload.copyOfRange(12 + 32, payload.size)
        val peerEphemeral = KeyFactory.getInstance("X25519")
            .generatePublic(X509EncodedKeySpec(ephEncoded))
        val agreement = KeyAgreement.getInstance("X25519")
        agreement.init(staticPrivate)
        agreement.doPhase(peerEphemeral, true)
        val shared = agreement.generateSecret()
        val key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(shared + prologue), "ChaCha20")
        val cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding")
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
        } catch (_: Exception) {
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(nonce))
        }
        cipher.updateAAD(prologue)
        return cipher.doFinal(ciphertext)
    }

    fun decodePublicKey(encoded: String): PublicKey =
        KeyFactory.getInstance("X25519").generatePublic(
            X509EncodedKeySpec(Base64.getUrlDecoder().decode(encoded)),
        )

    companion object {
        fun prologue(connectionId: String, hostId: String, clientId: String): ByteArray =
            "DSH-REMOTE\u0000v=1\u0000connection=$connectionId\u0000host=$hostId\u0000client=$clientId"
                .toByteArray(Charsets.UTF_8)

        fun fingerprint(encoded: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(encoded).copyOf(6)
            val hex = digest.joinToString("") { "%02X".format(it) }
            return hex.chunked(4).joinToString(" ")
        }

        fun generate(): NoiseIk = try {
            val pair = KeyPairGenerator.getInstance("X25519").generateKeyPair()
            NoiseIk(pair.private, pair.public)
        } catch (error: Exception) {
            throw RemoteException(
                RemoteErrorCodes.UNSUPPORTED_CRYPTO,
                remoteErrorHint(RemoteErrorCodes.UNSUPPORTED_CRYPTO),
            )
        }

        fun encodePublic(key: PublicKey): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(key.encoded)
    }
}
