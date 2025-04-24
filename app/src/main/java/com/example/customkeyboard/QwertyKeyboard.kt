package com.example.customkeyboard

import android.app.Activity
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
fun QwertyKeyboard(
    onSwitchKeyboard: () -> Unit,
    onOpenScanner: () -> Unit  // Add this parameter
) {
    // Define QWERTY keyboard layout with 3 main rows for letters and bottom row for special keys
    val qwertyKeysMatrix = arrayOf(
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        arrayOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "BS"),
        arrayOf("123", ",", "SPACE", ".", "Enter")
    )

    val numericKeysMatrix = arrayOf(
        arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        arrayOf("@", "#", "$", "_", "&", "-", "+", "(", ")"),
        arrayOf("=\\<", "*", "\"", ";", "'", ":", "!", "?", "BS"),
        arrayOf("ABC", ",", "SPACE",".", "Enter")
    )

    val symbolKeysMatrix = arrayOf(
        arrayOf("~", "\\", "|", "^", "€", "£", "¥", "•", "×", "÷"),
        arrayOf("{", "}", "[", "]", "<", ">", "¿", "/", "%"),
        arrayOf("123", "€", "°", "©", "®", "¢", "±", "=", "BS"),
        arrayOf("ABC",",", "SPACE", ".", "Enter")
    )

    val context = LocalContext.current
    val currentKeyPressed = remember { mutableStateOf("") }
    val showScannerScreen = remember { mutableStateOf(false) }
    var isCapsLockEnabled by remember { mutableStateOf(false) }
    var lastShiftTapTime by remember { mutableStateOf(0L) }
    var currentKeyboardMode by remember { mutableStateOf(KeyboardMode.QWERTY) }
    var isShiftEnabled by remember { mutableStateOf(false) }

    if (showScannerScreen.value) {
        ScannerScreen(
            onClose = { showScannerScreen.value = false }
        )
    } else {
        Column(
            modifier = Modifier
                .background(Color(0xFFEBEFF2)) // Lighter background for modern look
                .fillMaxWidth(),
        ) {
            // Add a row for the top-left and top-right corner icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 1.dp),
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
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onOpenScanner()
                    }
                ){
                    Icon(
                        painter = painterResource(id = R.drawable.barcodescan),
                        contentDescription = "QR Code Scanner Icon",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Display the appropriate keyboard layout based on current mode
            val currentMatrix = when (currentKeyboardMode) {
                KeyboardMode.QWERTY -> qwertyKeysMatrix
                KeyboardMode.NUMERIC -> numericKeysMatrix
                KeyboardMode.SYMBOL -> symbolKeysMatrix
            }

            // Render keyboard rows
            currentMatrix.forEachIndexed { rowIndex, row ->
                val rowModifier = if (rowIndex == 0) {
                    Modifier.fillMaxWidth()
                } else if (rowIndex == 1) {
                    Modifier.fillMaxWidth().padding(horizontal = if (currentKeyboardMode == KeyboardMode.QWERTY) 10.dp else 5.dp)
                } else {
                    Modifier.fillMaxWidth()
                }

                FixedHeightBoxQwerty (modifier = rowModifier, height = 56.dp) {
                    Row(Modifier) {
                        when (rowIndex) {
                            3 -> { // Bottom row with special keys
                                row.forEachIndexed { index, key ->
                                    when (key) {
                                        "123" -> {
                                            KeyboardKeyQwerty(
                                                keyboardKey = key,
                                                modifier = Modifier.weight(1.5f),
                                                backgroundColor = Color(0xFFD1D6DB),
                                                onClick = {
                                                    currentKeyboardMode = KeyboardMode.NUMERIC
                                                }
                                            )
                                        }
                                        "ABC" -> {
                                            KeyboardKeyQwerty(
                                                keyboardKey = key,
                                                modifier = Modifier.weight(1.5f),
                                                backgroundColor = Color(0xFFD1D6DB),
                                                onClick = {
                                                    currentKeyboardMode = KeyboardMode.QWERTY
                                                }
                                            )
                                        }
                                        "SPACE" -> {
                                            KeyboardKeyQwerty(
                                                keyboardKey = " ",
                                                modifier = Modifier.weight(5f),
                                                backgroundColor = Color(0xFFFFFFFF),
                                                onClick = {
                                                    currentKeyPressed.value = " "
                                                    val inputConnection = (context as IMEService).currentInputConnection
                                                    inputConnection.commitText(" ", 1)
                                                }
                                            )
                                        }
                                        "Enter" -> {
                                            EnterKeyQwerty(modifier = Modifier.weight(1.5f))
                                        }
                                        else -> {
                                            KeyboardKeyQwerty(
                                                keyboardKey = key,
                                                modifier = Modifier.weight(1f),
                                                backgroundColor = Color(0xFFFFFFFF),
                                                onClick = {
                                                    currentKeyPressed.value = key
                                                    val inputConnection = (context as IMEService).currentInputConnection
                                                    inputConnection.commitText(key, 1)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                row.forEachIndexed { index, key ->
                                    when (key) {
                                        "SHIFT" -> {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1.5f)
                                                    .fillMaxHeight(),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                val backgroundColor = when {
                                                    isCapsLockEnabled -> Color(0xFF3399FF) // blue for caps lock
                                                    isShiftEnabled -> Color(0xFF99E6C8)    // light green for single shift
                                                    else -> Color(0xFFD1D6DB)
                                                }
                                                val interactionSource = remember { MutableInteractionSource() }
                                                val isPressed = interactionSource.collectIsPressedAsState()

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(2.dp)
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = Color.Black.copy(alpha = 0.1f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable(interactionSource = interactionSource, indication = null) {
                                                            val currentTime = System.currentTimeMillis()
                                                            if (currentTime - lastShiftTapTime < 400) {
                                                                // Double tap → toggle caps lock
                                                                isCapsLockEnabled = !isCapsLockEnabled
                                                                isShiftEnabled = isCapsLockEnabled
                                                            } else {
                                                                // Single tap logic
                                                                if (isCapsLockEnabled) {
                                                                    // If in Caps Lock, disable both
                                                                    isCapsLockEnabled = false
                                                                    isShiftEnabled = false
                                                                } else {
                                                                    // Normal shift toggle
                                                                    isShiftEnabled = !isShiftEnabled
                                                                }
                                                            }
                                                            lastShiftTapTime = currentTime
                                                        }
                                                        .background(
                                                            if (isPressed.value) backgroundColor.copy(alpha = 0.7f) else backgroundColor
                                                        )
                                                        .padding(
                                                            start = 8.dp,
                                                            end = 8.dp,
                                                            top = 16.dp,
                                                            bottom = 16.dp
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.keyboardshiftlock),
                                                        contentDescription = "Shift",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                        "BS" -> {
                                            RemoveKeyQwerty(modifier = Modifier.weight(1.5f))
                                        }
                                        "=\\<" -> {
                                            // Special handling for SYM key
                                            KeyboardKeyQwerty(
                                                keyboardKey = key,
                                                modifier = Modifier.weight(1.5f),
                                                backgroundColor = Color(0xFFD1D6DB),
                                                onClick = {
                                                    currentKeyboardMode = KeyboardMode.SYMBOL
                                                }
                                            )
                                        }
                                        "123" -> {
                                            // Special handling for 123 key in symbol layout
                                            KeyboardKeyQwerty(
                                                keyboardKey = key,
                                                modifier = Modifier.weight(1.5f),
                                                backgroundColor = Color(0xFFD1D6DB),
                                                onClick = {
                                                    currentKeyboardMode = KeyboardMode.NUMERIC
                                                }
                                            )
                                        }
                                        "ABC" -> {
                                            // Special handling for ABC key in non-bottom rows
                                            KeyboardKeyQwerty(
                                                keyboardKey = key,
                                                modifier = Modifier.weight(1.5f),
                                                backgroundColor = Color(0xFFD1D6DB),
                                                onClick = {
                                                    currentKeyboardMode = KeyboardMode.QWERTY
                                                }
                                            )
                                        }
                                        else -> {
                                            // For letter keys, apply shift if enabled
                                            val displayKey = if ((currentKeyboardMode == KeyboardMode.QWERTY) && (isShiftEnabled || isCapsLockEnabled)) {
                                                key.uppercase()
                                            } else {
                                                key
                                            }

                                            KeyboardKeyQwerty(
                                                keyboardKey = displayKey,
                                                modifier = Modifier.weight(1f),
                                                backgroundColor = Color(0xFFFFFFFF),
                                                onClick = {
                                                    currentKeyPressed.value = displayKey
                                                    val inputConnection = (context as IMEService).currentInputConnection
                                                    inputConnection.commitText(displayKey, 1)

                                                    // Auto-disable shift after a single letter is typed
                                                    if (isShiftEnabled && !isCapsLockEnabled && currentKeyboardMode == KeyboardMode.QWERTY) {
                                                        isShiftEnabled = false
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class KeyboardMode {
    QWERTY, NUMERIC, SYMBOL
}

@Composable
fun KeyboardKeyQwerty(
    keyboardKey: String,
    modifier: Modifier,
    backgroundColor: Color = Color(0xFFFFFFFF),
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = interactionSource, indication = null) {
                    onClick?.invoke()
                    if (keyboardKey.length == 1 && keyboardKey != "⇧" && onClick == null) {
                        // Handle typing single characters when no custom onClick provided
                        (ctx as IMEService).currentInputConnection.commitText(keyboardKey, 1)
                    }
                }
                .background(
                    if (pressed.value) backgroundColor.copy(alpha = 0.7f) else backgroundColor
                )
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                keyboardKey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Show pressed state
        if (pressed.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp, vertical = 0.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor.copy(alpha = 0.9f))
                    .padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    keyboardKey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RemoveKeyQwerty(modifier: Modifier) {
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
                delay(600) // Threshold before rapid delete starts
                while (isLongPressing) {
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    inputConnection.deleteSurroundingText(1, 0) // Delete one character
                    delay(100) // Repeat delete faster for better UX
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = interactionSource, indication = null) {
                    val inputConnection = (ctx as IMEService).currentInputConnection

                    // Check if there is any selected text and delete it
                    if (inputConnection.getSelectedText(0) != null &&
                        inputConnection.getSelectedText(0) != ""
                    ) {
                        inputConnection.commitText("", 1) // Clear selected text
                    } else {
                        inputConnection.deleteSurroundingText(1, 0) // Remove one character
                    }
                }
                .background(
                    if (isPressed.value) Color(0xFFD1D6DB).copy(alpha = 0.7f)
                    else Color(0xFFD1D6DB)
                )
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.backspace),
                contentDescription = "Backspace",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EnterKeyQwerty(modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val ctx = LocalContext.current
    val isPressed = interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = interactionSource, indication = null) {
                    // Commit an Enter action when the Enter key is pressed
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    inputConnection.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                    )
                    inputConnection.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
                    )
                }
                .background(
                    if (isPressed.value) Color(0xFF99E6C8).copy(alpha = 0.7f)
                    else Color(0xFF99E6C8)
                )
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.entercheck),
                contentDescription = "Enter",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FixedHeightBoxQwerty(modifier: Modifier, height: Dp, content: @Composable () -> Unit) {
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


