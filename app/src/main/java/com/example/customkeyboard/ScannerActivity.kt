package com.example.customkeyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
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
                    }
                )
            }

            // Show the scanned barcode below the camera
            scannedBarcode?.let {
                Text(
                    text = it,
                    color = Color.Black,
                    style = TextStyle(fontSize = 24.sp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }

            // Optional UI elements (e.g., instructions, icons)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(26.dp)
            ) {
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
            }
        }
    }

    @Composable
    fun CameraScanner(onBarcodeScanned: (String) -> Unit) {
        val context = LocalContext.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
        val executor = Executors.newSingleThreadExecutor()

        var lastScannedTimestamp by remember { mutableStateOf(0L) } // Track last scan time

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp) // Size of the scanning area box
                    .clip(RoundedCornerShape(16.dp)) // Clip the camera preview to fit inside the box
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
                                    lastScannedTimestamp
                                ) { newTimestamp ->
                                    lastScannedTimestamp = newTimestamp // Update the last scanned timestamp
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

    fun processImageProxy(
        imageProxy: ImageProxy,
        barcodeScanner: BarcodeScanner,
        onBarcodeScanned: (String) -> Unit,
        lastScannedTimestamp: Long,
        onTimestampUpdated: (Long) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()

        // Check if the time difference is more than 2 seconds (2000 milliseconds)
        if (currentTime - lastScannedTimestamp > 2000) {
            val image = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            // Process the barcode and update the scanned value
                            onBarcodeScanned(rawValue)

                            // Update the last scanned timestamp to current time
                            onTimestampUpdated(currentTime)

                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close() // Skip processing if scanned too soon
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun ScannerScreenPreview() {
        ScannerScreen()
    }
}
