# ProGuard rules for Galaxy FM Demo
# Prevents obfuscation of classes used via reflection in hardware diagnostic routines

# Keep Qualcomm, CAF, and Android hardware FM classes if present
-keep class qcom.fmradio.** { *; }
-keep class com.caf.fmradio.** { *; }
-keep class android.hardware.fmradio.** { *; }

# Keep system property reflection methods and ServiceManager
-keep class android.os.SystemProperties {
    public static java.lang.String get(java.lang.String);
    public static java.lang.String get(java.lang.String, java.lang.String);
}
-keep class android.os.ServiceManager {
    public static android.os.IBinder getService(java.lang.String);
}
-keep class android.os.HwBinder {
    public static android.os.IHwBinder getService(java.lang.String, java.lang.String);
}

# Preserve Line Numbers for Exception reporting
-keepattributes SourceFile,LineNumberTable
