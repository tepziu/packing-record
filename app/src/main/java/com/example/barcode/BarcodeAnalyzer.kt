package com.example.barcode

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

data class ScannedBarcodeResult(
    val rawValue: String,
    val format: Int,
    val boundingBox: Rect?,
    val timestamp: Long = System.currentTimeMillis()
)

class BarcodeAnalyzer(
    private val onBarcodeDetected: (ScannedBarcodeResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastAnalyzedTimestamp = 0L
    private val throttleIntervalMs = 250L // 4 scans per second to keep CPU light and smooth

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < throttleIntervalMs) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    lastAnalyzedTimestamp = currentTimestamp
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue?.trim()
                        if (!rawValue.isNullOrBlank()) {
                            onBarcodeDetected(
                                ScannedBarcodeResult(
                                    rawValue = rawValue,
                                    format = barcode.format,
                                    boundingBox = barcode.boundingBox,
                                    timestamp = currentTimestamp
                                )
                            )
                            break // Process the primary detected code
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore transient frame analysis errors
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
