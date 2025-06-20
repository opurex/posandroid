# 🧾 Opurex POS Suite

> A powerful hybrid Point of Sale system for Android, Desktop (Java), and Cloud SaaS environments  
> Maintained by **Opurex Ortus**

🌐 Website: [www.opurex.com](http://www.opurex.com)  
📊 SaaS Dashboard: [https://pos.opurex.com](https://pos.opurex.com)  
🔒 License: [Custom Proprietary License + GPL attributions](#-licensing-summary)  
📁 Repositories:
- 🏢 Private: [repo.opurex.com](https://repo.opurex.com)
- 🌍 Public mirror: [github.com/opurex](https://github.com/opurex)

---

## 📢 Licensing Notice

> ⚠️ Important: As of the latest versions, **Opurex POS is no longer fully licensed under GPL**.

While parts of the codebase are derived from GPL-licensed projects (and are properly attributed), **the majority of current modules, business logic, and interface code are now licensed under a private Opurex License**.

This means:
- 🔐 Some features are closed-source or privately compiled
- 🤝 Commercial use requires permission or license from Opurex
- 🧾 GPL components are honored with proper acknowledgment and source disclosure where applicable

---

## 🧩 What is Opurex POS?

**Opurex POS** is a multi-platform retail Point of Sale solution for cooperatives, supermarkets, restaurants, and multi-branch enterprises.

It includes:
- 📱 Android POS clients (TPOS, J100, Sunmi, etc.)
- 💻 Java desktop POS for Linux, Windows, and macOS
- ☁️ Web-based SaaS dashboard ([pos.opurex.com](https://pos.opurex.com))

---

## 🚀 Tech Stack

| Platform     | Language / Tools              |
|--------------|-------------------------------|
| Android POS  | Java 24, Gradle, Android SDK  |
| Desktop POS  | Java 24 (JRE-compatible)      |
| Web Backend  | Python/Django, PostgreSQL     |
| Deployment   | Docker, Linux, CI/CD pipelines|

🧪 Fully tested on Java **24**.

---

## ⚙️ Quick Start (Android POS)

```bash
git clone https://repo.opurex.com/pos/opurex-android.git
cd opurex-android
export ANDROID_HOME=/path/to/android/sdk
./gradlew installVanillaDebug
```

---

## 🧪 Build Variants

| Build Type | Flavor   | Description              |
|------------|----------|--------------------------|
| Debug      | Vanilla  | Standard testing version |
| Release    | Bnp      | Business version         |
| Release    | Wcr      | Wholesale/Retail version |

Build commands:

```bash
./gradlew assembleVanillaDebug
./gradlew installVanillaDebug
```

---

## 🔐 Licensing Summary

| Component                  | License                                |
|---------------------------|----------------------------------------|
| Android POS (latest)      | 🔒 Custom Proprietary License (Opurex) |
| Java Desktop POS          | 🔒 Custom Proprietary License (Opurex) |
| Web SaaS Dashboard        | 🔒 Commercial SaaS (Private)           |
| Open-source libraries     | ✅ GPL/MIT/Apache (see `LICENSES.md`)  |

> **Note:** If you modify or redistribute any component derived from GPL, you must comply with the respective GPL terms for that component.

We maintain a clear separation between GPL components and proprietary modules. Attribution and access to GPL-licensed components will be published in `LICENSES.md`.

---

## 📤 Deployment Targets

- ✅ Android Terminals (J100, TPOS, Sunmi, etc.)
- ✅ Desktop Java (Linux, macOS, Windows — Java 24 compatible)
- ✅ Cloud Backend (SaaS at [pos.opurex.com](https://pos.opurex.com))

---

## 🤝 Contributions

We accept:

- Bug reports
- Feature requests
- Community testing

📌 **Code contributions are limited** due to licensing restrictions. For collaboration or partner access, contact us directly.

---

## 📬 Contact

📧 hello@opurex.com  
🔗 [www.opurex.com](http://www.opurex.com)  
🔒 For licensing & commercial inquiries, email us.

---

## 🛡 Disclaimer

This software includes and respects third-party open-source code. While some internal modules are protected and proprietary, we maintain full transparency and GPL compliance where required.