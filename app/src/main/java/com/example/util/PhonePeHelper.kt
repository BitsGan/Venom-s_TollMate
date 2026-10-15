package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.util.Locale

sealed class PhonePeLaunchResult {
    data class LaunchedDirectly(val vpa: String, val amount: Double) : PhonePeLaunchResult()
    data class LaunchedGenericUpi(val vpa: String, val amount: Double) : PhonePeLaunchResult()
    data class NoUpiAppInstalled(val vpa: String, val amount: Double) : PhonePeLaunchResult()
    data class Error(val message: String) : PhonePeLaunchResult()
}

object PhonePeHelper {
    private const val TAG = "PhonePeHelper"
    const val PHONEPE_PACKAGE = "com.phonepe.app"

    /**
     * Normalizes a mobile number or UPI ID into a valid PhonePe VPA.
     * In India, PhonePe mobile numbers map to <mobile>@ybl by default.
     */
    fun formatPhonePeVpa(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("@")) {
            return trimmed
        }
        val digits = trimmed.filter { it.isDigit() }
        val mobile10 = if (digits.length >= 10) digits.takeLast(10) else digits
        return if (mobile10.isNotEmpty()) "$mobile10@ybl" else "tollmate.fastag@ybl"
    }

    /**
     * Extracts a clean 10-digit mobile number from input if possible.
     */
    fun extractMobileNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    /**
     * Builds a standard NPCI UPI payment URI.
     */
    fun buildUpiUri(
        vpa: String,
        payeeName: String,
        amount: Double,
        note: String,
        txnRef: String
    ): Uri {
        val cleanVpa = formatPhonePeVpa(vpa)
        val cleanName = payeeName.ifBlank { "TollMate Highway Traveler" }
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val cleanNote = note.ifBlank { "TollMate Highway Toll Payment" }

        val uriString = StringBuilder("upi://pay?")
            .append("pa=").append(Uri.encode(cleanVpa))
            .append("&pn=").append(Uri.encode(cleanName))
            .append("&am=").append(Uri.encode(formattedAmount))
            .append("&cu=INR")
            .append("&tn=").append(Uri.encode(cleanNote))
            .append("&tr=").append(Uri.encode(txnRef))
            .toString()

        return Uri.parse(uriString)
    }

    /**
     * Attempts to launch PhonePe directly. If PhonePe is not installed,
     * falls back to any available UPI payment app (GPay, Paytm, BHIM, etc.).
     */
    fun launchPayment(
        context: Context,
        vpaOrMobile: String,
        payeeName: String,
        amount: Double,
        note: String,
        txnRef: String
    ): PhonePeLaunchResult {
        val vpa = formatPhonePeVpa(vpaOrMobile)
        val upiUri = buildUpiUri(vpa, payeeName, amount, note, txnRef)

        // 1. Try launching PhonePe specifically
        try {
            val phonePeIntent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                setPackage(PHONEPE_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(phonePeIntent)
            Log.d(TAG, "Successfully launched PhonePe app for $vpa (₹$amount)")
            return PhonePeLaunchResult.LaunchedDirectly(vpa, amount)
        } catch (e: Exception) {
            Log.w(TAG, "PhonePe app direct launch failed or not installed: ${e.message}")
        }

        // 2. Try generic UPI chooser
        try {
            val genericIntent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(genericIntent, "Complete Payment with PhonePe or UPI").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Log.d(TAG, "Launched generic UPI chooser for $vpa (₹$amount)")
            return PhonePeLaunchResult.LaunchedGenericUpi(vpa, amount)
        } catch (e: Exception) {
            Log.w(TAG, "Generic UPI intent failed: ${e.message}")
            return PhonePeLaunchResult.NoUpiAppInstalled(vpa, amount)
        }
    }

    /**
     * Opens PhonePe on the Google Play Store so user can install or update it.
     */
    fun openPhonePeOnPlayStore(context: Context) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PHONEPE_PACKAGE")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$PHONEPE_PACKAGE")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
