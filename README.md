# Wortlinge

A pet-collecting game for learning German, starting from zero. Every German word is a pet:
find its egg on the map, say the word to hatch it, and keep saying it to keep the pet fed.

Kotlin · Jetpack Compose · Room · no network, no accounts, no ads.

## Project layout

| Module | What lives there |
|---|---|
| `core/` | Plain Kotlin, no Android: word data model, pet genes, spaced repetition, speech matching. Unit-tested. |
| `app/` | The Android app: Compose UI, text-to-speech, speech recognition, Room. |
| `app/src/main/assets/words.json` | All words, areas and egg positions. |

## Build from the terminal (Windows)

Needs JDK 21 (`JAVA_HOME`) and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`).

```powershell
.\gradlew.bat :core:test              # fast unit tests for the game rules
.\gradlew.bat test                    # all tests, including pet snapshot images
.\gradlew.bat assembleDebug           # -> app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat installDebug            # build and install on the connected phone
```

Pet snapshots are written to `app/build/outputs/roborazzi/pets.png`.

## Installing on a phone without a cable (recommended)

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy.ps1
```

This builds the app and uploads it to the `dev` release on GitHub. On the phone, open
<https://github.com/Luca144/LearnGerman/releases/download/dev/wortlinge.apk>, then open the downloaded
file to install. Bookmark that link: it always points at the newest build. The build number under
the title in the app shows which build is installed.

The first time, Android asks to allow installs from your browser ("Install unknown apps"), and
Play Protect may say it doesn't recognise the developer: choose **Install anyway**. The APK is
public, like the rest of this repository.

## Installing on a phone over USB

1. On the phone: Settings → About phone → tap **Build number** seven times.
2. Settings → System → Developer options → turn on **USB debugging**.
3. Connect by USB, accept the "Allow USB debugging?" prompt, then check with `adb devices`.
4. Run `.\gradlew.bat installDebug`.

Wireless instead of USB (Android 11+): Developer options → **Wireless debugging** → *Pair device with
pairing code*, then `adb pair <ip>:<pair-port>` and `adb connect <ip>:<port>`.
