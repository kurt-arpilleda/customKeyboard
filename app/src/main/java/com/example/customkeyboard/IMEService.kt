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

    companion object {
        const val SCANNED_CODE_ACTION = "com.example.customkeyboard.SCANNED_CODE"
        const val SCANNED_CODE_EXTRA = "SCANNED_CODE"
    }

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
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        // Register the broadcast receiver when input starts.
        val filter = IntentFilter(SCANNED_CODE_ACTION)
        registerReceiver(scannedCodeReceiver, filter, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_EXPORTED else 0)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        try {
            unregisterReceiver(scannedCodeReceiver)
        } catch (e: IllegalArgumentException) {
            // Handle the case where the receiver was not registered. This can happen in some edge cases.
//            Log.w("IMEService", "Receiver not registered: ${e.message}")
        }
        codeFrequencyMap.clear()
        receivedCodes.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        store.clear()
    }

    override val viewModelStore: ViewModelStore
        get() = store

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val store = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val codeFrequencyMap = mutableMapOf<String, Int>()
    private val receivedCodes = mutableListOf<String>()

    private val scannedCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scannedCode = intent.getStringExtra(SCANNED_CODE_EXTRA)
            scannedCode?.let { code ->
                codeFrequencyMap[code] = codeFrequencyMap.getOrDefault(code, 0) + 1
                receivedCodes.add(code)
//                Log.d("IMEService", "Received scanned code: $code")

                Handler(Looper.getMainLooper()).postDelayed({
                    processCode()
                }, 1000)
            }
        }
    }

    private fun processCode() {
        if (codeFrequencyMap.isNotEmpty()) {
            val hasDuplicates = codeFrequencyMap.values.any { it > 1 }
            val codeToCommit = if (hasDuplicates) {
                codeFrequencyMap.maxByOrNull { it.value }?.key
            } else {
                receivedCodes.lastOrNull()
            }

            codeToCommit?.let {
                currentInputConnection?.apply {
                    commitText(it, 1)
                    sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
//                Log.d("IMEService", "Committed code: $it")
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
