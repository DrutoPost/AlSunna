<div align="center">

# AlSunnah

**Hadith Commentary Translation & Explanation Assistant**

AlSunnah allows you to translate and study Hadith explanations directly from [alsunna.net](https://alsunna.net). Powered by Google Gemini web, it provides complete, accurate Bengali translations of classical Hadith commentary (including *إخفاء شرح الحديث*) with proper citations and scholar references.

[![Android 8+](https://img.shields.io/badge/Android-8%2B-A8E06C?style=flat-square&labelColor=161A18&logo=android&logoColor=white)](https://github.com/DrutoPost/AlSunna)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-A8E06C?style=flat-square&labelColor=161A18&logo=kotlin&logoColor=white)](https://github.com/DrutoPost/AlSunna)
[![No analytics](https://img.shields.io/badge/analytics-none-A8E06C?style=flat-square&labelColor=161A18)](https://github.com/DrutoPost/AlSunna)

</div>

<br>

## What It Does

- **Direct Link Intake:** Share or paste any `alsunna.net` Hadith URL into the app.
- **Complete Commentary Translation:** Retrieves and translates the Hadith and its entire scholarly commentary into Bengali without abridgment.
- **Strict Markdown Formatting:** Presents every scholar's statement and narration as a blockquote (`> ...`) paired with its exact citation and reference source.
- **Dynamic Script-Aware Typography:**
  - **Bornomala Font:** Automatically applied to all Bengali characters.
  - **Scheherazade Font:** Automatically applied to all Arabic text and quotes.
  - Even within a single line containing mixed languages, fonts dynamically switch to their respective scripts.
- **Zero Telemetry & Private Storage:** Sign-in cookies and translation histories remain locally encrypted on your device.

---

## Features

| Feature | Description |
|---|---|
| **alsunna.net Support** | Accepts valid `http://` or `https://` links from `alsunna.net` (strictly without subdomains). |
| **Google Gemini Integration** | Connects to Gemini web with your own Google account for translation and synthesis. |
| **Markdown Rendering** | Interactive Compose Markdown renderer that styles headings, blockquotes, code blocks, lists, and links. |
| **Optimized App Size** | Streamlined footprint with native FFmpeg binaries removed for lightweight distribution. |

---

## Installation

1. Download the latest release APK (`release.apk`) from GitHub Releases.
2. Open the downloaded APK on your Android device (Android 8.0+ / API 26+).
3. If prompted, allow installation from unknown sources for your browser or file manager.
4. Tap **Install** to complete the setup.

---

## Building from Source

### Prerequisites
- **JDK:** Version 21 (Gradle toolchain managed)
- **Android SDK:** `compileSdk` 37, `minSdk` 26
- Android SDK directory configured in `local.properties`:
  ```properties
  sdk.dir=/path/to/android-sdk
  ```

### Build Commands

- **Run Core Tests:**
  ```bash
  ./gradlew :core:test
  ```

- **Run App Unit Tests:**
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```

- **Build Release APK:**
  ```bash
  ./gradlew assembleRelease
  ```

---

<div align="center">
<sub>AlSunnah is an independent open-source project. Not affiliated with Google or alsunna.net.</sub>
</div>
