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
    val keysMatrix = arrayOf(
        arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        arrayOf("z", "x", "c", "v", "b", "n", "m"),
        arrayOf(",", " ", ".", "Enter")
    )

    Column(
        modifier = Modifier
            .background(Color(0xFFB0B0B0))
            .fillMaxWidth()
    ) {
        keysMatrix.forEachIndexed { rowIndex, row ->
            FixedHeightBox(modifier = Modifier.fillMaxWidth(), height = 56.dp) {
                Row(Modifier) {
                    if (rowIndex == 3) { // Add Caps Lock button before 'z'
                        CapsLockButton(
                            modifier = Modifier.weight(1f),
                            onDoubleClick = { isCapsLockOn = !isCapsLockOn }
                        )
                    }

                    row.forEachIndexed { index, key ->
                        if (key == " ") {
                            // Space should take up a little more space, but not too much
                            KeyboardKey(
                                keyboardKey = key,
                                modifier = Modifier.weight(3.5f) // Slightly larger than other keys
                            )
                        } else if (key == "Enter") {
                            // Add the Enter key on the right side of the dot
                            EnterKey(modifier = Modifier.weight(1f))
                        } else {
                            // Other keys (comma, dot) should have equal size as the other keys
                            KeyboardKey(
                                keyboardKey = if (isCapsLockOn) key.uppercase() else key,
                                modifier = Modifier.weight(1f) // Ensure comma, dot, and other keys are the same size
                            )
                        }
                    }

                    if (rowIndex == 3) { // Add remove button in the last row
                        RemoveKey(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
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

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            "C", // Symbol for Caps Lock
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

    LaunchedEffect(isPressed.value) {
        if (isPressed.value) {
            // Continuously delete when pressed
            launch {
                while (isPressed.value) {
                    val inputConnection = (ctx as IMEService).currentInputConnection
                    val selectedText = inputConnection.getSelectedText(0) // Get the selected text

                    if (selectedText.isNullOrEmpty()) {
                        // If no text is selected, delete one character at the cursor
                        inputConnection.deleteSurroundingText(1, 0)
                    } else {
                        // If text is selected, delete the entire selected text
                        inputConnection.deleteSurroundingText(selectedText.length, 0)
                    }

                    delay(100) // Delay between each character removal (adjust to your preference)
                }
            }
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
                    val selectedText = inputConnection.getSelectedText(0) // Get the selected text

                    if (selectedText.isNullOrEmpty()) {
                        // If no text is selected, delete one character at the cursor
                        inputConnection.deleteSurroundingText(1, 0)
                    } else {
                        // If text is selected, delete the entire selected text
                        inputConnection.deleteSurroundingText(selectedText.length, 0)
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
