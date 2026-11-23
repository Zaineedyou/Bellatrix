package com.bellatrixos.adb;

/** Runs in the Shizuku/Sui user service process with shell or root privilege. */
interface IAdbShellService {
    /** Reserved transaction used by Shizuku to stop the user service. */
    void destroy() = 16777114;

    /** Installs the APK Bellatrix downloaded. Returns 0 on success. */
    int installApk(String apkPath);
}
