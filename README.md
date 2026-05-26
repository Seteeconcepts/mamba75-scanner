# Mamba75 Scanner — Android App v3.0

A floating overlay Android app that runs the Mamba75 Deriv digit scanner
continuously — even when you leave the app.

---

## Features

- **Floating window** — scanner stays on top of all other apps
- **Draggable** — move the window anywhere on screen
- **Minimize/Expand** — collapse to a compact header bar when not needed
- **Background foreground service** — persistent notification keeps the
  WebSocket alive; Android won't kill it
- **Stop button** in notification — stop scanner from the notification shade
- **No login required** — connects directly to Deriv's public WebSocket API

---

## How to Build (Android Studio — Recommended)

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- Android SDK API 34
- Java 8+

### Steps
1. Open Android Studio
2. **File → Open** → select the `mamba75-scanner-android` folder
3. Let Gradle sync (it downloads dependencies automatically)
4. If prompted, install SDK components — click **OK**
5. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
6. The signed APK will appear in `app/build/outputs/apk/release/`
7. Transfer the APK to your Android device and install it

> **Enable unknown sources:** Settings → Security → Install unknown apps → allow
> your file manager or browser

---

## How to Build (Command Line)

```bash
# You need ANDROID_HOME set to your Android SDK path, e.g.:
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Download Gradle wrapper jar (one time)
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/v8.0.0/gradle/wrapper/gradle-wrapper.jar

# Build debug APK
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk

# Install directly to connected device (USB debugging on)
./gradlew installDebug
```

---

## App Usage

1. Open **Mamba75 Scanner** app
2. Tap **▶ LAUNCH FLOATING SCANNER**
3. On first launch, you'll be sent to Android Settings to grant
   **"Display over other apps"** permission — enable it, then come back
4. The scanner window appears floating on your screen
5. **Minimize this app** — the scanner stays visible on top
6. Open Deriv, DBot, or any other app — scanner remains in the corner
7. **Drag** the scanner by its top bar to reposition
8. **—** button collapses it to a compact title bar
9. **✕** button stops the scanner entirely
10. **Notification → Stop** also stops the scanner

---

## First-Run Permissions

| Permission | When asked | Why needed |
|---|---|---|
| Display over other apps | On first launch | Show floating window |
| Post notifications | On first launch (Android 13+) | Show persistent notification |
| Internet | Automatic | WebSocket to Deriv API |

---

## Project Structure

```
app/src/main/
├── java/com/mamba75/scanner/
│   ├── MainActivity.java          — Control panel UI
│   └── FloatingWindowService.java — Floating window + background service
├── assets/
│   └── index.html                 — Mamba75 Scanner v3 (full app)
├── res/
│   ├── layout/
│   │   ├── activity_main.xml      — Main screen layout
│   │   └── floating_window.xml    — Floating overlay layout
│   └── values/
│       ├── strings.xml
│       └── themes.xml
└── AndroidManifest.xml
```

---

## Architecture Notes

- The scanner logic (WebSocket, signal detection, leaderboard) is **entirely
  in `assets/index.html`** — pure HTML/JS, unchanged from the web version
- `FloatingWindowService` wraps this in an Android `WebView` inside a
  `WindowManager` overlay — no native scanning code needed
- `START_STICKY` ensures the service restarts if Android kills it
- The foreground service type is `dataSync` — matches ongoing network data
  activity, which is exactly what this app does

---

*Built for Deriv volatility index digit-over trading · Strategy: RISE + digit 4|5 → predict OVER 4*
