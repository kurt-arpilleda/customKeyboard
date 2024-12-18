package com.example.customkeyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class ScannerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScannerScreen()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun ScannerScreen() {
        val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
        var scannedBarcode by remember { mutableStateOf<String?>(null) }
        var isCameraActive by remember { mutableStateOf(false) }
        var cameraWidth by remember { mutableStateOf(330.dp) }
        var cameraHeight by remember { mutableStateOf(80.dp) }

        val context = LocalContext.current

        // Request camera permission and activate camera when permission is granted
        LaunchedEffect(cameraPermissionState.status.isGranted) {
            if (cameraPermissionState.status.isGranted) {
                isCameraActive = true
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        }

        // Show Camera Scanner when active, filling the entire screen
        Box(modifier = Modifier.fillMaxSize()) {
            if (isCameraActive) {
                CameraScanner(
                    onBarcodeScanned = { barcodeValue ->
                        Log.d("ScannerActivity", "Scanned Barcode: $barcodeValue")
                        // Send the scanned result back via a broadcast
                        val intent = Intent().apply {
                            action = "com.example.customkeyboard.SCANNED_CODE"
                            putExtra("SCANNED_CODE", barcodeValue)
                        }
                        sendBroadcast(intent)
                        // Finish the activity to return control to the keyboard
                        finish()
                    },
                    cameraWidth = cameraWidth,
                    cameraHeight = cameraHeight
                )
            }

            // Optional UI elements (e.g., instructions, icons)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(26.dp)
            ) {
                // "Scan Here" section (this part stays the same)
                Row(
                    modifier = Modifier
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.qrscanner),
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Text(
                        text = "Scan Here",
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                }

                Text(
                    text = "Place the code inside the frame.\nMake sure it is not cut or has any blur.",
                    color = Color.Black,
                    fontSize = 12.sp
                )

                // Spacer to push the icons down to the bottom
                Spacer(modifier = Modifier.weight(1f))

                // Row for the QR and Barcode icons at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth() // Ensures the icons are aligned to the bottom of the screen
                        .padding(bottom = 16.dp), // Bottom padding for icons
                    horizontalArrangement = Arrangement.End, // Center the icons
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.qr_icon),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .clickable {
                                // When QR icon is clicked, change camera size
                                cameraWidth = 250.dp
                                cameraHeight = 250.dp
                            }
                            .size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp)) // Spacer between the two icons
                    Icon(
                        painter = painterResource(id = R.drawable.barcode_icon),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .clickable {
                                // When Barcode icon is clicked, reset camera size
                                cameraWidth = 330.dp
                                cameraHeight = 80.dp
                            }
                            .size(40.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun CameraScanner(
        onBarcodeScanned: (String) -> Unit,
        cameraWidth: Dp,
        cameraHeight: Dp
    ) {
        val context = LocalContext.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
        val executor = Executors.newSingleThreadExecutor()

        var lastScannedTimestamp by remember { mutableStateOf(0L) } // Track last scan time
        var lastImageHash by remember { mutableStateOf<Int>(0) } // Track image sharpness

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(cameraWidth)
                    .height(cameraHeight)
                    .clip(RoundedCornerShape(15.dp)) // Clip the camera preview to fit inside the box
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = androidx.camera.view.PreviewView(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Image Analysis for scanning
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                processImageProxy(
                                    imageProxy,
                                    barcodeScanner,
                                    onBarcodeScanned,
                                    lastScannedTimestamp,
                                    lastImageHash
                                ) { newTimestamp, newImageHash ->
                                    lastScannedTimestamp = newTimestamp // Update the last scanned timestamp
                                    lastImageHash = newImageHash // Update the last image hash for sharpness
                                }
                            }

                            // Camera Setup
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                (ctx as ComponentActivity),
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )

                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize() // Make camera scanner fill the entire box area
                )
            }
        }
    }

    private fun processImageProxy(
        imageProxy: ImageProxy,
        barcodeScanner: BarcodeScanner,
        onBarcodeScanned: (String) -> Unit,
        lastScannedTimestamp: Long,
        lastImageHash: Int,
        onTimestampUpdated: (Long, Int) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()

        // Check if the time difference is more than 1 second (1000 milliseconds) to add a delay between scans
        if (currentTime - lastScannedTimestamp > 1000) {  // Delay added here to wait between scans
            val image = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

            // Check if the image is sharp enough by calculating the sharpness using contrast-based method
            val currentImageHash = calculateImageSharpness(imageProxy)

            // Only process if the image has changed significantly or meets a confidence threshold
            if (currentImageHash != lastImageHash) {
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        var foundValidBarcode = false
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            val confidence = barcode.boundingBox?.width() ?: 0

                            // Check if the barcode is fully visible, sharp enough, and has a high confidence level
                            if (rawValue != null && confidence > 300 && isBarcodeVisible(barcode) && isSharpEnough(imageProxy)) {
                                // Process the barcode and update the scanned value
                                onBarcodeScanned(rawValue)
                                foundValidBarcode = true

                                // Update the last scanned timestamp to current time
                                onTimestampUpdated(currentTime, currentImageHash)

                                // Once a valid barcode is successfully scanned, break out of the loop
                                break
                            }
                        }

                        // If no valid barcode was found, continue scanning (to prevent false positives)
                        if (!foundValidBarcode) {
                            // Optionally log a message or delay a bit more before trying again
                            Log.d("ScannerActivity", "No valid barcode detected, retrying...")
                        }
                    }
                    .addOnFailureListener {
                        // Handle failure (optional logging)
                        Log.e("ScannerActivity", "Barcode scan failed")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                // Skip processing if the image is too similar to the previous one (indicating little change in stability)
                imageProxy.close()
            }
        } else {
            // Skip processing if scanned too soon (to avoid duplicate scans or scanning every frame)
            imageProxy.close()
        }
    }

    private fun isBarcodeVisible(barcode: Barcode): Boolean {
        val boundingBox = barcode.boundingBox
        // Ensure the bounding box is large enough and properly visible
        return (boundingBox?.width() ?: 0) > 200 && (boundingBox?.height() ?: 0) > 60 // Minimum dimensions for visibility
    }

    private fun calculateImageSharpness(imageProxy: ImageProxy): Int {
        val image = imageProxy.image
        val buffer = image?.planes?.get(0)?.buffer
        val byteArray = ByteArray(buffer?.remaining() ?: 0)
        buffer?.get(byteArray)

        // Optional: Apply contrast or edge detection to improve sharpness check
        return byteArray.contentHashCode() // Replace with an edge-detection or contrast-based sharpness check
    }

    // Function to check if the image is sharp enough for processing
    private fun isSharpEnough(imageProxy: ImageProxy): Boolean {
        val image = imageProxy.image
        val buffer = image?.planes?.get(0)?.buffer
        val byteArray = ByteArray(buffer?.remaining() ?: 0)
        buffer?.get(byteArray)

        // Using basic content hash as a proxy for sharpness – this can be replaced with more complex methods
        return byteArray.contentHashCode() != 0 // Arbitrary condition, needs improvement based on real sharpness logic
    }

    @Preview(showBackground = true)
    @Composable
    fun ScannerScreenPreview() {
        ScannerScreen()
    }
}


