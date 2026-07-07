package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FmViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _permissionsStatus = MutableStateFlow<List<ComponentStatus>>(emptyList())
    val permissionsStatus: StateFlow<List<ComponentStatus>> = _permissionsStatus.asStateFlow()

    private val _deviceNodesStatus = MutableStateFlow<List<ComponentStatus>>(emptyList())
    val deviceNodesStatus: StateFlow<List<ComponentStatus>> = _deviceNodesStatus.asStateFlow()

    private val _librariesStatus = MutableStateFlow<List<ComponentStatus>>(emptyList())
    val librariesStatus: StateFlow<List<ComponentStatus>> = _librariesStatus.asStateFlow()

    private val _halStatus = MutableStateFlow<ComponentStatus>(
        ComponentStatus("vendor.qti.hardware.fm@1.0::IFmHci", false, error = "Not Checked Yet")
    )
    val halStatus: StateFlow<ComponentStatus> = _halStatus.asStateFlow()

    private val _classesStatus = MutableStateFlow<List<ComponentStatus>>(emptyList())
    val classesStatus: StateFlow<List<ComponentStatus>> = _classesStatus.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<String>("")
    val diagnosticReport: StateFlow<String> = _diagnosticReport.asStateFlow()

    // Real-time hardware controller states (based on Qualcomm Framework reflection)
    private var fmReceiverInstance: Any? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isPoweredOn = MutableStateFlow(false)
    val isPoweredOn: StateFlow<Boolean> = _isPoweredOn.asStateFlow()

    private val _currentFrequency = MutableStateFlow(98.1) // Default starting frequency in MHz
    val currentFrequency: StateFlow<Double> = _currentFrequency.asStateFlow()

    private val _rssi = MutableStateFlow<Int?>(null)
    val rssi: StateFlow<Int?> = _rssi.asStateFlow()

    private val _rdsText = MutableStateFlow<String?>(null)
    val rdsText: StateFlow<String?> = _rdsText.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    init {
        addLog("Galaxy FM Demo Diagnostic ViewModel Initialized.")
    }

    fun addLog(message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        val formattedLog = "[$timestamp] $message"
        Log.d("GalaxyFmLog", formattedLog)
        _logs.update { currentList ->
            currentList + formattedLog
        }
    }

    /**
     * Executes a complete and honest diagnostic scan of all device layers.
     */
    fun runFullDiagnostic(context: Context) {
        viewModelScope.launch {
            addLog("Starting full hardware and HAL diagnostic audit...")

            // 1. Audit SELinux & System Properties
            val platform = FmDiagnosticEngine.getSystemProperty("ro.board.platform")
            val selinux = FmDiagnosticEngine.getSELinuxStatus()
            addLog("Detected SoC Platform: $platform (Snapdragon 695 Target)")
            addLog("SELinux Status: $selinux")

            // 2. Audit Permissions
            addLog("Auditing Android manifest permissions...")
            val permCheck = FmDiagnosticEngine.checkPermissions(context)
            _permissionsStatus.value = permCheck
            permCheck.forEach {
                addLog("Permission: ${it.name} | Granted: ${it.isLoaded} | Error: ${it.error ?: "None"}")
            }

            // 3. Audit Kernel Device Nodes
            addLog("Auditing Linux kernel device nodes...")
            val nodeCheck = FmDiagnosticEngine.auditDeviceNodes()
            _deviceNodesStatus.value = nodeCheck
            nodeCheck.forEach {
                addLog("Device: ${it.name} | Present: ${it.isPresent} | Read Access: ${it.isLoaded} | Error: ${it.error ?: "None"}")
            }

            // 4. Audit Proprietary Shared Libraries
            addLog("Searching for Qualcomm proprietary FM shared (.so) libraries...")
            val libCheck = FmDiagnosticEngine.auditAndLoadLibraries()
            _librariesStatus.value = libCheck
            libCheck.forEach {
                if (it.isPresent) {
                    addLog("Library: ${it.name} | Located at: ${it.path} | Loaded: ${it.isLoaded} | Error: ${it.error ?: "None"}")
                } else {
                    addLog("Library: ${it.name} | NOT found on storage. | Error: ${it.error}")
                }
            }

            // 5. Audit HwBinder HIDL service
            addLog("Auditing Qualcomm HIDL FM HAL service...")
            val halCheck = FmDiagnosticEngine.checkHidlHalStatus()
            _halStatus.value = halCheck
            addLog("HAL Service: ${halCheck.name} | Present: ${halCheck.isPresent} | Error: ${halCheck.error ?: "None"}")

            // 6. Audit Framework Classpath
            addLog("Auditing classpath for proprietary AOSP/Qualcomm FM Java classes...")
            val classCheck = FmDiagnosticEngine.auditFrameworkClasses()
            _classesStatus.value = classCheck
            classCheck.forEach {
                addLog("Class: ${it.name} | Available: ${it.isPresent} | Error: ${it.error ?: "None"}")
            }

            // 7. Compile Final Raw Technical Report
            addLog("Compiling consolidated diagnostics report...")
            _diagnosticReport.value = FmDiagnosticEngine.generateDiagnosticReport(context)
            addLog("Diagnostics report successfully completed.")
        }
    }

    /**
     * Attempts to instantiate the Qualcomm/CAF FM Receiver framework class via reflection.
     */
    fun initializeFmReceiver() {
        addLog("Attempting to initialize Qualcomm FM Receiver...")
        try {
            // Find an available framework class to instantiate
            val classNames = listOf(
                "qcom.fmradio.FmReceiver",
                "android.hardware.fmradio.FmReceiver",
                "com.caf.fmradio.FmReceiver"
            )

            var targetClass: Class<*>? = null
            for (name in classNames) {
                try {
                    targetClass = Class.forName(name)
                    addLog("Found system FM Receiver class: $name")
                    break
                } catch (e: ClassNotFoundException) {
                    // Try next class
                }
            }

            if (targetClass == null) {
                throw ClassNotFoundException("No Qualcomm/AOSP native FM Receiver Java classes exist in the current system classpath.")
            }

            // Try to find typical constructors
            val constructor = try {
                // constructor taking path and callback (common on qcom)
                targetClass.getConstructor(String::class.java, Object::class.java)
            } catch (e: Exception) {
                try {
                    // default constructor
                    targetClass.getConstructor()
                } catch (e: Exception) {
                    addLog("No standard constructor found: ${e.message}")
                    null
                }
            }

            if (constructor != null) {
                fmReceiverInstance = if (constructor.parameterCount == 2) {
                    constructor.newInstance("/dev/radio0", null)
                } else {
                    constructor.newInstance()
                }
                _isInitialized.value = true
                addLog("Success: Qualcomm FM Receiver class instantiated successfully!")
            } else {
                throw IllegalAccessException("Failed to resolve a compatible public constructor for ${targetClass.name}")
            }

        } catch (e: UnsatisfiedLinkError) {
            addLog("JNI Error: Dynamic native linker failure when binding JNI methods for the class. Detail: ${e.localizedMessage}")
            _isInitialized.value = false
        } catch (e: ClassNotFoundException) {
            addLog("Unsupported Hardware Error: No compatible FM Receiver API classes exist on this ROM. Detail: ${e.localizedMessage}")
            _isInitialized.value = false
        } catch (e: SecurityException) {
            addLog("Permission Error: Java security manager denied creation. Detail: ${e.localizedMessage}")
            _isInitialized.value = false
        } catch (e: Exception) {
            addLog("Exception during FM Receiver initialization: ${e.javaClass.simpleName} - ${e.localizedMessage}")
            _isInitialized.value = false
        }
    }

    /**
     * Attempts to power ON the FM hardware receiver.
     */
    fun powerOnFm() {
        addLog("Attempting to power ON Qualcomm FM Radio...")
        val instance = fmReceiverInstance
        if (instance == null) {
            addLog("HAL Error: FM Receiver is not initialized. Please click 'Initialize FM' first.")
            return
        }

        try {
            // Standard Qualcomm API uses .enable() or .acquire() to power on hardware
            val methods = instance.javaClass.methods
            var powerMethod = methods.find { it.name == "enable" || it.name == "acquire" || it.name == "powerOn" }
            
            if (powerMethod == null) {
                throw NoSuchMethodException("Could not find power-on method (enable/acquire/powerOn) in ${instance.javaClass.name}")
            }

            addLog("Invoking power-on method: ${powerMethod.name}()")
            powerMethod.isAccessible = true
            
            // Invoke enable
            val result = if (powerMethod.parameterCount == 1) {
                // Some versions require a config object or bundle
                powerMethod.invoke(instance, null)
            } else {
                powerMethod.invoke(instance)
            }

            addLog("Power-on execution returned: $result")
            _isPoweredOn.value = true
            addLog("Success: FM Radio powered ON successfully.")
            
        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            addLog("Power ON Failed: ${rootCause.javaClass.simpleName} - ${rootCause.localizedMessage}")
            _isPoweredOn.value = false
        }
    }

    /**
     * Attempts to power OFF the FM hardware receiver.
     */
    fun powerOffFm() {
        addLog("Attempting to power OFF Qualcomm FM Radio...")
        val instance = fmReceiverInstance
        if (instance == null) {
            addLog("HAL Error: FM Receiver is not initialized.")
            return
        }

        try {
            val methods = instance.javaClass.methods
            var powerOffMethod = methods.find { it.name == "disable" || it.name == "release" || it.name == "powerOff" }

            if (powerOffMethod == null) {
                throw NoSuchMethodException("Could not find power-off method (disable/release/powerOff) in ${instance.javaClass.name}")
            }

            addLog("Invoking power-off method: ${powerOffMethod.name}()")
            powerOffMethod.isAccessible = true
            powerOffMethod.invoke(instance)

            _isPoweredOn.value = false
            _rssi.value = null
            _rdsText.value = null
            addLog("Success: FM Radio powered OFF.")
            
        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            addLog("Power OFF Failed: ${rootCause.javaClass.simpleName} - ${rootCause.localizedMessage}")
        }
    }

    /**
     * Tunes FM Radio to a specific frequency.
     */
    fun tuneFrequency(freqMhz: Double) {
        addLog("Attempting to TUNE to frequency: $freqMhz MHz")
        val instance = fmReceiverInstance
        if (instance == null || !_isPoweredOn.value) {
            addLog("HAL Error: FM Receiver is either not initialized or powered OFF.")
            return
        }

        try {
            val methods = instance.javaClass.methods
            // Qualcomm uses kHz internally (e.g. 98.1 MHz = 98100 kHz)
            val khz = (freqMhz * 1000).toInt()
            
            var tuneMethod = methods.find { it.name == "setStation" || it.name == "tune" || it.name == "setFrequency" }

            if (tuneMethod == null) {
                throw NoSuchMethodException("Could not find tuning method (setStation/tune/setFrequency) in ${instance.javaClass.name}")
            }

            addLog("Invoking tuning method: ${tuneMethod.name}($khz kHz)")
            tuneMethod.isAccessible = true
            
            val result = if (tuneMethod.parameterTypes.firstOrNull() == Double::class.java) {
                tuneMethod.invoke(instance, freqMhz)
            } else {
                tuneMethod.invoke(instance, khz)
            }

            _currentFrequency.value = freqMhz
            addLog("Tuning returned: $result. Frequency updated to $freqMhz MHz.")

            // Read hardware RSSI automatically after tuning if possible
            readRssi()

        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            addLog("Tune Failed: ${rootCause.javaClass.simpleName} - ${rootCause.localizedMessage}")
        }
    }

    /**
     * Performs a scan or seek operation.
     */
    fun seek(up: Boolean) {
        val direction = if (up) "UP" else "DOWN"
        addLog("Attempting to SEEK station: $direction")
        val instance = fmReceiverInstance
        if (instance == null || !_isPoweredOn.value) {
            addLog("HAL Error: FM Receiver is either not initialized or powered OFF.")
            return
        }

        try {
            val methods = instance.javaClass.methods
            // seek/searchStation methods usually accept directional parameters or ints
            var seekMethod = methods.find { it.name == "searchStations" || it.name == "seek" || it.name == "search" }

            if (seekMethod == null) {
                throw NoSuchMethodException("Could not find seek method (searchStations/seek) in ${instance.javaClass.name}")
            }

            addLog("Invoking seek method: ${seekMethod.name}() with direction $direction")
            seekMethod.isAccessible = true

            // Qualcomm searchStations takes params (int mode, int dwell, int direction) or similar
            val result = when (seekMethod.parameterCount) {
                0 -> seekMethod.invoke(instance)
                1 -> seekMethod.invoke(instance, if (up) 1 else 0)
                else -> {
                    // Typical Qualcomm params: scanMode=1, dwellPeriod=2, direction=up(0)/down(1)
                    seekMethod.invoke(instance, 1, 2, if (up) 0 else 1)
                }
            }

            addLog("Seek operation triggered. Return status: $result")

        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            addLog("Seek Failed: ${rootCause.javaClass.simpleName} - ${rootCause.localizedMessage}")
        }
    }

    /**
     * Scans for all available stations.
     */
    fun scanStations() {
        addLog("Attempting to SCAN the entire FM band (87.5 - 108.0 MHz)...")
        val instance = fmReceiverInstance
        if (instance == null || !_isPoweredOn.value) {
            addLog("HAL Error: FM Receiver is either not initialized or powered OFF.")
            return
        }

        try {
            val methods = instance.javaClass.methods
            var scanMethod = methods.find { it.name == "startScan" || it.name == "autoScan" || it.name == "scan" }

            if (scanMethod == null) {
                throw NoSuchMethodException("Could not find scan method (startScan/autoScan/scan) in ${instance.javaClass.name}")
            }

            addLog("Invoking scan method: ${scanMethod.name}()")
            scanMethod.isAccessible = true
            val result = scanMethod.invoke(instance)
            addLog("Scan initiated. Return status: $result")

        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            addLog("Scan Failed: ${rootCause.javaClass.simpleName} - ${rootCause.localizedMessage}")
        }
    }

    /**
     * Reads RSSI (Received Signal Strength Indicator).
     */
    fun readRssi() {
        addLog("Attempting to read RSSI signal strength from Qualcomm hardware register...")
        val instance = fmReceiverInstance
        if (instance == null || !_isPoweredOn.value) {
            addLog("HAL Error: FM Receiver is either not initialized or powered OFF.")
            return
        }

        try {
            val methods = instance.javaClass.methods
            var rssiMethod = methods.find { it.name == "getRssi" || it.name == "getSignalStrength" || it.name == "getRssiDbm" }

            if (rssiMethod == null) {
                throw NoSuchMethodException("Could not find RSSI read method (getRssi/getSignalStrength) in ${instance.javaClass.name}")
            }

            addLog("Invoking RSSI method: ${rssiMethod.name}()")
            rssiMethod.isAccessible = true
            val valResult = rssiMethod.invoke(instance) as? Int ?: 0
            _rssi.value = valResult
            addLog("Success: Read hardware RSSI: $valResult dBm")

        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            addLog("Read RSSI Failed: ${rootCause.javaClass.simpleName} - ${rootCause.localizedMessage}")
            _rssi.value = null
        }
    }

    /**
     * Reads RDS (Radio Data System) data.
     */
    fun readRds() {
        addLog("Attempting to read RDS text payload...")
        val instance = fmReceiverInstance
        if (instance == null || !_isPoweredOn.value) {
            addLog("HAL Error: FM Receiver is either not initialized or powered OFF.")
            return
        }

        try {
            val methods = instance.javaClass.methods
            var rdsMethod = methods.find { it.name == "getRdsText" || it.name == "getProgramService" || it.name == "getRadioText" }

            if (rdsMethod == null) {
                throw NoSuchMethodException("Could not find RDS method (getRdsText/getProgramService/getRadioText) in ${instance.javaClass.name}")
            }

            addLog("Invoking RDS method: ${rdsMethod.name}()")
            rdsMethod.isAccessible = true
            val valResult = rdsMethod.invoke(instance) as? String ?: ""
            _rdsText.value = valResult.ifBlank { "No active RDS data payload received." }
            addLog("Success: Read hardware RDS: '${_rdsText.value}'")

        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            addLog("Read RDS Failed: ${rootCause.javaClass.simpleName} - ${rootCause.localizedMessage}")
            _rdsText.value = null
        }
    }

    /**
     * Exports the raw diagnostics report as a shareable plain text payload.
     */
    fun exportDiagnosticReport(context: Context) {
        val report = _diagnosticReport.value
        if (report.isBlank()) {
            addLog("Error: Report is empty. Run diagnostic scan first!")
            return
        }

        try {
            addLog("Preparing export intent for share sheet...")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Qualcomm Snapdragon FM Hardware Diagnostics Report")
                putExtra(Intent.EXTRA_TEXT, report)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Diagnostic Report"))
            addLog("Success: Shared diagnostics report successfully.")
        } catch (e: Exception) {
            addLog("Export Report Failed: ${e.localizedMessage}")
        }
    }
}
