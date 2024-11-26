package com.example.customkeyboard

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KeyboardScreen() {
    var isCapsLockOn by remember { mutableStateOf(false) }
    var isNumericKeyboardOn by remember { mutableStateOf(false) } // State to track the keyboard layout (ABC vs 123)

    val alphabeticKeysMatrix = arrayOf(
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        arrayOf("z", "x", "c", "v", "b", "n", "m"),
        arrayOf(",", " ", ".", "Enter")
    )

    val numericKeysMatrix = arrayOf(
        arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        arrayOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
        arrayOf("*", "\"", "'", ":", ";", "!", "?"),   // Adjusted symbols
        arrayOf(",", " ", ".", "Enter")
    )

    val keysMatrix = if (isNumericKeyboardOn) numericKeysMatrix else alphabeticKeysMatrix

    Column(
        modifier = Modifier
            .background(Color(0xFFB0B0B0))
            .fillMaxWidth()
    ) {
        keysMatrix.forEachIndexed { rowIndex, row ->
            FixedHeightBox(modifier = Modifier.fillMaxWidth(), height = 56.dp) {
                Row(Modifier) {
                    if (rowIndex == 3) { // Change Keyboard Button stays in row 3
                        ChangeKeyboardButton(
                            modifier = Modifier.weight(1f),
                            isNumericKeyboardOn = isNumericKeyboardOn,
                            onClick = { isNumericKeyboardOn = !isNumericKeyboardOn }
                        )
                    }

                    if (!isNumericKeyboardOn && rowIndex == 2) { // Only show CapsLock in alphabetic layout
                        CapsLockButton(
                            modifier = Modifier.weight(1f),
                            isCapsLockOn = isCapsLockOn,
                            onDoubleClick = { isCapsLockOn = !isCapsLockOn }
                        )
                    }

                    row.forEachIndexed { index, key ->
                        if (key == " ") {
                            KeyboardKey(
                                keyboardKey = key,
                                modifier = Modifier.weight(3.5f) // Slightly larger for space
                            )
                        } else if (key == "Enter") {
                            EnterKey(modifier = Modifier.weight(1f))
                        } else {
                            // Capitalize letters when Caps Lock is on
                            KeyboardKey(
                                keyboardKey = if (isCapsLockOn) key.uppercase() else key,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (rowIndex == 2) { // Add Remove button in the last row
                        RemoveKey(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}



@Composable
fun ChangeKeyboardButton(
    modifier: Modifier,
    isNumericKeyboardOn: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val ctx = LocalContext.current

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            if (isNumericKeyboardOn) "AB" else "?1", // Toggle between the two states
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = interactionSource, indication = null) {
                    onClick() // Switch between alphabetic and numeric layouts
                }
                .background(Color.White)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        )
    }
}


@Composable
fun EnterKey(modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val ctx = LocalContext.current

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            "⏎", // Enter key symbol
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
                .clickable(interactionSource = interactionSource, indication = null) {
                    // Commit an Enter action when the Enter key is pressed
                    (ctx as IMEService).currentInputConnection.commitText("\n", 1)

                    // Simulate an enter key event (KEYCODE_ENTER)
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
                .background(Color.White)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        )
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

@Composable
fun KeyboardKey(
    keyboardKey: String,
    modifier: Modifier
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
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
                .clickable(interactionSource = interactionSource, indication = null) {
                    if (keyboardKey == " ") {
                        // Commit space when the space bar is pressed
                        (ctx as IMEService).currentInputConnection.commitText(" ", 1)
                    } else {
                        (ctx as IMEService).currentInputConnection.commitText(
                            keyboardKey,
                            keyboardKey.length
                        )
                    }
                }
                .background(Color.White)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        )
        if (pressed.value) {
            Text(
                keyboardKey,
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
                    .background(Color.White)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 48.dp
                    )
            )
        }
    }
}

@Composable
fun CapsLockButton(
    modifier: Modifier,
    isCapsLockOn: Boolean,
    onDoubleClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val ctx = LocalContext.current
    val isPressed = interactionSource.collectIsPressedAsState()

    // Tracking the time between clicks
    var lastClickTime by remember { mutableStateOf(0L) }

    LaunchedEffect(isPressed.value) {
        if (isPressed.value) {
            // Handle double-click detection with a time threshold
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 300) {
                onDoubleClick() // Trigger Caps Lock toggle on double-click
            }
            lastClickTime = currentTime
        }
    }

    // Determine the symbol to display
    val capsLockSymbol = if (isCapsLockOn) "⬆" else "⇧"

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            capsLockSymbol,
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
                .clickable(interactionSource = interactionSource, indication = null) {
                    // Do nothing on single click as double click will be handled
                }
                .background(Color.White)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        )
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
                delay(800) // Extended long-press threshold (800ms)
                while (isLongPressing) {
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    inputConnection.deleteSurroundingText(1, 0) // Delete one character
                    delay(100) // Repeat delete every 100ms while pressed
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
            "←", // Symbol for removing text
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
                .clickable(interactionSource = interactionSource, indication = null) {
                    val inputConnection = (ctx as IMEService).currentInputConnection

                    // Check if there is any selected text and delete it
                    if (inputConnection.getSelectedText(0) != null && inputConnection.getSelectedText(0) != "") {
                        inputConnection.commitText("", 1) // Clear selected text
                    } else {
                        inputConnection.deleteSurroundingText(1, 0) // Remove one character around the cursor
                    }
                }
                .background(Color.White)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        )
    }
}

