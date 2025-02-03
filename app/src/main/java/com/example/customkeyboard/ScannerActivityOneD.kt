package com.example.customkeyboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class ScannerActivityOneD : ComponentActivity() {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, show the ScannerScreen
            setContent {
                ScannerScreen()
            }
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, show the ScannerScreen
                    setContent {
                        ScannerScreen()
                    }
                } else {
                    // Check if the user selected "Don't ask again"
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        // User denied permission permanently, direct them to app settings
                        Toast.makeText(this, "Camera permission is required. Please enable it in app settings.", Toast.LENGTH_LONG).show()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                        finish() // Optional: Close the current activity
                    } else {
                        // Permission denied but not permanently, show a message
                        Toast.makeText(this, "Please enable camera permission in the app settings.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    override fun onDestroy(){
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1
    }

    @Composable
    fun ScannerScreen() {
        var isCameraActive by remember { mutableStateOf(true) }
        var cameraWidth by remember { mutableStateOf(330.dp) }
        var cameraHeight by remember { mutableStateOf(80.dp) }
        val context = LocalContext.current
        var flashlightOn by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isCameraActive) {
                CameraScanner(
                    onBarcodeScanned = { barcodeValue ->
                        if (barcodeValue.startsWith("http://") || barcodeValue.startsWith("https://")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(barcodeValue))
                            context.startActivity(intent)
                        } else {
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
                    flashlightOn = flashlightOn
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(26.dp)
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.barcodescan),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "Scan 1D",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }

                Text(
                    text = "Place the 1D Barcode inside the frame.\nEnsure it is centered and not blurry.",
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
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
                                    val intent = Intent(context, ScannerActivityTwoD::class.java)
                                    if (context !is Activity) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    if (context is Activity) {
                                        context.overridePendingTransition(
                                            R.anim.animate_fade_enter,
                                            R.anim.animate_fade_exit
                                        )
                                    }
                                }
                                .size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.flash_icon),
                            contentDescription = null,
                            tint = if (flashlightOn) Color.Yellow else Color.White,
                            modifier = Modifier
                                .clickable {
                                    flashlightOn = !flashlightOn
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
        flashlightOn: Boolean
    ) {
        val context = LocalContext.current
        val density = LocalDensity.current.density
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        var delayCompleted by remember { mutableStateOf(false) }
        var showCenterLine by remember { mutableStateOf(false) }
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        // Convert DP dimensions to pixels once
        val frameWidthPx = remember(cameraWidth) { (cameraWidth.value * density).toInt() }
        val frameHeightPx = remember(cameraHeight) { (cameraHeight.value * density).toInt() }

        LaunchedEffect(Unit) {
            delay(500)
            delayCompleted = true
        }

        LaunchedEffect(flashlightOn) {
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val cameraControl = cameraProvider.bindToLifecycle(
                context as ComponentActivity,
                cameraSelector
            ).cameraControl
            cameraControl.enableTorch(flashlightOn)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (delayCompleted) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FIT_CENTER
                            }

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                // Wait for previewView layout
                                previewView.post {
                                    val imageAnalyzer = ImageAnalysis.Builder()
                                        .setTargetResolution(
                                            android.util.Size(
                                                cameraWidth.value.toInt(),
                                                cameraHeight.value.toInt()
                                            )
                                        )
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build().also { analyzer ->
                                            analyzer.setAnalyzer(cameraExecutor, BarcodeAnalyzer(
                                                onBarcodeScanned = { barcode ->
                                                    onBarcodeScanned(barcode)
                                                    showCenterLine = true
                                                    // Vibrate on scan
                                                    (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                                        } else {
                                                            @Suppress("DEPRECATION")
                                                            it.vibrate(100)
                                                        }
                                                    }
                                                },
                                                previewWidth = previewView.width,
                                                previewHeight = previewView.height,
                                                frameLeft = (previewView.width - frameWidthPx) / 2,
                                                frameTop = (previewView.height - frameHeightPx) / 2,
                                                frameWidth = frameWidthPx,
                                                frameHeight = frameHeightPx
                                            ))
                                        }

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        ctx as ComponentActivity,
                                        cameraSelector,
                                        preview,
                                        imageAnalyzer
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dimColor = Color.Black.copy(alpha = 0.8f)
                        drawRect(color = dimColor)

                        val frameWidth = cameraWidth.toPx()
                        val frameHeight = cameraHeight.toPx()
                        val centerX = (size.width - frameWidth) / 2
                        val centerY = (size.height - frameHeight) / 2

                        // Draw transparent scanning window
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(centerX, centerY),
                            size = Size(frameWidth, frameHeight),
                            blendMode = BlendMode.Clear
                        )

                        // Draw center line when scanned
                        if (showCenterLine) {
                            drawLine(
                                color = Color.Green,
                                start = Offset(centerX, centerY + frameHeight / 2),
                                end = Offset(centerX + frameWidth, centerY + frameHeight / 2),
                                strokeWidth = 4f
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Preparing 1D Scanner...",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }

    class BarcodeAnalyzer(
        private val onBarcodeScanned: (String) -> Unit,
        private val previewWidth: Int,
        private val previewHeight: Int,
        private val frameLeft: Int,
        private val frameTop: Int,
        private val frameWidth: Int,
        private val frameHeight: Int,
        private val debounceInterval: Long = 100,
        private val threshold: Int = 3
    ) : ImageAnalysis.Analyzer {

        private val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.CODE_93,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.ITF
                    ),
                    DecodeHintType.TRY_HARDER to true  // Increases sensitivity for smaller barcodes
                )
            )
        }
        private var lastScanTime: Long = 0
        private val lock = Any()
        private val scanQueue = mutableListOf<String>()

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            try {
                imageProxy.image?.let { image ->
                    if (image.format in listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)) {
                        val luminanceData = getLuminancePlaneData(imageProxy)
                        val rotatedImage = RotatedImage(luminanceData, image.width, image.height)
                        rotateImageArray(rotatedImage, imageProxy.imageInfo.rotationDegrees)

                        // Calculate crop region in rotated image coordinates
                        val scaleX = rotatedImage.width.toFloat() / previewWidth
                        val scaleY = rotatedImage.height.toFloat() / previewHeight

                        val cropLeft = (frameLeft * scaleX).toInt().coerceIn(0, rotatedImage.width)
                        val cropTop = (frameTop * scaleY).toInt().coerceIn(0, rotatedImage.height)
                        val cropWidth = (frameWidth * scaleX).toInt().coerceIn(1, rotatedImage.width - cropLeft)
                        val cropHeight = (frameHeight * scaleY).toInt().coerceIn(1, rotatedImage.height - cropTop)

                        val source = PlanarYUVLuminanceSource(
                            rotatedImage.byteArray,
                            rotatedImage.width,
                            rotatedImage.height,
                            cropLeft,
                            cropTop,
                            cropWidth,
                            cropHeight,
                            false
                        )

                        try {
                            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                            handleResult(result.text)
                        } catch (e: NotFoundException) {
                            // Barcode not found in cropped region
                        }
                    }
                }
            } finally {
                imageProxy.close()
            }
        }

        private fun handleResult(barcode: String) {
            synchronized(lock) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScanTime >= debounceInterval) {
                    scanQueue.add(barcode)
                    if (scanQueue.size > threshold) scanQueue.removeAt(0)

                    Log.d("BarcodeAnalyzer", "Scanned Barcode: $barcode, Queue: $scanQueue")

                    if (scanQueue.size == threshold && scanQueue.all { it == scanQueue[0] }) {
                        Log.d("BarcodeAnalyzer", "Final Scanned Barcode: ${scanQueue[0]}")
                        onBarcodeScanned(scanQueue[0])
                        scanQueue.clear()
                    }
                    lastScanTime = currentTime
                }
            }
        }


        private fun getLuminancePlaneData(image: ImageProxy): ByteArray {
            val plane = image.planes[0]
            val buf: ByteBuffer = plane.buffer
            val data = ByteArray(buf.remaining())
            buf.get(data)
            buf.rewind()
            val width = image.width
            val height = image.height
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            val cleanData = ByteArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    cleanData[y * width + x] = data[y * rowStride + x * pixelStride]
                }
            }
            return cleanData
        }

        private fun rotateImageArray(imageToRotate: RotatedImage, rotationDegrees: Int) {
            if (rotationDegrees == 0) return
            if (rotationDegrees % 90 != 0) return

            val width = imageToRotate.width
            val height = imageToRotate.height

            val rotatedData = ByteArray(imageToRotate.byteArray.size)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    when (rotationDegrees) {
                        90 -> rotatedData[x * height + height - y - 1] =
                            imageToRotate.byteArray[x + y * width]
                        180 -> rotatedData[width * (height - y - 1) + width - x - 1] =
                            imageToRotate.byteArray[x + y * width]
                        270 -> rotatedData[y + x * height] =
                            imageToRotate.byteArray[y * width + width - x - 1]
                    }
                }
            }

            imageToRotate.byteArray = rotatedData

            if (rotationDegrees != 180) {
                imageToRotate.height = width
                imageToRotate.width = height
            }
        }
    }

    private data class RotatedImage(var byteArray: ByteArray, var width: Int, var height: Int)

    @Preview(showBackground = true)
    @Composable
    fun ScannerScreenPreview() {
        ScannerScreen()
    }
}
