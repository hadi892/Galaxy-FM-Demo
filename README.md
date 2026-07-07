# Galaxy FM Demo 📻 (Snapdragon 695 Diagnostic Engine)

A complete, production-quality, low-level hardware diagnostics and HAL validation application built for **Android 16 (API 36)** targeting the **Samsung Galaxy Tab A9+ 5G (SM-X216B)** powered by the **Qualcomm Snapdragon 695 SoC (SM6375)**.

This utility is a **REAL hardware debugger and diagnostic scanner**. It contains no mock data, no simulator stubs, and never bypasses Android Sandbox or SELinux policies. It scans, reports, and executes real, raw dynamic loading and framework reflection routines to determine the active capabilities of Qualcomm FM tuner hardware.

---

## 🎨 Design Concept: "Qualcomm Slate Cyberpunk"
The application utilizes a dark theme designed for engineers, using high-contrast slate-colored dynamic cards, electric blue active telemetry gauges, and vivid neon-green state status indicators. Every touch target exceeds standard size guidelines (min 48dp), and layout paddings adhere to the strict Material Design 3 8dp density grid.

---

## 🔍 Diagnostics Audit Vector

The diagnostic engine performs deep inspections across 5 security and driver layers:

### 1. App Permission Sandbox
Checks declaration and runtime grant status for hardware boundary permissions:
* `android.permission.ACCESS_FM_RADIO` (Proprietary OEM hardware gate)
* `android.permission.ACCESS_FINE_LOCATION` (Station seeking capability)
* `android.permission.MODIFY_AUDIO_SETTINGS` (Tuner audio-out routing)
* `android.permission.RECORD_AUDIO` (Tuner line-in recording)

### 2. Linux Kernel Driver Nodes
Directly queries the file system for kernel driver entry nodes:
* `/dev/radio0` (Primary V4L2 Snapdragon tuner node)
* `/dev/fm` (Qualcomm custom driver bus)
* `/dev/fmradio` (AOSP interface node)

### 3. Proprietary Linker Shared Libraries (`.so`)
Locates and attempts to dynamically load Qualcomm's proprietary hardware library layers directly from `/vendor/lib/` and `/vendor/lib64/` using `System.load()`:
* `vendor.qti.hardware.fm@1.0.so` (HIDL interface wrapper)
* `vendor.qti.hardware.fm@1.0-impl.so` (HIDL service binder implementation)
* `libfmpal.so` (FM platform abstraction layer)
* `ftm_fm_lib.so` (Factory Test Mode interface library)

### 4. HIDL Binder Service Availability
Uses dynamic class reflection on AOSP's `android.os.HwBinder` to query the service manager if `vendor.qti.hardware.fm@1.0::IFmHci` is active and reachable within the device HAL registry.

### 5. OEM Framework Classpath Reflection
Checks whether the underlying custom Android ROM contains the Java package bindings for the Qualcomm FM framework layer:
* `qcom.fmradio.FmReceiver` (Primary hardware controller class)
* `qcom.fmradio.FmTransmitter`
* `android.hardware.fmradio.FmReceiver`
* `com.caf.fmradio.FmReceiver` (Qualcomm CAF platform package)

---

## 🛠️ System Control Panel

The Control Center provides real hardware interactions via reflection bindings mapped directly to Qualcomm's proprietary client SDK methods (`enable()`, `acquire()`, `setStation()`, `searchStations()`, `getRssi()`, and `getRdsText()`).

* **Initialize FM:** Dynamically loads the framework classloader.
* **Power ON/OFF:** Powers the hardware receiver register.
* **Tuning (MHz):** Directs the tuner PLL frequency synthesizer (87.5 MHz - 108.0 MHz).
* **Seek Up/Down:** Commands the Qualcomm DSP to scan for adjacent carrier peaks.
* **Telemetry (RSSI/RDS):** Pulls raw Signal-to-Noise/RSSI dBm registers and decodes the RDS text payload from the RDS carrier sub-channel.

*Note: On consumer ROMs, non-system applications are blocked by default from accessing `/dev/radio0` or `/vendor/lib/` due to SELinux policies. If these barriers block execution, this app reports the **exact native link errors or sandbox security exceptions** in real-time, providing 100% honest transparency without any simulation.*

---

## 🚀 GitHub Actions Continuous Integration (CI)

This repository includes a production-grade **CI Workflow** under `.github/workflows/build.yml` that:
* Runs on `ubuntu-latest` with **Temurin JDK 21**.
* Validates and executes using **Gradle 9.3.1**.
* Automatically runs on `push`, `pull_request`, and manually on `workflow_dispatch`.
* **Zero Signing Requirements:** Generates full Debug and Release APK/AAB outputs. If custom release keystores are not configured in the environment, the build system automatically falls back to standard debug signing.
* Uploads build artifacts automatically (APKs, AABs, ProGuard Obfuscation Maps, and reports).

### Compilation Commands

To build the APKs and Bundles manually on your workstation:
```bash
# Compile and package Debug APK and Android App Bundle (AAB)
./gradlew assembleDebug bundleDebug

# Compile and package signed Release APK and Android App Bundle (AAB)
./gradlew assembleRelease bundleRelease
```

---

## 📂 Project Structure

```
.
├── .github/workflows/       # GitHub Actions Continuous Integration
│   └── build.yml
├── app/
│   ├── build.gradle.kts     # Main App Module configuration
│   ├── proguard-rules.pro   # ProGuard keep rules for Qualcomm Reflection
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml   # Permission and activity declarations
│           └── java/com/example/     # Main Application package
│               ├── MainActivity.kt   # App Entry Point (Compose binding)
│               ├── FmViewModel.kt    # Hardware state machine and logger
│               ├── FmDiagnosticEngine.kt # Kernel, binder and library scanner
│               └── FmDashboard.kt    # UI Layout (Cyberpunk diagnostics theme)
└── build.gradle.kts         # Root project build configuration
```

---

## 📜 License
Licensed under the [MIT License](LICENSE).
