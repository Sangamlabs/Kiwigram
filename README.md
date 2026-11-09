# Kiwigram: Enhanced Android Client

[![Build Kiwigram Debug APK](https://github.com/Sangamlabs/Kiwigram/actions/workflows/build-debug.yml/badge.svg)](https://github.com/Sangamlabs/Kiwigram/actions/workflows/build-debug.yml)

**Kiwigram** is an advanced, premium Android messaging client built on Telegram's MTProto protocol. It combines full messaging compatibility with the Telegram network alongside powerful privacy and productivity enhancements:

- **Modular Ghost Mode**: Granular, per-account control over Seen Ghost (stealth read receipts), Online Ghost (offline presence suppression), Typing Ghost, Recording Ghost (voice & video note stealth), and Story Ghost.
- **Voice Chat (VC) Ghost**: Advanced voice chat stealth with Auto Mute on join, suppression of speaking indicators, suppression of raise-hand signals, and real-time MTProto compatibility inspection (`groupCall.listeners_hidden`).
- **God Mode**: Safe message management with server-side sender-filtered deletion (`TL_messages_search`), bounded cursor pagination, and client-side authorship verification.
- **Premium Crown Identity**: Distinctive Kiwigram crown branding, blue UI styling, adaptive launcher icons, and splash screens.
- **Multi-Account Isolation**: Independent preference management across all registered accounts (`UserConfig.MAX_ACCOUNT_COUNT = 4`).

---

## Technical Architecture & Safety

Kiwigram maintains full upstream compatibility with the official Telegram MTProto protocol:
- Official MTProto protocol identifiers and serialization schemas are strictly preserved.
- Server-side communication and authentication workflows operate directly against Telegram's secure cloud infrastructure.
- Zero modification to upstream database schemas, encryption algorithms, or core network serialization.

---

## Building from Source

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- Android SDK 36 (`platforms;android-36`, `build-tools;36.0.0`)
- Android NDK `27.2.12479018`
- CMake `3.22.1`
- JDK 21 (Temurin 21 recommended)

### Build Commands
```bash
# Verify Java sources and compilation
./gradlew :TMessagesProj:compileDebugSources --console=plain

# Run automated unit and benchmark test suite
./gradlew :TMessagesProj:testDebugUnitTest --console=plain

# Assemble Kiwigram Debug APK
./gradlew :TMessagesProj_App:assembleAfatDebug --console=plain
```

The resulting debug package will be generated at:
`TMessagesProj_App/build/outputs/apk/afat/debug/Kiwigram-Android-debug.apk`

---

## Automated CI/CD
Automated builds and APK verification are continuously executed via GitHub Actions:
- Workflow: [`.github/workflows/build-debug.yml`](.github/workflows/build-debug.yml)
- Artifact Output: `Kiwigram-Android-debug.apk` (packaged under artifact `kiwigram-debug-apk`)

---

## License & Attribution
Based on the DrKLO Telegram for Android source code, licensed under GNU GPL v2 / v3.
All Telegram protocol, trademark, and original author attributions are preserved in accordance with licensing guidelines.
