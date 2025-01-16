package com.example.customkeyboard

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity

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
        var showPermissionDialog by remember { mutableStateOf(false) }

        // Handle camera permission state
        LaunchedEffect(cameraPermissionState.status) {
            when (cameraPermissionState.status) {
                is PermissionStatus.Granted -> {
                    // Start the scanner directly when permission is granted
                    startScanner()
                }
                is PermissionStatus.Denied -> {
                    if (!(cameraPermissionState.status as PermissionStatus.Denied).shouldShowRationale) {
                        showPermissionDialog = true
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { Text("Camera Permission Required") },
                    text = { Text("Please enable camera permissions from the settings to use the scanner.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }) {
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

    private fun startScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scan a code")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(false) // Allow scanning in any orientation
        integrator.setCaptureActivity(CustomCaptureActivity::class.java) // Use custom activity for portrait mode
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val barcodeValue = result.contents
            val intent = Intent().apply {
                action = "com.example.customkeyboard.SCANNED_CODE"
                putExtra("SCANNED_CODE", barcodeValue)
            }
            sendBroadcast(intent)
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
class CustomCaptureActivity : CaptureActivity()
