# 👻 Ghost Files

> **Your files. Invisible to everyone but you.**

Ghost Files is an Android application that allows users to **encrypt and hide selected files** on their device using **biometric authentication**.
Only the registered owner can access the hidden files — through their fingerprint scan or a custom drawn lock pattern — making unauthorized access virtually impossible.

---

## 🔐 How It Works

1. **Select** any file on your device you want to protect
2. **Encrypt** it with a single tap — the file becomes invisible and inaccessible to anyone else
3. **Authenticate** with your fingerprint or draw your secret lock pattern to decrypt and view your files anytime
4. **No password. No PIN. Just you.**

---

## ✨ Features

- 🔒 **Fingerprint & Pattern unlock** — authenticate via fingerprint sensor or your own drawn lock pattern
- 🧩 **AES/CTR/NoPadding encryption** — industry-standard symmetric encryption
- 👁️ **File hiding** — encrypted files are concealed from the standard file system view
- 📁 **Supports multiple file types** — documents, images, videos, and more
- ⚡ **Fast & lightweight** — minimal battery and storage overhead
- 🚫 **No cloud, no sync** — everything stays on your device, fully offline

---

## 🛡️ Security

Ghost Files uses the **AES (Advanced Encryption Standard)** algorithm in **CTR (Counter) mode** with **NoPadding**, providing:

- **Stream cipher behavior** — encrypts data block by block without requiring padding
- **Biometric key binding** — the encryption key is protected by the Android Keystore and only released upon successful fingerprint authentication or pattern verification
- **Hardware-backed security** — leverages the device's Trusted Execution Environment (TEE) where available

> ⚠️ **Important:** Biometric data is never stored or transmitted by the app. Fingerprint authentication is handled entirely by the Android BiometricPrompt API and the device's secure hardware. Lock patterns are stored in encrypted form on-device only.

---

## 📱 Requirements

| Requirement | Minimum |
|---|---|
| Android Version | Android 6.0 (API 23)+ |
| Biometric Hardware | Fingerprint sensor (optional — pattern unlock works without it) |
| Storage Permission | Required for file access |

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/ghost-files.git
   ```
2. Open the project in **Android Studio**
3. Build and run on a physical device (biometrics require real hardware)
4. Register your biometrics in device Settings if not already done
5. Launch Ghost Files and start protecting your files

---

## 🏗️ Tech Stack

- **Language:** Kotlin
- **Encryption:** `javax.crypto` — AES/CTR/NoPadding
- **Biometrics:** AndroidX `BiometricPrompt` API
- **Pattern Auth:** Custom drawn lock pattern with on-device encrypted storage
- **Key Storage:** Android Keystore System
- **Min SDK:** 23 | **Target SDK:** 34+

---

## ⚠️ Disclaimer

Ghost Files is intended for **personal privacy protection** of your own files. The developer is not responsible for any misuse of this application. Always comply with applicable laws and regulations in your jurisdiction.

---

## 📄 License

```
MIT License — feel free to use, modify, and distribute.
```

---

<p align="center">
  Made with 🖤 for privacy
</p>
