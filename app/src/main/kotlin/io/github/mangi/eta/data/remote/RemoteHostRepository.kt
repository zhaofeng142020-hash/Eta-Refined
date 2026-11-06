/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 远程主机配置仓库：DataStore 存主机列表与开关，Android Keystore 加密存凭据。
 * 凭据（launchToken / cookie / accessToken / refreshToken / X25519 私钥）绝不写日志、不回显。
 */
package io.github.mangi.eta.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.mangi.eta.agent.remote.RemoteHostConfig
import io.github.mangi.eta.agent.remote.RemoteHostDefaults
import io.github.mangi.eta.agent.remote.RemoteHostKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.remoteDataStore: DataStore<Preferences> by preferencesDataStore(name = "eta_refined_remote_hosts_v1")

class RemoteHostRepository(private val context: Context) {
    private val appContext = context.applicationContext
    private val credentials = RemoteCredentialStore(appContext)

    init { INSTANCE = this }

    private val HOSTS = stringPreferencesKey("remote_hosts_v1")
    private val ACTIVE = stringPreferencesKey("remote_active_host_id")
    private val RELAY_ENABLED = booleanPreferencesKey("remote_relay_server_enabled")
    private val RELAY_URL = stringPreferencesKey("remote_relay_server_url")

    fun hosts(): Flow<List<RemoteHostConfig>> = appContext.remoteDataStore.data.map { prefs ->
        parseHosts(prefs[HOSTS] ?: "[]")
    }

    suspend fun upsert(config: RemoteHostConfig) {
        appContext.remoteDataStore.edit { prefs ->
            val list = parseHosts(prefs[HOSTS] ?: "[]").toMutableList()
            list.removeAll { it.id == config.id }
            list.add(config)
            prefs[HOSTS] = encodeHosts(list)
        }
    }

    suspend fun remove(id: String) {
        credentials.clear(id)
        appContext.remoteDataStore.edit { prefs ->
            prefs[HOSTS] = encodeHosts(parseHosts(prefs[HOSTS] ?: "[]").filterNot { it.id == id })
            if (prefs[ACTIVE] == id) prefs.remove(ACTIVE)
        }
    }

    suspend fun activeHostId(): String? = appContext.remoteDataStore.data.first()[ACTIVE]

    suspend fun setActiveHostId(id: String?) {
        appContext.remoteDataStore.edit { prefs ->
            if (id == null) prefs.remove(ACTIVE) else prefs[ACTIVE] = id
        }
    }

    suspend fun host(id: String): RemoteHostConfig? = hosts().first().firstOrNull { it.id == id }

    suspend fun saveLaunchToken(hostId: String, token: String) = credentials.put(hostId, "launch_token", token)
    suspend fun credential(hostId: String, key: String): String? = credentials.get(hostId, key)
    suspend fun saveCredential(hostId: String, key: String, value: String) = credentials.put(hostId, key, value)
    suspend fun clearCredentials(hostId: String) = credentials.clear(hostId)

    fun relayServerEnabled(): Flow<Boolean> = appContext.remoteDataStore.data
        .map { it[RELAY_ENABLED] ?: true }

    suspend fun setRelayServerEnabled(enabled: Boolean) {
        appContext.remoteDataStore.edit { it[RELAY_ENABLED] = enabled }
    }

    fun relayServerUrl(): Flow<String> = appContext.remoteDataStore.data
        .map { it[RELAY_URL] ?: RemoteHostDefaults.RELAY_SERVER_URL }

    suspend fun setRelayServerUrl(url: String) {
        appContext.remoteDataStore.edit { it[RELAY_URL] = url }
    }

    private fun parseHosts(text: String): List<RemoteHostConfig> = runCatching {
        val array = JSONArray(text)
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            RemoteHostConfig(
                id = o.optString("id"),
                kind = runCatching { RemoteHostKind.valueOf(o.optString("kind")) }.getOrDefault(RemoteHostKind.DIRECT),
                displayName = o.optString("displayName"),
                baseUrl = o.optString("baseUrl"),
                serverUrl = o.optString("serverUrl"),
                enabled = o.optBoolean("enabled", true),
            )
        }.filter { it.id.isNotEmpty() }
    }.getOrDefault(emptyList())

    private fun encodeHosts(list: List<RemoteHostConfig>): String = JSONArray().apply {
        list.forEach { c ->
            put(JSONObject()
                .put("id", c.id)
                .put("kind", c.kind.name)
                .put("displayName", c.displayName)
                .put("baseUrl", c.baseUrl)
                .put("serverUrl", c.serverUrl)
                .put("enabled", c.enabled))
        }
    }.toString()

    companion object {
        @Volatile
        private var INSTANCE: RemoteHostRepository? = null
        fun get(): RemoteHostRepository = INSTANCE
            ?: error("RemoteHostRepository 尚未初始化（进程重启后需在 UI 创建实例）")
    }
}

/** Keystore AES/GCM 加密的键值凭据，逐主机、逐 key 独立文件；不写日志、不进备份。 */
private class RemoteCredentialStore(context: Context) {
    private val dir = File(context.applicationContext.noBackupFilesDir, "remote_host_credentials")
    private val transformation = "AES/GCM/NoPadding"
    private val keyAlias = "eta_refined_remote_credentials_v1"

    fun put(hostId: String, key: String, value: String) {
        val file = file(hostId, key)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(create = true))
        cipher.updateAAD("$hostId|$key".toByteArray(Charsets.UTF_8))
        val plaintext = value.toByteArray(Charsets.UTF_8)
        val payload = cipher.iv + cipher.doFinal(plaintext)
        AtomicFile(file).let { atomic ->
            val stream = atomic.startWrite()
            try {
                stream.write(payload)
                atomic.finishWrite(stream)
            } catch (error: Exception) {
                atomic.failWrite(stream)
                throw error
            }
        }
    }

    fun get(hostId: String, key: String): String? {
        val file = file(hostId, key)
        if (!file.exists()) return null
        return runCatching {
            val payload = file.readBytes()
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(create = false), GCMParameterSpec(128, payload, 0, 12))
            cipher.updateAAD("$hostId|$key".toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(payload, 12, payload.size - 12), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear(hostId: String) {
        dir.listFiles()?.filter { it.name.startsWith(sha(hostId)) }?.forEach { it.delete() }
    }

    private fun file(hostId: String, key: String) = File(dir, "${sha(hostId)}_${sha(key)}.bin")

    private fun sha(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun secretKey(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        if (!create) throw IllegalStateException("credential key unavailable")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
            generateKey()
        }
    }

    init { dir.mkdirs() }
}
