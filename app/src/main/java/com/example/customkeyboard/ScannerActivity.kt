package com.example.customkeyboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
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

class ScannerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScannerScreen()
        }
    }
    @Composable
    fun ScannerScreen() {
        var isCameraActive by remember { mutableStateOf(false) }
        var cameraWidth by remember { mutableStateOf(330.dp) }
        var cameraHeight by remember { mutableStateOf(80.dp) }
        val context = LocalContext.current
        var flashlightOn by remember { mutableStateOf(false) }
        var isQrMode by remember { mutableStateOf(false) } // false = barcode, true = QR code
        var showPermissionDialog by remember { mutableStateOf(false) }

        // Check if the app has CAMERA permission
        val cameraPermissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )

        // Handle requesting camera permission if not granted
        if (cameraPermissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            LaunchedEffect(Unit) {
                // Request permission if not granted
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.CAMERA),
                    1001 // Permission request code
                )
            }
        } else {
            isCameraActive = true
        }

        // Check permission result in the parent activity (this should be handled in the Activity that contains this Composable)
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            isCameraActive = isGranted
            if (!isGranted) {
                showPermissionDialog = true
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
                            painter = painterResource(id = if (isQrMode) R.drawable.qr_icon else R.drawable.barcode_icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .clickable {
                                    isQrMode = !isQrMode
                                    if (isQrMode) {
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

            // Show dialog if permission is denied permanently
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("Camera Permission Required") },
                    text = { Text("Please enable camera permissions from the settings to use the scanner.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", context.packageName, null)
                                intent.data = uri
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Go to Settings", color = MaterialTheme.colors.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colors.primary)
                        }
                    }
                )
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
                        val dimColor = Color.Black.copy(alpha = 0.8f)
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
                        BarcodeFormat.DATA_MATRIX,
                        BarcodeFormat.CODE_93,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.UPC_A,
                    ),
                    DecodeHintType.TRY_HARDER to true  // Increases sensitivity for smaller barcodes
                )
            )
        }

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            try {
                imageProxy.image?.let {
                    if ((it.format == ImageFormat.YUV_420_888
                                || it.format == ImageFormat.YUV_422_888
                                || it.format == ImageFormat.YUV_444_888)
                        && it.planes.size == 3) {

                        val buffer = it.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        val rotatedImage = RotatedImage(bytes, imageProxy.width, imageProxy.height)
                        rotateImageArray(rotatedImage, imageProxy.imageInfo.rotationDegrees)

                        val source = PlanarYUVLuminanceSource(
                            rotatedImage.byteArray,
                            rotatedImage.width,
                            rotatedImage.height,
                            0,
                            0,
                            rotatedImage.width,
                            rotatedImage.height,
                            false
                        )

                        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                        try {
                            val result = reader.decodeWithState(binaryBitmap)
                            onBarcodeScanned(result.text)
                        } catch (e: NotFoundException) {
                            // No barcode found, do nothing or log the failure
                        } finally {
                            reader.reset()
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } finally {
                imageProxy.close()
            }
        }

        private fun rotateImageArray(imageToRotate: RotatedImage, rotationDegrees: Int) {
            if (rotationDegrees == 0) return // No rotation needed
            if (rotationDegrees % 90 != 0) return // Only handle rotations in 90-degree steps

            val width = imageToRotate.width
            val height = imageToRotate.height

            val rotatedData = ByteArray(imageToRotate.byteArray.size)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    when (rotationDegrees) {
                        90 -> rotatedData[x * height + height - y - 1] =
                            imageToRotate.byteArray[x + y * width] // Rotate 90 degrees clockwise
                        180 -> rotatedData[width * (height - y - 1) + width - x - 1] =
                            imageToRotate.byteArray[x + y * width] // Rotate 180 degrees clockwise
                        270 -> rotatedData[y + x * height] =
                            imageToRotate.byteArray[y * width + width - x - 1] // Rotate 270 degrees clockwise
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
