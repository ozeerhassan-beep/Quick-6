package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AppFingerprintUtils {

    const val KNOWN_APP_ID = "com.kwickartfree.mu"
    const val KNOWN_SHA1 = "88:55:87:BC:14:CB:4D:C6:12:50:B4:76:54:4C:BF:43:5D:14:D6:31"
    const val KNOWN_SHA256 = "D2:ED:60:85:D6:2C:EB:46:81:1D:AC:72:AF:7D:73:F8:39:A6:E3:16:D1:25:04:30:5D:04:C6:F2:25:04:76:76"

    fun getApplicationId(context: Context): String {
        val pkg = context.packageName
        return if (!pkg.isNullOrBlank()) pkg else KNOWN_APP_ID
    }

    fun getSha1Fingerprint(context: Context): String {
        val computed = getCertificateFingerprint(context, "SHA-1")
        return if (!computed.isNullOrBlank() && computed != "Indisponible") computed else KNOWN_SHA1
    }

    fun getSha256Fingerprint(context: Context): String {
        val computed = getCertificateFingerprint(context, "SHA-256")
        return if (!computed.isNullOrBlank() && computed != "Indisponible") computed else KNOWN_SHA256
    }

    private fun getCertificateFingerprint(context: Context, algorithm: String): String {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val packageInfo = pm.getPackageInfo(context.packageName, flags)
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
                    ?: packageInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val cert = signatures?.firstOrNull()?.toByteArray() ?: return "Indisponible"
            val md = MessageDigest.getInstance(algorithm)
            val digest = md.digest(cert)
            digest.joinToString(":") { String.format("%02X", it) }
        } catch (e: Exception) {
            "Indisponible"
        }
    }
}
