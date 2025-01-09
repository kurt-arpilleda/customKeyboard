package com.example.customkeyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

class IMEService : LifecycleInputMethodService(),

    ViewModelStoreOwner,

    SavedStateRegistryOwner {
    override fun onCreateInputView(): View {

        val view = ComposeKeyboardView(this)
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        return view
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
// Register the broadcast receiver for scanned codes
        val filter = IntentFilter("com.example.customkeyboard.SCANNED_CODE")
        registerReceiver(scannedCodeReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
// Unregister the broadcast receiver
        unregisterReceiver(scannedCodeReceiver)
    }
    override val viewModelStore: ViewModelStore
        get() = store
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
// Map to track the frequency of received codes
    private val codeFrequencyMap = mutableMapOf<String, Int>()
// List to track the order of received codes
    private val receivedCodes = mutableListOf<String>()
// BroadcastReceiver to handle scanned codes
    private val scannedCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scannedCode = intent.getStringExtra("SCANNED_CODE")
            if (!scannedCode.isNullOrEmpty()) {
// Increment the count for the scanned code
                codeFrequencyMap[scannedCode] = codeFrequencyMap.getOrDefault(scannedCode, 0) + 1
// Add to the received codes list
                receivedCodes.add(scannedCode)
//                Log.d("IMEService", "Received scanned code: $scannedCode")
// Post a delayed action to process the most frequent or last code
                Handler(Looper.getMainLooper()).postDelayed({
                    processCode()
                }, 1000)
            }
        }
    }
    private fun processCode() {
        if (codeFrequencyMap.isNotEmpty()) {
// Determine if there are duplicates
            val hasDuplicates = codeFrequencyMap.values.any { it > 1 }
            val codeToCommit = if (hasDuplicates) {
// Find the code with the highest frequency
                codeFrequencyMap.maxByOrNull { it.value }?.key
            } else {
// No duplicates, get the last received code
                receivedCodes.lastOrNull()
            }
            codeToCommit?.let {
                currentInputConnection?.apply {
// Commit the selected code
                    commitText(it, 1)
// Simulate the "Enter" key press (KeyEvent.KEYCODE_ENTER)
                    val enterKeyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                    sendKeyEvent(enterKeyEvent)
// Simulate key up for "Enter" to complete the action
                    val enterKeyUpEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
                    sendKeyEvent(enterKeyUpEvent)

                }
//                Log.d("IMEService", "Committed code: $it")
// Clear the frequency map and received codes list after committing
                codeFrequencyMap.clear()
                receivedCodes.clear()
            }
        }
    }
}
// Custom Compose view for the keyboard
class ComposeKeyboardView(context: Context) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        KeyboardScreen()
    }
}
