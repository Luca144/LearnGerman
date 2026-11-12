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

## Installing on a phone

1. On the phone: Settings → About phone → tap **Build number** seven times.
2. Settings → System → Developer options → turn on **USB debugging**.
3. Connect by USB, accept the "Allow USB debugging?" prompt, then check with `adb devices`.
4. Run `.\gradlew.bat installDebug`.

Wireless instead of USB (Android 11+): Developer options → **Wireless debugging** → *Pair device with
pairing code*, then `adb pair <ip>:<pair-port>` and `adb connect <ip>:<port>`.
