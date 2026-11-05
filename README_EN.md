<h1 align="center">Eta-Refined</h1>

<p align="center"><a href="README.md">Chinese (Simplified)</a> | <strong>English</strong></p>

<p align="center"><strong>A system-level AI assistant for Android, with the UI/UX rebuilt on top of Mangi-11/Eta</strong></p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-13%2B%20%7C%20arm64-3DDC84?logo=android&logoColor=white" alt="Android 13+ | arm64">
  <img src="https://img.shields.io/badge/License-PolyForm%20Noncommercial%201.0.0-1677FF" alt="PolyForm Noncommercial 1.0.0">
  <img src="https://img.shields.io/badge/Star-Eta--Refined-1677FF?logo=github" alt="Star Eta-Refined">
</p>

> **Origin and attribution**
>
> This repository is a **fork / derivative of [Mangi-11/Eta](https://github.com/Mangi-11/Eta)**. The original author is **Mangi-11**, and the original repository is <https://github.com/Mangi-11/Eta>.
>
> **Eta-Refined only reworks the UI/UX and engineering (design language, documentation, and the release / upstream-sync workflows); the complete feature set of the original app is inherited from upstream.** It does not change the Agent Runtime, device tools, system adaptation, or other core implementation logic. Both the original project and this derivative are licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE); redistributions must comply with its Required Notice requirement (see [License](#license)).

Eta is an AI agent designed for phones and other mobile devices. It combines the autonomous task planning and execution of coding agents such as [Codex](https://openai.com/codex/) with the GUI-driven, cross-app interaction demonstrated by [Doubao Phone Assistant](https://o.doubao.com/). It can work with files, run commands, write code, and integrate deeply with Android and OEM apps to retrieve personal context and invoke native system and first-party app interfaces, with the goal of delivering a true **system-level AI assistant experience**.

Requires **Android 13 or later on ARM64 devices**. The app is not tied to any phone brand, and core features do not require root. Root and LSPosed unlock deeper personal context, first-party app interfaces, and system assistant entry points, subject to permissions and ROM compatibility.

[Download APK](https://github.com/zhaofeng142020-hash/Eta-Refined/releases) · [Quick start](#quick-start) · [What has changed](#what-has-changed) · [FAQ](docs/FAQ.md)

## Quick start

1. **Download**: get the `Eta-Refined` APK from [Releases](https://github.com/zhaofeng142020-hash/Eta-Refined/releases).
2. **Install**: install the APK on an Android 13+ ARM64 device (allow installation from unknown sources on first install).
3. **Configure and chat**: on first launch the **setup wizard** opens automatically. It walks you through model, appearance, and permission steps; each step can jump straight to the relevant settings page, so you enter your **API Key** (or sign in with a **Codex / Grok** subscription) and pick a default model under **Settings → Model Providers**. You can skip the wizard and configure later. Then start a conversation.

> Running tasks requires a model with **Tool Calling** support; interpreting images also requires image input support. Chat and the system assistant entry points do not work until a model is configured.

## Capabilities

Eta-Refined inherits the full capability set of the original Eta. The following is a condensed overview.

### Execution tools

- **System and first-party app actions:** read and write calendar events (single or batch creation, updates, deletion), manage alarms and timers, create and recycle system notes; control the flashlight, brightness, screen timeout, ringer and Do Not Disturb, Night Light and dark mode, volume, media playback, Wi-Fi, Bluetooth, mobile data, personal hotspot, NFC, location, and battery saver. App versions, install sources, and permissions can be inspected; apps can be force-stopped or frozen, and system logs filtered with root. Settings, toggles, and first-party app writes are read back to confirm the actual state rather than replayed blindly.
- **GUI Agent and background virtual display:** combine the accessibility UI tree, element targeting, and on-demand screenshots to tap, long-press, swipe, scroll, and type text; successful actions include a snapshot of the new UI tree, and screenshots keep their original size and pixels without pre-upload scaling or JPEG conversion. Tasks on an app already in the foreground run in place; cross-app tasks can run silently on a trusted background virtual display with root or LSPosed, without interrupting the main screen. The model can call `present_to_user` to hand a running task back to the physical display.
- **Run notifications:** on Android 16 and later with Live Updates, agent status can appear in the status bar capsule and on the lock screen; availability depends on system support and notification settings, and some systems map it to Fluid Cloud or similar surfaces.
- **Web search and built-in browser:** lightweight public web search and paginated static content reading, switchable between the built-in default, DeepSeek Search / Fetch, or custom HTTP endpoints; dynamic JavaScript pages load in a built-in WebView for content extraction, DOM interaction, and screenshots, with the same browser session available for manual takeover.
- **Terminal:** Android user/root shell, Alpine / Debian Linux, and script execution, with persistent sessions, asynchronous background commands, and daemon tasks; returns the actual shell, execution identity, and parseable command information.
- **File tools:** read, write, precise edit, metadata lookup, directory paging, path matching, text search, and local image reading. Text reads continue at UTF-8 byte boundaries, edits verify old content, and search results state the actual range and any unfinished portion.

### Context and extensions

- **Personal context:** structured read-only tools for on-device and OEM ROM data. Standard permissions cover time and timezone, recent location, device environment, active and recent-7-day notifications, recent app activity and foreground usage, and calendar events. Root adds contacts, call logs, SMS and OTP extraction, photos (by filename or by OCR text / visual description / tags), audio and recordings, shared files and downloads, IME clipboard history, Health Connect summaries, saved Wi-Fi passwords, and WeChat/QQ chat image caches. On ColorOS, an in-process Xiaobu Memory hook bridge and system DMP indexes additionally expose memory, notes, call and recording summaries, bills and spending, flight/train/hotel itineraries, delivery orders, to-do leads, life events, and saved places.
- **Long-term memory:** cross-conversation context stored in a local `MEMORY.md`; core content is included within a context budget and the rest is read on demand. Editable, clearable, and disableable.
- **Subagent:** dispatch and run multiple Subagents concurrently in isolated contexts (built-in `explore`, `coder`, `plan`), optionally with lighter/faster models, with live step-by-step inspection in the chat timeline.
- **Skills:** load task instructions, reference material, and script resources on demand; install from public GitHub repositories or import a local ZIP. Installation does not run scripts or grant extra permissions.
- **MCP:** connect remote tools over Streamable HTTP with optional Bearer Token; tools are enabled individually and participate alongside local tools.

### Agent runtime and optional roles

The Agent Runtime runs inside the Eta app; requests from chat and system assistants share one Agent Loop. The model selects tools through tool calling, results return to its context, and it decides the next step. The runtime manages streaming events, steering, cancellation, and incremental transcripts. Sessions and results are archived locally; after an interruption it attempts to recover existing records without automatically replaying actions.

The sidebar "Characters" manages, imports, and exports tavern-compatible PNG / JSON character cards and starts conversations from a character. Character sessions can still call Eta's phone tools and support user personas, embedded world books, and independent story memory; ordinary conversations and system assistant entry points keep the default Eta.

## System assistant entry points

Eta implements a full Android native digital assistant service (`VoiceInteractionService`) and can run as the system default assistant, waking Eta directly from a long press of the power button:

- **Eta native digital assistant:** wake Eta's own global overlay from the power button, with the current screenshot and app content supplied by the system; voice or keyboard input, suggested prompts, and a pull-up gesture to take over the current session in the main UI. Speech recognition and playback are independently configurable.
- **Power-button routing:** choose whether a long press wakes Eta, Gemini, or the default OEM assistant.
- **Breeno / Super XiaoAI takeover:** keep the OEM assistant UI and route requests to Eta and the model you choose.
- **Gemini and Circle to Search:** on supported systems, fill in Gemini system assistant capabilities and unlock Circle to Search.

On ROMs that lock down the default assistant or power-button behavior, routing and OEM assistant takeover require LSPosed and corresponding system support; some features additionally require root.

## Models and BYOK

Eta's AI features work with your own model-provider **API Key (BYOK)** or **Codex (ChatGPT) / Grok subscription sign-ins**. Built-in provider configurations include OpenAI, Anthropic, Alibaba Cloud Model Studio, DeepSeek, Kimi, MiMo, MiniMax, StepFun, SiliconFlow, OpenRouter, and xAI; custom services can be added.

The built-in OpenAI and xAI providers switch between an API Key and subscription sign-in: Codex via PKCE, Grok via device code, both completed in the system browser. OAuth credentials are encrypted via Android Keystore in non-backed-up local storage and excluded from IPC and backups. The provider layer supports OpenAI-compatible Chat Completions, the Responses API, Anthropic Messages, and Gemini native Interactions API, including SSE streaming, tool calling, image input, and reasoning content.

## Permissions and data boundaries

System tools, sensitive reads, sensitive actions, terminal and file access, browsing, and memory each have independent switches, enabled by default. The runtime rechecks permissions before execution; revoking access or losing a connection does not overwrite saved settings.

- **Capability boundaries:** unrooted devices support chat, web search and the built-in browser, memory, Skills, MCP, the ordinary terminal, and a private workspace; calendar read/write, GUI control, and notification/location/usage reads are granted on demand. Rooted devices additionally unlock background virtual-display execution, system settings and toggles, app management, protected files, deep personal-data retrieval, and native first-party app actions. LSPosed with a compatible ROM unlocks OEM assistant takeover, system shortcuts, and Google feature enablement; some features also require root.
- **Where data goes:** task-relevant conversation content, images, and tool results are sent to your configured model service; a local runtime does not imply local inference. Custom HTTP endpoints transmit API keys and request content without transport encryption.
- **Local records:** raw arguments and results of sensitive tools and MCP tools are excluded from persistent sessions, while model replies are saved. With notification access granted, up to 1,000 notifications are kept for seven days; MCP auth tokens, standalone web-tool keys, and provider OAuth credentials are encrypted via Android Keystore.
- **Execution limits:** tasks can be stopped or taken over. Background work is subject to Android and OEM process management; restart tasks manually after a force-stop or reboot, and system/app updates may require hook adaptations.

See [Device support](docs/ROOTLESS_SUPPORT.md) for full conditions and validation coverage.

## What has changed

Eta-Refined differs from the original Eta only in the **visual and interaction layer**; business logic and system adaptation are unchanged. The following distinguishes delivered work from work still being migrated, rather than describing only the target design:

- **Mist Glass color token layer (delivered):** a Compose-based color token layer has been **introduced and wired into `AgentAppTheme`** as the global theme entry point, replacing the default Miuix palette. Color tokens are in effect across the app.
- **Design token layer (partially delivered, migration in batches):** tokens for shapes, spacing, typography, motion, glass tiers, and responsive breakpoints are defined in `ui/theme2/` and have been applied to the chat input bar, tool chips, empty states, error cards, and the onboarding wizard. Other existing screens (`SettingsScreen`, chat bubbles, top bars, and so on) still use their original literal values and are **being migrated in batches**. Glass tiers are an approximation — semi-transparent fill plus a hairline stroke — **not a true background blur**.
- **First-launch onboarding wizard (delivered):** the wizard is wired into the startup path with a persisted first-launch flag, conditional rendering in `MainActivity`, and routed "go configure" actions in `AgentAppRoot` that lead to the model, appearance, and permission settings; the skip action works.
- **Unified error card (partially delivered):** `MistErrorCard` (title, actionable fix hint, retry) has been introduced and wired into the conversation-load-failure path; the remaining 20+ Toast sites are being migrated gradually. Empty states in lists now delegate to `MistEmptyState`.
- **Documentation and engineering (delivered):** README, FAQ, refactoring and upstream-sync notes have been rewritten; the original build system is kept, with documented release, signing, and upstream-sync workflows.

Token details (colors, corner radii, 4-multiple spacing, type scale, motion durations, four glass tiers, responsive breakpoints), migration cost and risk, and the no-regression guarantees — together with an explicit **current implementation status** section — are recorded in **[docs/REFACTORING.md](docs/REFACTORING.md)**.

## Upstream sync

Feature updates follow the upstream **[Mangi-11/Eta](https://github.com/Mangi-11/Eta)**:

- **Daily automated sync:** a scheduled job pulls functional updates from upstream and merges them into `main` through a Pull Request.
- **No silent overwrite on conflict:** on conflict, an Issue and a conflict PR are opened automatically, with the conflicting files and reasons annotated, for maintainer adjudication; this repository's reserved areas (`ui/theme2/`, `ui/onboarding/`, `README.md`, `docs/`) are preserved first.
- **Quality gate:** a build or test failure blocks the merge.

See **[docs/UPSTREAM_SYNC.md](docs/UPSTREAM_SYNC.md)** for the sync source and workflow details. Upstream sync involves Mangi-11/Eta only, never any design-reference project.

## Releases

- **Stable releases:** pushing a `v*` tag triggers the stable build and release workflow.
- **Nightly builds:** automatic builds produced after merges into `main`, for continuous validation.
- **Manual trigger:** the build can be run manually from Actions.

Release workflow, signing configuration, and versioning rules are documented in **[.github/RELEASING-REFINED.md](.github/RELEASING-REFINED.md)**.

## FAQ

Installation failures, signature conflicts, API Key configuration, root / LSPosed permissions, upstream sync, upgrades, and rolling back to the original Eta are covered in **[docs/FAQ.md](docs/FAQ.md)**.

## Further reading

Most of these implementation notes are currently written in Chinese:

- [Refactoring notes and differences](docs/REFACTORING.md): technology choices, Mist Glass design tokens, directory changes, and risk control.
- [Upstream sync notes](docs/UPSTREAM_SYNC.md): sync source, branch strategy, conflict handling, and reserved areas.
- [FAQ](docs/FAQ.md): installation, configuration, permissions, upgrades, and rollback.
- [Device support and permission boundaries](docs/ROOTLESS_SUPPORT.md): unrooted, rooted, file workspace, and background execution.
- [Technical implementation](docs/TECHNICAL.md): device tools, data retrieval, browser, terminal, and system integration.
- [Agent Runtime](docs/AGENT_RUNTIME.md): Agent Loop, providers, steering, transcripts, and result recovery.
- [Third-party notices](docs/THIRD_PARTY_NOTICES.md): bundled third-party components and licenses.

## References and acknowledgements

- **[Mangi-11/Eta](https://github.com/Mangi-11/Eta)**: the upstream and origin of this project; original author Mangi-11. All of Eta-Refined's capabilities come from it.
- **[dsh-Go](https://github.com/ccch1mneyyy/dsh-Go)**: UI/UX design-language reference only (design ideas, no code copied; licensed AGPL-3.0).
- [Pi Coding Agent](https://github.com/earendil-works/pi): core reference for Eta's Agent Runtime.
- [OmniBot](https://github.com/omnimind-ai/OmniBot): reference project for AI agents on Android.
- [libxposed API](https://github.com/libxposed/api): modern Xposed API.
- [Miuix](https://github.com/compose-miuix-ui/miuix): UI component library used by the original interface.

Full third-party attributions are in **[ATTRIBUTION.md](ATTRIBUTION.md)** and [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md).

## Star History

<p align="center">
  <a href="https://www.star-history.com/#zhaofeng142020-hash/Eta-Refined&amp;Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=zhaofeng142020-hash/Eta-Refined&amp;type=Date&amp;theme=dark">
      <img src="https://api.star-history.com/svg?repos=zhaofeng142020-hash/Eta-Refined&amp;type=Date" alt="Eta-Refined Star History" width="800">
    </picture>
  </a>
</p>

## License

Eta-Refined, like the original Eta, is licensed under the **[PolyForm Noncommercial License 1.0.0](LICENSE)**. The license permits copying and distributing the software (including modified and derivative works) for **noncommercial purposes**, provided that each copy ships with the license terms (or the license URL) together with the `Required Notice` copyright line listed in [LICENSE](LICENSE). Commercial use (resale, paid installation, commercial integration, and similar) is outside the grant and requires separate permission from the [original author](https://github.com/Mangi-11). The full terms in [LICENSE](LICENSE) govern.

The original copyright remains with the author. When redistributing this software or a derivative work, you must include the license terms (or the license URL) together with the **Required Notice** text, recorded verbatim in [NOTICE](NOTICE) and [ATTRIBUTION.md](ATTRIBUTION.md).

<sub>Community: <a href="https://linux.do">LINUX DO</a></sub>
