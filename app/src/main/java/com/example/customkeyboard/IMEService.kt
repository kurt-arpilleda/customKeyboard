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
import android.view.inputmethod.InputMethodManager
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

class IMEService : LifecycleInputMethodService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val handler = Handler(Looper.getMainLooper())
    private var currentScannedCode: String? = null
    private var processingInProgress = false

    // BroadcastReceiver to handle scanned codes
    private val scannedCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scannedCode = intent.getStringExtra("SCANNED_CODE") ?: return

            Log.d("IMEService", "Received scanned code: $scannedCode")

            // Cancel any pending processing
            handler.removeCallbacksAndMessages(null)

            currentScannedCode = scannedCode
            processingInProgress = true

            // Ensure we have focus before processing the code
            ensureInputFocus {
                processCurrentCode()
            }
        }
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
        val filter = IntentFilter("com.example.customkeyboard.SCANNED_CODE")
        registerReceiver(scannedCodeReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scannedCodeReceiver)
        handler.removeCallbacksAndMessages(null)
    }

    override val viewModelStore: ViewModelStore
        get() = store

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private fun ensureInputFocus(callback: () -> Unit) {
        window?.window?.decorView?.let { view ->
            view.post {
                view.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                callback()
            }
            return
        }
        callback()
    }

    private fun processCurrentCode() {
        currentScannedCode?.let { code ->
            Log.d("IMEService", "Processing code: $code")

            currentInputConnection?.apply {
                // Commit the text
                commitText(code, 1)

                // Simulate Enter key press
                val now = System.currentTimeMillis()
                val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0)
                val upEvent = KeyEvent(now, now + 100, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0)

                sendKeyEvent(downEvent)
                sendKeyEvent(upEvent)

                Log.d("IMEService", "Code committed: $code")
            } ?: run {
                Log.e("IMEService", "No current input connection")
                // Retry after a short delay if we don't have input connection
                handler.postDelayed({
                    processCurrentCode()
                }, 100)
            }
        }

        // Reset state
        currentScannedCode = null
        processingInProgress = false
    }
}

// Custom Compose view for the keyboard
class ComposeKeyboardView(context: Context) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        ScannerScreen()
    }
}