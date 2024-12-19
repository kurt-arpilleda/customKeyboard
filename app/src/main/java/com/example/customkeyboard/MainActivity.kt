package com.example.customkeyboard

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.customkeyboard.ui.theme.CustomKeyboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import splitties.systemservices.inputMethodManager

class MainActivity : AppCompatActivity() {
    private lateinit var appUpdateService: AppUpdateService
    private lateinit var connectivityReceiver: NetworkUtils.ConnectivityReceiver
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateService = AppUpdateService(this)
        checkForUpdates()

        connectivityReceiver = NetworkUtils.ConnectivityReceiver {
            checkForUpdates()
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, filter)

        setContent {
            CustomKeyboardTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Column(Modifier.fillMaxSize()) {
                        TopAppBarContent()
                        MainContent()
                        ShowImportantReminderDialog()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectivityReceiver)
    }

    private fun checkForUpdates() {
        coroutineScope.launch {
            if (NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                appUpdateService.checkForAppUpdate()
            }
        }
    }
}
@Composable
fun ShowImportantReminderDialog() {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        // Custom AlertDialog with a more modern design
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text(
                    text = "Important Reminder",
                    style = MaterialTheme.typography.h6, // Make the title stand out
                )
            },
            text = {
                Text(
                    text = "After selecting this keyboard as your default, clear this app in the background because after scanning, it will be back to this activity and not to the previous page.",
                    style = MaterialTheme.typography.body2, // Better text style
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f) // Subtle text color
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                    },
                    content = {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.button, // Standard button text style
                        )
                    }
                )
            },
            modifier = Modifier.padding(16.dp), // Add padding around the dialog content
            shape = MaterialTheme.shapes.medium, // Rounded corners
            backgroundColor = MaterialTheme.colors.surface, // Background color of the dialog
            contentColor = MaterialTheme.colors.onSurface // Text color inside the dialog
        )
    }
}

@Composable
fun TopAppBarContent() {
    TopAppBar(
        title = {
            Text(
                text = "Custom Keyboard",
                style = TextStyle(
                    fontSize = 24.sp
                )
            )
        },
        backgroundColor = Color.Transparent, // Removed the background
        elevation = 0.dp // Remove shadow/elevation as well
    )
}

@Composable
fun MainContent() {
    val ctx = LocalContext.current
    val (text, setValue) = remember { mutableStateOf(TextFieldValue()) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top) // Adjust spacing and alignment
    ) {
        Spacer(modifier = Modifier.height(80.dp)) // Add space to shift the content down slightly

        TextField(
            value = text,
            onValueChange = setValue,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Try here",
                    style = TextStyle(fontSize = 16.sp) // Set the same size as input text
                )
            },
            textStyle = TextStyle(fontSize = 16.sp), // Set input text size to 16.sp
            singleLine = true // Ensure the input does not expand vertically
        )

        Button(
            onClick = {
                ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text(text = "Enable Custom Keyboard")
        }

        Button(
            onClick = {
                inputMethodManager.showInputMethodPicker()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text(text = "Select Custom Keyboard")
        }
    }
}
