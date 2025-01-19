package com.example.customkeyboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

class ScannerActivity : ComponentActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private var showPermissionDialog by mutableStateOf(false)
    private var permissionDeniedCount = 0 // Track the number of permission denial attempts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScannerScreen()
        }
    }

    @Composable
    fun ScannerScreen() {
        // Check if camera permission is granted
        val cameraPermissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )

        if (cameraPermissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Start the scanner if permission is granted
            startScanner()
        } else {
            // Show permission dialog or request permission if denied
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show an explanation dialog if needed
                showPermissionDialog = true
            } else {
                // Request permission directly
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Permission Needed") },
                text = { Text("Camera permission is required to scan barcodes.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPermissionDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                    ) {
                        Text("Go to Settings")
                    }
                }
            )
        }
    }

    private fun startScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Volume Up: Flash On, Volume Down: Flash Off.")
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.setOrientationLocked(false) // Allow scanning in any orientation
        integrator.setCaptureActivity(CustomCaptureActivity::class.java) // Use custom activity for portrait mode
        integrator.initiateScan()
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, // Fix type to Array<String>
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the scanner
                startScanner()
            } else {
                // Permission denied
                permissionDeniedCount++ // Increment the denial count
                if (permissionDeniedCount < 3) {
                    // Retry permission request if less than 3 times
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Show the final permission dialog after 3 denials
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        showPermissionDialog = true
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val scannedContent = result.contents

            // Check if the scanned content is a valid URL
            if (scannedContent.startsWith("http://") || scannedContent.startsWith("https://")) {
                // It's a link, open it in the browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedContent))
                startActivity(intent)
            } else {
                // If it's not a URL, proceed with the default behavior (send broadcast, etc.)
                vibrateDevice()
                val intent = Intent().apply {
                    action = "com.example.customkeyboard.SCANNED_CODE"
                    putExtra("SCANNED_CODE", scannedContent)
                }
                sendBroadcast(intent)
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    private fun vibrateDevice() {
        val vibrator = getSystemService(Vibrator::class.java)
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)) // Vibrate for 500ms
            } else {
                vibrator.vibrate(500) // For older versions of Android
            }
        }
    }
}

class CustomCaptureActivity : CaptureActivity()
