# CallMaster (كول ماستر)
> **Every Conversation, Organized. | كل محادثة، منظمة.**

CallMaster is a native Android call recording management, audio library, AI transcription, and secure storage platform built with Kotlin, Jetpack Compose, Material 3, Room Database, and Android Keystore AES-256-GCM.

---

## 🌟 Key Features & Architecture

### 1. Dual-Language & Custom Cairo Typography (دعم اللغتين وخط Cairo)
- **Arabic (العربية)**: Full Right-to-Left (RTL) layout using Google Fonts **Cairo** bundled locally (`res/font/cairo.ttf`).
- **English**: Modern LTR typography using **Outfit** / Cairo.
- Dynamic runtime language switching in Settings.

### 2. Device Compatibility Engine (محرك فحص التوافق)
Evaluates device manufacturer, model, and Android OS policies across 4 transparent levels:
- **LEVEL A**: Native Supported Recording (Full 2-sided call capture).
- **LEVEL B**: Authorized Alternative Recording (System Audio Capture with user grant).
- **LEVEL C**: Partial Recording (Microphone audio only; speakerphone recommended).
- **LEVEL D**: Unsupported (Direct capture prohibited by platform security constraints).
- Specific compatibility profiles for Cellular Calls, WhatsApp, Telegram, Facebook Messenger, Signal, and Voice Memos.

### 3. Transparent Recording Engine & Privacy
- State Machine: `IDLE` → `CHECKING_COMPATIBILITY` → `WAITING_FOR_CONSENT` → `RECORDING` → `PAUSED` → `FINALIZING` → `VALIDATING` → `SAVED` / `FAILED`.
- Distinguishes honestly between two-sided and microphone-only recordings.
- Never creates fake empty files or simulates bogus success.
- **Quiet Recording Experience**: Suppresses redundant in-app alerts and sounds without hiding Android's OS green microphone privacy indicators.
- Foreground Service with `microphone` type for reliable audio capture.

### 4. Audio Management & Player
- Real-time amplitude metering and dynamic waveform visualization (`CallMasterWaveform`).
- Speed controls (1.0x, 1.25x, 1.5x, 2.0x), seeking, skip forward (+10s), skip backward (-10s).
- Full library management: Search, source filtering (Cellular, WhatsApp, Telegram, etc.), favorite tagging, notes editor, and sharing.

### 5. AI Assistant & Transcription (المساعد الذكي والتفريغ الصوتي)
- Arabic transcription (Standard Arabic & Egyptian Arabic dialect).
- English transcription.
- Executive summary generation.
- Key discussion points extraction.
- Action items checklist.
- One-tap clipboard copy and export.

### 6. Security & Storage (الأمان والتشفير)
- Android Room Database for local structured records.
- Application-private storage.
- Android Keystore AES-256-GCM encryption for audio data.
- SHA-256 PIN lock protection and biometric authentication readiness.
- Secure file wipe/shredding upon deletion.

---

## 📱 Build & Test Verification
- **Min SDK**: 24 (Android 7.0+)
- **Target SDK**: 36 (Android 15+)
- **Kotlin**: 2.2.10
- **Jetpack Compose Material 3**
- **Test Suite**: Robolectric local JVM tests & Unit tests.
