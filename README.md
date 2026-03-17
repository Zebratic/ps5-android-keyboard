# 🎮 SenseKeyboard

**Gamepad keyboard for Android TV**

A fully customizable on-screen keyboard designed for gamepad controllers on Android TV. Navigate with the D-pad, type with the action buttons, and customize everything from colors to animations.

> **Tested with:** PS5 DualSense controllers — should work with any standard gamepad controller (Xbox, Switch Pro, generic Bluetooth controllers).

## 📥 Download

**[⬇️ Download Latest APK](https://github.com/Zebratic/SenseKeyboard/releases/latest)**

Or grab it from the [Releases](https://github.com/Zebratic/SenseKeyboard/releases) page.

The app has a built-in update checker — tap "Check for updates" on the Setup page to auto-download and install new versions.

## ✨ Features

- **Full gamepad support** — D-pad navigation, face buttons for typing, triggers for shift/enter
- **Shift** — L2 hold for temporary shift, double-tap L2 for caps lock, on-screen ⇧ button
- **Text selection** — L2 + L1/R1 to select text, with Copy & Paste buttons
- **Hold to repeat** — Hold A/Cross to repeat characters, X/Square to rapid-delete, bumpers for cursor
- **Word suggestions** — Autocomplete with learned vocabulary
- **6 theme presets** — PS5, Xbox, Steam, Minimal, Rounded, Retro (or fully custom)
- **Color controls** — Accent color with brightness & saturation, key colors with opacity, font color
- **4 navigation effects** — Wind particles, sliding border, ripple, ghost trail
- **3 click animations** — Fill, pop, flash
- **Multiple layouts** — US QWERTY, Danish QWERTY (more coming)
- **Numpad mode** — Calculator-style layout with operators
- **Symbol keyboard** — Full special character set
- **Number row** — Optional, shows !@#$%^&*() when shifted
- **Adjustable size & position** — Width, height, anchors, margins
- **Font customization** — 8 font families + size scaling
- **Live preview** — See your keyboard overlay while customizing in settings
- **Auto-update** — Check for and install updates directly from the app

## 🎮 Controls

| Button | Action |
|--------|--------|
| **D-pad** | Navigate keys |
| **A / ✕** | Type character (hold to repeat) |
| **Y / △** | Space |
| **X / □** | Backspace (hold to repeat) |
| **B / ○** | Close keyboard |
| **L1 / R1** | Move cursor (hold to repeat) |
| **L2 hold** | Shift (uppercase while held) |
| **L2 × 2** | Caps lock |
| **L2 + L1/R1** | Select text |
| **R2** | Enter |
| **L2 + R2** | New line |
| **L3** | Switch to symbols |
| **Options/Menu** | Done/Submit |

## 📱 Installation

1. Download the APK from [Releases](https://github.com/Zebratic/SenseKeyboard/releases/latest)
2. Install via ADB: `adb install SenseKeyboard.apk`
3. Open the SenseKeyboard app on your TV
4. Follow the 3-step setup: **Enable → Select → Type!**

## 🛠️ Build from Source

```bash
git clone https://github.com/Zebratic/SenseKeyboard.git
cd SenseKeyboard
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## 📄 License

MIT

---

**Made by [Zebratic](https://github.com/Zebratic)**
