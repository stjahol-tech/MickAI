# Mick AI Android — Kotlin/Compose rebuild (source preview)

## What's included
- Native Jetpack Compose personal AI chat screen.
- Explicit user-started floating mascot foreground service, draggable with tap-to-wave.
- Existing original 8×11 sprite atlas, with **manifest-driven** row and frame counts.
- Original head-turn strip retained separately for future atlas packing.
- HTTPS-only backend client with session-only bearer token entry (not persisted).
- FastAPI backend that keeps the OpenAI key on the server and checks a bearer token.
- Kotlin JUnit animation tests and Python backend tests.

## Android build
Open this folder in Android Studio (JDK 17, Android SDK 35) and sync Gradle. Build the `app` module and install on an Android 8+ device. Grant overlay permission from the app's button and notification permission when prompted. Tap **Float Mick** again after granting overlay access.

The environment used to prepare this archive has no Android SDK/Gradle executable, so compilation and on-device behavior have **not** been verified. Android foreground-service restrictions vary by OS/device; test on Android 14/15, including notification and overlay permission behavior. No Gradle wrapper is bundled: Android Studio can import the project using its installed Gradle tooling, or generate a wrapper from a trusted Gradle installation.

## Backend
Install Python dependencies in `backend/requirements.txt`. Set `MICK_ACCESS_TOKEN` to a long random secret, `OPENAI_API_KEY` to a valid provider key, optionally `OPENAI_MODEL`, then run `uvicorn main:app --host 127.0.0.1 --port 8000` from `backend/`. For device access, deploy behind **HTTPS** with TLS, authentication, rate limits, and appropriate production logging policies. Enter the resulting HTTPS base URL and bearer token in the app. Never put the OpenAI API key in Android code. This single shared-token prototype is suitable only for controlled personal testing; use individual authentication before multiuser release.

## Tests
From `backend/`, run `pytest -q`. In Android Studio run the `testDebugUnitTest` Gradle task.

## Limitations / next work
- AI responses are request/response (not streaming). Chat history exists in memory for the current session only; Room persistence, voice, and account management are not yet implemented.
- The floating pet and chat are separate interfaces; mascot AI-state synchronization is not yet wired.
- The eight-frame head-turn strip is preserved as `head-turn-original.png` with `head-turn-manifest.json`; it is not yet packed into the main sprite atlas.
- **Visual QA warning:** source continuity metadata flags the transition at **157.5° → 180°**. Inspect that boundary before enabling continuous directional head-turn playback.
- API security needs production rate limiting, abuse controls, per-user authentication, deployment secrets management, and monitoring.
- No APK or signed production build is included.

## Reused source
The original uploaded `MickAI-Android-source.zip` contains the Java/Kotlin service, activity, original atlas, metadata and Gradle project. This rebuild preserves and extends the overlay logic and atlas, and introduces Compose UI, manifest-driven playback and backend code.
