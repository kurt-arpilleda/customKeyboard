package com.example.customkeyboard

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
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
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.text.font.FontWeight
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
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text(
                    text = "Important Reminder",
                    style = MaterialTheme.typography.h6
                )
            },
            text = {
                Text(
                    text = "After selecting this keyboard as your default, clear this app in the background because after scanning, it will be back to this activity and not to the previous page.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.button
                    )
                }
            },
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.surface
        )
    }
}

@Composable
fun TopAppBarContent() {
    TopAppBar(
        title = {
            Text(
                text = "ARKeyboard Pro",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onPrimary
                )
            )
        },
        backgroundColor = MaterialTheme.colors.primary,
        elevation = 8.dp
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
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        OutlinedTextField(
            value = text,
            onValueChange = setValue,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(12.dp)), // Use shape for rounded corners
            placeholder = {
                Text(
                    text = "Type here...",
                    style = TextStyle(fontSize = 16.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                )
            },
            textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colors.onSurface),
            singleLine = true
        )


        Button(
            onClick = {
                ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Enable ARKeyboard Pro",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }

        Button(
            onClick = {
                inputMethodManager.showInputMethodPicker()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondaryVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Select ARKeyboard Pro",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }
    }
}

