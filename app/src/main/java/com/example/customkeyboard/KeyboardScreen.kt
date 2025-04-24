package com.example.customkeyboard

import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KeyboardScreen(
    onSwitchKeyboard: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val alphabeticKeysMatrix = arrayOf(
        arrayOf("DEL", "", "BS", "7", "8", "9"),
        arrayOf("B", "H", "M", "4", "5", "6"),
        arrayOf("P", "R", "S", "1", "2", "3"),
        arrayOf("-", "", "", "0", "Enter")
    )
    val context = LocalContext.current
    val currentKeyPressed = remember { mutableStateOf("") }
    val showScannerScreen = remember { mutableStateOf(false) } // Flag to show ScannerScreen

    if (showScannerScreen.value) {
        ScannerScreen(
            onClose = { showScannerScreen.value = false }
        )
    } else {
        Column(
            modifier = Modifier
                .background(Color(0xFFA2ABBA))
                .fillMaxWidth(),
        ) {
            // Add a row for the top-left and top-right corner icons
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        onSwitchKeyboard()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.changekeyboard),
                        contentDescription = "Keyboard Icon",
                        modifier = Modifier.size(25.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onOpenScanner()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.barcodescan),
                        contentDescription = "QR Code Scanner Icon",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            // The rest of the keyboard layout
            alphabeticKeysMatrix.forEachIndexed { rowIndex, row ->
                FixedHeightBox(modifier = Modifier.fillMaxWidth(), height = 56.dp) {
                    Row(Modifier) {
                        row.forEachIndexed { index, key ->
                            if (key.isNotEmpty()) {
                                when (key) {
                                    "DEL" -> {
                                        RemoveAll(modifier = Modifier.weight(1f))
                                    }
                                    "BS" -> {
                                        RemoveKey(modifier = Modifier.weight(1f))
                                    }
                                    "B", "H", "M", "P", "R", "S" -> {
                                        KeyboardKey(
                                            keyboardKey = key,
                                            modifier = Modifier.weight(1f),
                                            backgroundColor = Color(0xFFCCE7EB),
                                            onClick = {
                                                currentKeyPressed.value = key
                                            }
                                        )
                                    }
                                    " " -> {
                                        KeyboardKey(
                                            keyboardKey = key,
                                            modifier = Modifier.weight(3.5f),
                                            onClick = {
                                                currentKeyPressed.value = key
                                            }
                                        )
                                    }
                                    "Enter" -> {
                                        EnterKey(modifier = Modifier.weight(2f))
                                    }
                                    else -> {
                                        KeyboardKey(
                                            keyboardKey = key,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                currentKeyPressed.value = key
                                            }
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .alpha(0f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemoveAll(modifier: Modifier) {
    val ctx = LocalContext.current

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            "DEL",
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    inputConnection.deleteSurroundingText(
                        10000,
                        0
                    ) // A large number to ensure deletion of all text
                }
                .background(Color(0xFFFBECDD))
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 18.dp,
                    bottom = 17.dp
                )
        )
    }
}
@Composable
fun KeyboardKey(
    keyboardKey: String,
    modifier: Modifier,
    backgroundColor: Color = Color(0xFFD8E2E3), // Default color
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState()
    val ctx = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            keyboardKey,
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp)) // Apply rounded corners
                .clickable(interactionSource = interactionSource, indication = null) {
                    onClick?.invoke() // If click action exists, invoke it
                    if (keyboardKey != "Enter") {
                        // Handle typing characters
                        (ctx as IMEService).currentInputConnection.commitText(keyboardKey, 1)
                    }
                }
                .background(backgroundColor)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 19.dp,
                    bottom = 18.dp
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // Add ellipsis if text overflows
        )
        if (pressed.value) {
            Text(
                keyboardKey,
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp)) // Apply rounded corners
                    .background(backgroundColor)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 15.dp,
                        bottom = 28.dp
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Add ellipsis if text overflows
            )
        }
    }
}
@Composable
fun RemoveKey(modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val ctx = LocalContext.current
    val isPressed = interactionSource.collectIsPressedAsState()

    // Coroutine scope for handling long-press behavior
    val scope = rememberCoroutineScope()
    var isLongPressing by remember { mutableStateOf(false) }

    // Launch effect to handle long-press deletion
    LaunchedEffect(isPressed.value) {
        if (isPressed.value) {
            isLongPressing = true
            scope.launch {
                delay(900) // Extended long-press threshold (800ms)
                while (isLongPressing) {
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    inputConnection.deleteSurroundingText(1, 0) // Delete one character
                    delay(150) // Repeat delete every 100ms while pressed
                }
            }
        } else {
            isLongPressing = false // Stop long-press when released
        }
    }

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            "BS", // Symbol for removing text
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp)) // Apply rounded corners
                .clickable(interactionSource = interactionSource, indication = null) {
                    val inputConnection = (ctx as IMEService).currentInputConnection

                    // Check if there is any selected text and delete it
                    if (inputConnection.getSelectedText(0) != null && inputConnection.getSelectedText(
                            0
                        ) != ""
                    ) {
                        inputConnection.commitText("", 1) // Clear selected text
                    } else {
                        inputConnection.deleteSurroundingText(
                            1,
                            0
                        ) // Remove one character around the cursor
                    }
                }
                .background(
                    if (isPressed.value) Color(0xFFF0C243).copy(alpha = 0.5f) else Color(0xFFF0C243) // Updated with opacity adjustment
                )

                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 19.dp,
                    bottom = 18.dp
                )
        )
    }
}
@Composable
fun EnterKey(modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val ctx = LocalContext.current
    val isPressed = interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box( // Outer box for text alignment
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp)) // Apply rounded corners
                .clickable(interactionSource = interactionSource, indication = null) {
                    // Commit an Enter action when the Enter key is pressed
                    (ctx as IMEService).currentInputConnection.commitText("\n", 1)

                    // Simulate an enter key event (KEYCODE_ENTER)
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    inputConnection.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_ENTER
                        )
                    )
                    inputConnection.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_ENTER
                        )
                    )
                }
                .background(
                    if (isPressed.value) Color(0xFF99E6C8) else Color(0xFFC0FAE6) // Feedback color
                ),
            contentAlignment = Alignment.Center // Center content inside the box
        ) {
            Text(
                "ENTER", // Enter key symbol
                modifier = Modifier.padding(
                    start = 10.dp,
                    end = 12.dp,
                    top = 19.dp,
                    bottom = 18.dp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Add ellipsis if text overflows
            )
        }
    }
}

@Composable
fun FixedHeightBox(modifier: Modifier, height: Dp, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        val h = height.roundToPx()
        layout(constraints.maxWidth, h) {
            placeables.forEach { placeable ->
                placeable.place(x = 0, y = kotlin.math.min(0, h - placeable.height))
            }
        }
    }
}
