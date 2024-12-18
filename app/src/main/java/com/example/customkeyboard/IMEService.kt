package com.example.customkeyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
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
        registerReceiver(scannedCodeReceiver, filter, RECEIVER_NOT_EXPORTED)
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

    // BroadcastReceiver to handle scanned codes
    private val scannedCodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scannedCode = intent.getStringExtra("SCANNED_CODE")
            if (!scannedCode.isNullOrEmpty()) {
                Log.d("IMEService", "Received scanned code: $scannedCode")
                // Try posting a delayed commitText action
                Handler(Looper.getMainLooper()).postDelayed({
                    currentInputConnection?.commitText(scannedCode, 1)
                }, 500) // Delay for 500ms
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
