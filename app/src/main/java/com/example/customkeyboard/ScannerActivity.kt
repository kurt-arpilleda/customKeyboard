package com.example.customkeyboard

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.delay
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
        var isCameraActive by remember { mutableStateOf(false) }
        var cameraWidth by remember { mutableStateOf(330.dp) }
        var cameraHeight by remember { mutableStateOf(80.dp) }

        val context = LocalContext.current
        var flashlightOn by remember { mutableStateOf(false) } // State for flashlight

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
                        // Check if scanned code is a valid URL
                        if (Patterns.WEB_URL.matcher(barcodeValue).matches()) {
                            // If it is a URL, open it in the browser
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(barcodeValue))
                            context.startActivity(intent)
                        } else {
                            // If it's not a URL, send the scanned result via a broadcast
                            val intent = Intent().apply {
                                action = "com.example.customkeyboard.SCANNED_CODE"
                                putExtra("SCANNED_CODE", barcodeValue)
                            }
                            context.sendBroadcast(intent)
                            finish()
                        }
                    },
                    cameraWidth = cameraWidth,
                    cameraHeight = cameraHeight,
                    flashlightOn = flashlightOn // Pass the flashlight state to CameraScanner
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
                        tint = Color.White
                    )
                    Text(
                        text = "Scan Here",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }

                Text(
                    text = "Place the code inside the frame.\nEnsure it is centered and not blurry.",
                    color = Color.White,
                    fontSize = 12.sp
                )

                // Spacer to push the icons down to the bottom
                Spacer(modifier = Modifier.weight(1f))

                // Row for the QR, Barcode, and Flashlight icons at the bottom with border
                Row(
                    modifier = Modifier
                        .fillMaxWidth() // Ensures the icons are aligned to the bottom of the screen
                        .padding(bottom = 16.dp), // Bottom padding for icons
                    horizontalArrangement = Arrangement.Center, // Center the icons
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.qr_icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .clickable {
                                    // When QR icon is clicked, change camera size
                                    cameraWidth = 250.dp
                                    cameraHeight = 250.dp
                                }
                                .size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp)) // Spacer between the icons
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.barcode_icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .clickable {
                                    // When Barcode icon is clicked, reset camera size
                                    cameraWidth = 330.dp
                                    cameraHeight = 80.dp
                                }
                                .size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp)) // Spacer between the icons
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.flash_icon),
                            contentDescription = null,
                            tint = if (flashlightOn) Color.Yellow else Color.White, // Change tint based on flashlight status
                            modifier = Modifier
                                .clickable {
                                    flashlightOn = !flashlightOn // Toggle flashlight on/off
                                }
                                .size(40.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CameraScanner(
        onBarcodeScanned: (String) -> Unit,
        cameraWidth: Dp,
        cameraHeight: Dp,
        flashlightOn: Boolean // Add flashlight state parameter
    ) {
        val context = LocalContext.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
        val executor = Executors.newCachedThreadPool() // Use cached thread pool for better performance.

        var lastImageHash by remember { mutableStateOf(0) }
        var boundingBox by remember { mutableStateOf<Rect?>(null) }
        var isQRCode by remember { mutableStateOf(false) }
        var delayCompleted by remember { mutableStateOf(false) }
        var showCenterLine by remember { mutableStateOf(false) } // State to control the center line visibility

        LaunchedEffect(Unit) {
            delay(500)
            delayCompleted = true
        }

        // Set flashlight on/off based on state
        LaunchedEffect(flashlightOn) {
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val cameraControl = cameraProvider.bindToLifecycle(
                context as ComponentActivity,
                cameraSelector
            ).cameraControl

            if (flashlightOn) {
                cameraControl.enableTorch(true)
            } else {
                cameraControl.enableTorch(false)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (delayCompleted) {
                Box(
                    modifier = Modifier.fillMaxSize() // Ensure the camera fills the screen
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = androidx.camera.view.PreviewView(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setTargetResolution(android.util.Size(1920, 1080))
                                    .build()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    processImageProxy(
                                        imageProxy,
                                        barcodeScanner,
                                        onBarcodeScanned,
                                        lastImageHash
                                    ) { newImageHash, detectedBoundingBox, detectedIsQRCode ->
                                        lastImageHash = newImageHash
                                        boundingBox = detectedBoundingBox
                                        isQRCode = detectedIsQRCode

                                        // If a barcode is detected, show the center line
                                        showCenterLine = true
                                    }
                                }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                val meteringPointFactory = previewView.meteringPointFactory
                                val center = meteringPointFactory.createPoint(0.5f, 0.5f)
                                val action = FocusMeteringAction.Builder(center).build()

                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    ctx as ComponentActivity,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )

                                val cameraControl = cameraProvider.bindToLifecycle(
                                    ctx,
                                    cameraSelector,
                                    preview
                                ).cameraControl
                                cameraControl.startFocusAndMetering(action)

                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val dimColor = Color.Black.copy(alpha = 0.7f)
                        drawRect(color = dimColor)

                        val frameWidth = cameraWidth.toPx()
                        val frameHeight = cameraHeight.toPx()
                        val centerX = (size.width - frameWidth) / 2
                        val centerY = (size.height - frameHeight) / 2

                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(centerX, centerY),
                            size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
                            blendMode = BlendMode.Clear
                        )

                        // Draw the horizontal line only when a barcode is detected
                        if (showCenterLine) {
                            val centerLineY = (size.height / 2) // Middle of the screen vertically
                            drawLine(
                                color = Color.Green,
                                start = Offset(centerX, centerLineY),
                                end = Offset(centerX + frameWidth, centerLineY),
                                strokeWidth = 6f
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Preparing Scanner...",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }

    private fun processImageProxy(
        imageProxy: ImageProxy,
        barcodeScanner: BarcodeScanner,
        onBarcodeScanned: (String) -> Unit,
        lastImageHash: Int,
        onStateUpdated: (Int, Rect?, Boolean) -> Unit
    ) {
        val image = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val currentImageHash = calculateImageHash(imageProxy)

        if (currentImageHash != lastImageHash) {
            // Check image sharpness before processing
            if (isImageFocused(imageProxy)) {
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        var barcodeDetected = false
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            val boundingBox = barcode.boundingBox

                            if (rawValue != null && boundingBox != null) {
                                // Get the camera feed resolution
                                val imageWidth = imageProxy.width
                                val imageHeight = imageProxy.height

                                // Set a threshold as a percentage of the resolution
                                val minWidth = imageWidth * 0.25f
                                val minHeight = imageHeight * 0.1f

                                // Check if the bounding box is sufficiently large based on resolution
                                if (boundingBox.width() > minWidth && boundingBox.height() > minHeight) {
                                    val isQRCode = barcode.format == Barcode.FORMAT_QR_CODE

                                    val centerMargin = 0.5
                                    val centerRect = Rect(
                                        (imageWidth * centerMargin).toInt(),
                                        (imageHeight * centerMargin).toInt(),
                                        (imageWidth * (1 - centerMargin)).toInt(),
                                        (imageHeight * (1 - centerMargin)).toInt()
                                    )

                                    // Check if the bounding box is within the center region of the image
                                    if (isBoundingBoxInCenterRegion(boundingBox, centerRect)) {
                                        // Only trigger the scan if the frame is good
                                        onBarcodeScanned(rawValue)
                                        onStateUpdated(currentImageHash, boundingBox, isQRCode)
                                        barcodeDetected = true
                                        break
                                    }
                                }
                            }
                        }

                        // If no barcode is detected, do not call the onBarcodeScanned callback
                        if (!barcodeDetected) {
//                            Log.d("ScannerActivity", "Barcode not detected or not in center region.")
                        }
                    }
                    .addOnFailureListener {
//                        Log.e("ScannerActivity", "Barcode scan failed: ${it.message}")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
//                Log.d("ScannerActivity", "Image is not focused enough, skipping scan.")
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }


    private fun calculateImageHash(imageProxy: ImageProxy): Int {
        val image = imageProxy.image
        val buffer = image?.planes?.get(0)?.buffer
        val byteArray = ByteArray(buffer?.remaining() ?: 0)
        buffer?.get(byteArray)

        // Return a hash of the image content for determining stability
        return byteArray.contentHashCode()
    }
    // Function to check if the bounding box is inside the center region
    private fun isBoundingBoxInCenterRegion(boundingBox: Rect, centerRegion: Rect): Boolean {
        return boundingBox.intersect(centerRegion)
    }

    private fun isImageFocused(imageProxy: ImageProxy): Boolean {
        val image = imageProxy.image
        val width = image?.width ?: return false
        val height = image?.height ?: return false

        // Use 30% of image width and 10% of image height as thresholds
        val minWidth = width * 0.25
        val minHeight = height * 0.1

        return width > minWidth && height > minHeight
    }

    @Preview(showBackground = true)
    @Composable
    fun ScannerScreenPreview() {
        ScannerScreen()
    }
}


