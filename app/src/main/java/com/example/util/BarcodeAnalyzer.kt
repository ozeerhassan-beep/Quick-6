package com.example.util

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_ITF
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastScannedTimestamp = 0L
    private var lastScannedCode = ""

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue ?: barcode.displayValue
                        if (!rawValue.isNullOrBlank()) {
                            val now = System.currentTimeMillis()
                            // Debounce scans (same code 2.5s, new code 1s)
                            if (rawValue != lastScannedCode || (now - lastScannedTimestamp > 2500)) {
                                lastScannedTimestamp = now
                                lastScannedCode = rawValue
                                Log.d("BarcodeAnalyzer", "Detected Barcode: $rawValue, format: ${barcode.format}")
                                onBarcodeDetected(rawValue)
                            }
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("BarcodeAnalyzer", "Barcode recognition error: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
