package com.example.customkeyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.delay
import java.nio.ByteBuffer

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
        var flashlightOn by remember { mutableStateOf(false) }
        var isQRCodeMode by remember { mutableStateOf(true) } // Toggle for QR/Barcode mode

        LaunchedEffect(cameraPermissionState.status.isGranted) {
            if (cameraPermissionState.status.isGranted) {
                isCameraActive = true
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isCameraActive) {
                CameraScanner(
                    onBarcodeScanned = { barcodeValue ->
                        if (Patterns.WEB_URL.matcher(barcodeValue).matches()) {
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
                            painter = painterResource(id = if (isQRCodeMode) R.drawable.qr_icon else R.drawable.barcode_icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .clickable {
                                    isQRCodeMode = !isQRCodeMode
                                    if (isQRCodeMode) {
                                        cameraWidth = 250.dp
                                        cameraHeight = 250.dp
                                    } else {
                                        cameraWidth = 330.dp
                                        cameraHeight = 80.dp
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
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        var delayCompleted by remember { mutableStateOf(false) }
        var showCenterLine by remember { mutableStateOf(false) }

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
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = androidx.camera.view.PreviewView(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setTargetResolution(
                                        android.util.Size(
                                            cameraWidth.value.toInt(),
                                            cameraHeight.value.toInt()
                                        )
                                    )
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(
                                            ContextCompat.getMainExecutor(ctx),
                                            BarcodeAnalyzer(
                                                onBarcodeScanned = {
                                                    onBarcodeScanned(it)  // Trigger the callback
                                                    showCenterLine = true  // Show the center line when barcode is scanned

                                                    // Trigger vibration on barcode detection
                                                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                                    } else {
                                                        vibrator.vibrate(100) // For devices below Android O
                                                    }
                                                }
                                            )
                                        )
                                    }

                                val meterFactory = previewView.meteringPointFactory
                                val centerX = previewView.width / 2f
                                val centerY = previewView.height / 2f
                                val centerMeteringPoint = meterFactory.createPoint(centerX, centerY)

                                val cameraControl = cameraProvider.bindToLifecycle(
                                    ctx as ComponentActivity,
                                    cameraSelector,
                                    preview,
                                    imageAnalyzer
                                ).cameraControl

                                val action = FocusMeteringAction.Builder(centerMeteringPoint)
                                    .build()
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

                        if (showCenterLine) {
                            val centerLineY = (size.height / 2)
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

    class BarcodeAnalyzer(private val onBarcodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {

        private val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.QR_CODE,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.UPC_A,
                    ),
                    DecodeHintType.TRY_HARDER to true  // Increases sensitivity for smaller barcodes
                )
            )
        }

        override fun analyze(image: ImageProxy) {
            val buffer: ByteBuffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val centerRegionWidth = image.width / 2
            val centerRegionHeight = image.height / 2
            val startX = image.width / 4
            val startY = image.height / 4

            // Crop to the center region (reduced size)
            val croppedData = cropImage(data, image.width, image.height, startX, startY, centerRegionWidth, centerRegionHeight)

            // Optionally, rotate the data if needed (ensure it's correctly adjusted for barcode orientation)
            val rotatedData = rotateImageData(croppedData, centerRegionWidth, centerRegionHeight)

            // Create a luminance source for the center region only
            val source = PlanarYUVLuminanceSource(
                rotatedData,
                centerRegionWidth,
                centerRegionHeight,
                0,
                0,
                centerRegionWidth,
                centerRegionHeight,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decodeWithState(binaryBitmap)
                onBarcodeScanned(result.text)
            } catch (e: NotFoundException) {
                // If no barcode is found, do nothing or log the failure
            } finally {
                image.close()
            }
        }

        private fun cropImage(data: ByteArray, width: Int, height: Int, startX: Int, startY: Int, cropWidth: Int, cropHeight: Int): ByteArray {
            val croppedData = ByteArray(cropWidth * cropHeight)

            for (y in 0 until cropHeight) {
                for (x in 0 until cropWidth) {
                    val oldIndex = (startY + y) * width + (startX + x)
                    val newIndex = y * cropWidth + x
                    croppedData[newIndex] = data[oldIndex]
                }
            }

            return croppedData
        }

        private fun rotateImageData(data: ByteArray, width: Int, height: Int): ByteArray {
            val rotatedData = ByteArray(data.size)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val newX = height - y - 1
                    val newY = x
                    val oldIndex = y * width + x
                    val newIndex = newY * height + newX
                    rotatedData[newIndex] = data[oldIndex]
                }
            }

            return rotatedData
        }
    }

   @Preview(showBackground = true)
    @Composable
    fun ScannerScreenPreview() {
        ScannerScreen()
    }
}
