# AI-Assisted System for Converting Websites into Mobile Applications

**Academic project:** Final-year engineering project
**Institution:** S.J.C. Institute of Technology
**Department:** Information Science and Engineering
**Team:** Group 14
**Academic year:** 2026–2027

## Overview

This project is an Android application that helps a user configure a website as a
mobile application and generate an installable Android APK. It is an academic
adaptation of the upstream [WebToApp](https://github.com/shiaho777/web-to-app)
codebase. The adapted version focuses the visible workflow on website input,
basic app configuration, website/PWA analysis, optional AI-assisted settings,
preview, and APK creation.

The application performs APK assembly and signing locally using the existing
WebToApp build pipeline. It does not require a separate project-specific
backend for the AI Configuration Advisor; the advisor uses the AI provider,
model, and API-key configuration already supported by the application.

## Current features

The simplified website-to-app flow currently provides:

- Website URL input and basic application configuration, including app name,
  icon, and custom package name.
- Website/PWA analysis using the supplied URL. The analyzer reads a web app
  manifest when available and falls back to page metadata.
- An optional **Suggest Settings with AI** action in the simplified Web
  creation flow.
- A recommendation preview that can be explicitly **Applied** or **Cancelled**.
- AI recommendations for:
  - application name;
  - portrait or landscape orientation;
  - a validated hexadecimal theme/status-bar color;
  - fullscreen behavior.
- Previewing the configured website before saving.
- Building and signing APKs locally.
- Installing or sharing generated APKs where the Android device and the
  existing implementation support those actions.
- Application appearance/settings and an About screen.

The AI response is treated as untrusted input. Only the supported fields and
validated values are applied; recommendations are not guaranteed to be correct
and always require user approval.

The adapted version has removed the Google Play AAB export flow and its related
UI. The README intentionally does not describe every advanced capability from
the upstream project as part of this simplified application.

## AI Configuration Advisor

### User workflow

1. Open the simplified website creation flow.
2. Enter a valid website URL. Bare domains are accepted and normalized by the
   existing URL handling.
3. Use **Analyze Site** when website/PWA metadata is useful.
4. Select **Suggest Settings with AI**.
5. Review the returned recommendation preview.
6. Select **Apply** to update the editor, or **Cancel** to discard it.
7. Continue manual configuration, preview the app, save it, and build the APK.

The advisor reuses the current URL and the available PWA analysis result. It
does not execute generated code or accept generated URLs as configuration.
Blank or invalid URLs disable the analysis and advisor actions.

### AI Settings

Configure the provider, API key, and saved model from:

**Settings → AI Settings**

The existing AI Settings screen supports API-key management, connection tests,
saved-model selection, model catalog access, and secure local storage of
credentials. The advisor uses the configured general text model. AI Settings
must contain a usable text-capable model and corresponding provider
configuration before the advisor can return recommendations.

The provider catalog currently includes:

- Google Gemini;
- OpenRouter;
- OpenAI;
- Anthropic;
- Grok;
- Together;
- Perplexity;
- Fireworks;
- DeepSeek;
- Qwen;
- GLM;
- Ollama;
- LM Studio;
- vLLM;
- custom endpoints.

Provider availability, endpoint format, and model access remain subject to the
provider's service and account requirements.

## Technology stack

The following technologies are used by the current repository:

- Kotlin 2.2.10;
- Android Gradle Plugin 9.2.0;
- Jetpack Compose and Material 3;
- Android SDK compile/target configuration 36;
- Gradle Wrapper 9.4.1;
- KSP for generated persistence code;
- Room for local application data;
- OkHttp for network requests;
- Gson for configuration and AI response handling;
- Android DataStore and Android Keystore-backed encryption for AI settings;
- `com.android.tools.build:apksig` for local APK signing;
- optional GeckoView support in addition to the Android System WebView.

## Prerequisites

For a clean local setup, install:

- Git;
- Android Studio with a compatible Android SDK installation;
- a JDK 17 runtime;
- Android SDK Platform 36;
- the Android SDK build tools selected by the installed Android Gradle Plugin;
- network access for Gradle and dependency downloads;
- an AI provider API key if the AI Configuration Advisor is to be tested.

The repository does not contain API keys, passwords, or signing credentials.
Do not add them to source control or to this README.

The exact Android SDK build-tools revision is not pinned in the application
Gradle configuration. Android Studio should therefore be allowed to install
the compatible build-tools package requested by Gradle.

## Installation and setup

Clone the repository:

```bash
git clone https://github.com/cb3333099-ux/web-to-app.git
cd web-to-app
```

Open the `web-to-app` directory in Android Studio and allow Gradle sync to
finish. If Android Studio reports missing SDK components, install the
requested Platform 36 and build-tools components from SDK Manager.

After installing the application on an emulator or device:

1. Open **More → Settings → AI Settings**.
2. Add and test an AI provider API key.
3. Add a text-capable model and mark/select it for general use.
4. Return to **My Apps → Create**, select the Web app type, and enter a URL.
5. Use the website analyzer or AI advisor as needed.

### Build the standard debug APK

The repository's standard debug build can be run with:

```bash
./gradlew :app:assembleStandardDebug -PallowDebugSignedRelease=true --no-daemon --no-parallel --max-workers=1 --console=plain
```

On Windows PowerShell, use:

```powershell
.\gradlew.bat :app:assembleStandardDebug -PallowDebugSignedRelease=true --no-daemon --no-parallel --max-workers=1 --console=plain
```

`allowDebugSignedRelease` is a local development override used by this
repository when a release signing configuration is not available during
Gradle configuration. It is not a production signing setup.

The verified standard debug APK output path is:

```text
app/build/outputs/apk/standard/debug/app-standard-debug.apk
```

Install that APK with Android Studio, the emulator, or the Android Debug
Bridge as appropriate for the local environment.

## Testing

Run the standard debug unit-test task with:

```bash
./gradlew :app:testStandardDebugUnitTest --no-configuration-cache -PskipShellTemplateSync=true
```

On Windows:

```powershell
.\gradlew.bat :app:testStandardDebugUnitTest --no-configuration-cache -PskipShellTemplateSync=true
```

The unit tests are under
[`app/src/test/java/com/webtoapp`](app/src/test/java/com/webtoapp). The
AI-advisor parsing tests are in
[`AiConfigurationAdvisorTest.kt`](app/src/test/java/com/webtoapp/core/ai/AiConfigurationAdvisorTest.kt).
They cover valid recommendations, partial recommendations, malformed JSON,
invalid values, and responses with no usable settings.

Unit tests do not replace manual device testing. On an emulator or physical
device, verify URL validation, website analysis, AI Settings configuration,
recommendation preview, Apply/Cancel behavior, preview, APK installation, and
APK sharing.

## Project structure

Important paths include:

```text
app/
├── src/main/java/com/webtoapp/
│   ├── core/ai/
│   │   ├── AiApiClient.kt
│   │   ├── AiConfigManager.kt
│   │   └── AiConfigurationAdvisor.kt
│   ├── core/apkbuilder/       # APK assembly, patching, and signing
│   ├── core/pwa/              # Website/PWA analysis
│   ├── data/model/            # WebApp and AI configuration models
│   ├── ui/navigation/         # App routes, including AI Settings
│   ├── ui/screens/
│   │   ├── CreateAppScreen.kt
│   │   ├── AiSettingsScreen.kt
│   │   └── SettingsScreen.kt
│   └── ui/viewmodel/
│       └── MainViewModel.kt
└── src/test/java/com/webtoapp/
    ├── core/ai/               # AI configuration and advisor tests
    └── core/pwa/              # Website/PWA analyzer tests
```

Other top-level modules are:

- `shell/` — runtime template used by generated applications;
- `clone-host/` — support code for the inherited app-cloning functionality;
- `feature-stacks/` — optional feature-stack modules;
- `docs/` — upstream developer and user documentation;
- `gradle/` — Gradle Wrapper files;
- `scripts/` — repository build and consistency helpers.

## Team collaboration

Each contributor should create a branch for a focused change:

```bash
git switch -c feature/short-description
```

Review the changes locally, run the relevant tests/build tasks, and commit with
a descriptive message:

```bash
git add README.md
git commit -m "Document AI-assisted website-to-app project"
git push -u origin feature/short-description
```

Open a pull request against the shared repository branch. Keep API keys,
keystores, passwords, emulator data, and other private material out of commits.

## Academic contribution and attribution

### Work completed for this academic adaptation

- Simplified the visible website-to-app creation workflow for the project
  demonstration.
- Kept the basic URL, app identity, preview, save, and APK build flow visible
  in the adapted UI.
- Removed the Google Play AAB export flow and related UI from the adapted
  workflow.
- Added the AI Configuration Advisor, including:
  - reuse of existing provider, API-key, and model infrastructure;
  - reuse of website/PWA metadata;
  - strict bounded recommendation parsing and validation;
  - a preview before any settings are changed;
  - explicit Apply and Cancel actions.

### Inherited functionality

The APK assembly, shell-template packaging, local signing, WebView runtime,
database models, and much of the surrounding application infrastructure are
adapted from the upstream WebToApp project. This project is not presented as
an application written entirely from scratch.

The original repository's public-domain dedication and warranty disclaimer are
retained in [`LICENSE`](LICENSE). Upstream documentation and attribution are
retained in the repository; this README documents only the adapted workflow
and verified project scope.

## Limitations and future work

Current limitations include:

- AI recommendations require a configured provider, API key, and usable text
  model.
- Network availability and provider-specific quotas or failures can prevent
  recommendations.
- AI output is advisory and may be incomplete or unsuitable; users must
  review it before applying it.
- The simplified academic UI does not expose every feature present in the
  upstream codebase.
- Google Play AAB export is not part of this adapted workflow.

Possible future work, not implemented in the current version, includes:

- evaluating recommendation quality across a larger set of websites;
- adding user feedback and comparison metrics for suggested settings;
- supporting additional validated app settings;
- improving offline or locally hosted model support;
- adding repeatable usability and build-time evaluation experiments.
