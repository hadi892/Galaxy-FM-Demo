package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * Result structure representing the diagnostic state of an individual Qualcomm library or component.
 */
data class ComponentStatus(
    val name: String,
    val isPresent: Boolean,
    val path: String = "N/A",
    val error: String? = null,
    val isLoaded: Boolean = false
)

/**
 * Robust Hardware Diagnostic Engine for Qualcomm FM components on Android.
 * This class performs actual, raw inspections of system properties, device nodes, shared libraries,
 * and HAL services. It does not mock or simulate success.
 */
object FmDiagnosticEngine {
    private const val TAG = "FmDiagnosticEngine"

    // Paths where Qualcomm FM shared libraries are usually located on Snapdragon devices
    private val LIB_SEARCH_PATHS = listOf(
        "/vendor/lib64/",
        "/vendor/lib64/hw/",
        "/vendor/lib64/soundfx/",
        "/system/lib64/",
        "/vendor/lib/",
        "/vendor/lib/hw/",
        "/vendor/lib/soundfx/",
        "/system/lib/"
    )

    // Hardware device nodes for FM on Snapdragon SOCs
    private val FM_DEVICE_NODES = listOf(
        "/dev/radio0",
        "/dev/fm",
        "/dev/fmradio"
    )

    // Qualcomm/CAF FM Framework Class names
    private val FM_FRAMEWORK_CLASSES = listOf(
        "qcom.fmradio.FmReceiver",
        "qcom.fmradio.FmTransmitter",
        "android.hardware.fmradio.FmReceiver",
        "com.caf.fmradio.FmReceiver",
        "com.caf.fmradio.FmRxControls"
    )

    /**
     * Reads System Properties using reflection to inspect Snapdragon SoC details.
     */
    fun getSystemProperty(key: String): String {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as? String ?: ""
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    /**
     * Determines the current SELinux execution state.
     */
    fun getSELinuxStatus(): String {
        // Method 1: Check getenforce binary
        try {
            val process = Runtime.getRuntime().exec("getenforce")
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                val line = reader.readLine()
                if (!line.isNullOrBlank()) return line.trim()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to run getenforce: ${e.message}")
        }

        // Method 2: Check standard enforce file status
        return try {
            val enforceFile = File("/sys/fs/selinux/enforce")
            if (enforceFile.exists()) {
                val value = enforceFile.readText().trim()
                if (value == "1") "Enforcing" else "Permissive"
            } else {
                "Unknown (Could not read enforce file)"
            }
        } catch (e: Exception) {
            "Enforcing (Access restricted: ${e.localizedMessage})"
        }
    }

    /**
     * Checks if the required system permission declarations are present in the AndroidManifest.xml
     * and queries their current grant state.
     */
    fun checkPermissions(context: Context): List<ComponentStatus> {
        val permissionsToCheck = listOf(
            "android.permission.ACCESS_FM_RADIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO"
        )
        return permissionsToCheck.map { permission ->
            val isGranted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            val isDeclared = try {
                val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                info.requestedPermissions?.contains(permission) == true
            } catch (e: Exception) {
                false
            }

            val errorMsg = when {
                !isDeclared -> "Not Declared in AndroidManifest"
                !isGranted -> "Declared but NOT Granted by User/System"
                else -> null
            }

            ComponentStatus(
                name = permission,
                isPresent = isDeclared,
                error = errorMsg,
                isLoaded = isGranted
            )
        }
    }

    /**
     * Audits the physical/virtual device nodes created by the Qualcomm kernel driver.
     */
    fun auditDeviceNodes(): List<ComponentStatus> {
        return FM_DEVICE_NODES.map { path ->
            val file = File(path)
            val exists = file.exists()
            var error: String? = null
            var canRead = false
            if (exists) {
                try {
                    canRead = file.canRead()
                    if (!canRead) {
                        error = "File exists but is NOT readable (SELinux/Unix Permission Denied)"
                    }
                } catch (e: Exception) {
                    error = e.localizedMessage
                }
            } else {
                error = "Device node not found (Driver not loaded by Kernel)"
            }

            ComponentStatus(
                name = path,
                isPresent = exists,
                path = path,
                error = error,
                isLoaded = canRead
            )
        }
    }

    /**
     * Searches for, diagnostics, and attempts to dynamically load Qualcomm FM shared libraries.
     */
    fun auditAndLoadLibraries(): List<ComponentStatus> {
        val targetLibs = listOf(
            "vendor.qti.hardware.fm@1.0.so",
            "vendor.qti.hardware.fm@1.0-impl.so",
            "libfmpal.so",
            "ftm_fm_lib.so"
        )

        return targetLibs.map { libName ->
            var foundFile: File? = null
            // Check in standard directory hierarchies
            for (searchPath in LIB_SEARCH_PATHS) {
                val file = File(searchPath + libName)
                if (file.exists()) {
                    foundFile = file
                    break
                }
            }

            if (foundFile == null) {
                ComponentStatus(
                    name = libName,
                    isPresent = false,
                    error = "Library not found in vendor or system library search paths."
                )
            } else {
                var loaded = false
                var errorMsg: String? = null
                try {
                    // Attempt dynamic loading of the absolute system library file
                    System.load(foundFile.absolutePath)
                    loaded = true
                } catch (err: UnsatisfiedLinkError) {
                    errorMsg = "Linker failed: ${err.message}"
                    Log.e(TAG, "Failed to load ${foundFile.absolutePath}: ${err.message}")
                } catch (e: Exception) {
                    errorMsg = "Error: ${e.message}"
                    Log.e(TAG, "Exception loading ${foundFile.absolutePath}: ${e.message}")
                }

                ComponentStatus(
                    name = libName,
                    isPresent = true,
                    path = foundFile.absolutePath,
                    error = errorMsg,
                    isLoaded = loaded
                )
            }
        }
    }

    /**
     * Inspects AOSP / OEM HwBinder registry to determine if the Qualcomm FM HIDL service
     * is registered and accessible.
     */
    fun checkHidlHalStatus(): ComponentStatus {
        val serviceName = "vendor.qti.hardware.fm@1.0::IFmHci"
        return try {
            val hwBinderClass = Class.forName("android.os.HwBinder")
            val getServiceMethod = hwBinderClass.getMethod("getService", String::class.java, String::class.java)
            val service = getServiceMethod.invoke(null, serviceName, "default")
            
            if (service != null) {
                ComponentStatus(
                    name = serviceName,
                    isPresent = true,
                    path = "HwBinder Registry",
                    isLoaded = true
                )
            } else {
                ComponentStatus(
                    name = serviceName,
                    isPresent = false,
                    error = "HwBinder returned null (Service is registered but inaccessible or not active)"
                )
            }
        } catch (e: ClassNotFoundException) {
            ComponentStatus(
                name = serviceName,
                isPresent = false,
                error = "HwBinder API not supported on this Android framework version."
            )
        } catch (e: Exception) {
            ComponentStatus(
                name = serviceName,
                isPresent = false,
                error = "Exception checking HIDL: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Checks if the Android framework includes native Qualcomm FM Java classes.
     */
    fun auditFrameworkClasses(): List<ComponentStatus> {
        return FM_FRAMEWORK_CLASSES.map { className ->
            var isPresent = false
            var error: String? = null
            try {
                Class.forName(className)
                isPresent = true
            } catch (e: ClassNotFoundException) {
                error = "Class not found in system classpath (Framework API missing)"
            } catch (e: Exception) {
                error = e.localizedMessage
            }

            ComponentStatus(
                name = className,
                isPresent = isPresent,
                error = error,
                isLoaded = isPresent
            )
        }
    }

    /**
     * Compiles a comprehensive technical diagnostic report.
     */
    fun generateDiagnosticReport(context: Context): String {
        val separator = "========================================\n"
        val sb = StringBuilder()
        sb.append(separator)
        sb.append("   QUALCOMM FM DIAGNOSTICS REPORT\n")
        sb.append(separator)
        sb.append("Timestamp:       ${java.time.OffsetDateTime.now()}\n")
        sb.append("Device Manufacturer: ${Build.MANUFACTURER}\n")
        sb.append("Device Brand:        ${Build.BRAND}\n")
        sb.append("Device Model:        ${Build.MODEL} (SM-X216B Target)\n")
        sb.append("Device Hardware:     ${Build.HARDWARE}\n")
        sb.append("Device Board:        ${Build.BOARD}\n")
        sb.append("Platform SoC:        ${getSystemProperty("ro.board.platform")} (Snapdragon 695 Target)\n")
        sb.append("Android SDK Level:   ${Build.VERSION.SDK_INT} (Android 16 API 36 Target)\n")
        sb.append("SELinux Enforce State: ${getSELinuxStatus()}\n")
        sb.append(separator)

        sb.append("\n[1] PERMISSIONS AUDIT\n")
        checkPermissions(context).forEach {
            sb.append("• ${it.name}\n")
            sb.append("  Declared: ${it.isPresent} | Granted: ${it.isLoaded}\n")
            if (it.error != null) sb.append("  Error: ${it.error}\n")
        }

        sb.append("\n[2] KERNEL HARDWARE DEVICE NODES\n")
        auditDeviceNodes().forEach {
            sb.append("• ${it.name}\n")
            sb.append("  Driver Created: ${it.isPresent} | Readable: ${it.isLoaded}\n")
            if (it.error != null) sb.append("  Error: ${it.error}\n")
        }

        sb.append("\n[3] PROPRIETARY SYSTEM SHARED LIBRARIES\n")
        auditAndLoadLibraries().forEach {
            sb.append("• ${it.name}\n")
            sb.append("  Located: ${it.isPresent}\n")
            if (it.isPresent) sb.append("  Path: ${it.path}\n")
            sb.append("  Successfully Loaded: ${it.isLoaded}\n")
            if (it.error != null) sb.append("  Linker Error: ${it.error}\n")
        }

        sb.append("\n[4] HIDL FM SERVICE DIAGNOSTICS\n")
        val hidl = checkHidlHalStatus()
        sb.append("• ${hidl.name}\n")
        sb.append("  HAL Active: ${hidl.isPresent}\n")
        if (hidl.error != null) sb.append("  HAL Error: ${hidl.error}\n")

        sb.append("\n[5] FRAMEWORK CLASS CLASSPATH CHECK\n")
        auditFrameworkClasses().forEach {
            sb.append("• ${it.name}\n")
            sb.append("  In Classpath: ${it.isPresent}\n")
            if (it.error != null) sb.append("  ClassLoader Error: ${it.error}\n")
        }

        sb.append("\n")
        sb.append(separator)
        sb.append("           DIAGNOSTIC COMPLETE\n")
        sb.append(separator)

        return sb.toString()
    }
}
