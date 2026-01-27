package com.example.responderapp

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.responderapp.data.nfc.NfcManager
import com.example.responderapp.ui.theme.ResponderAppTheme
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import com.example.responderapp.ui.navigation.AppNavigation
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var nfcManager: NfcManager
    
    private var nfcAdapter: NfcAdapter? = null
    private var nfcTagCallback: ((Tag) -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        setContent {
            ResponderAppTheme {
                AppNavigation()
            }
        }
        
        // Handle NFC intent if activity was started from NFC tag
        handleNfcIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // Enable foreground dispatch for NFC tag detection
        nfcManager.enableForegroundDispatch(this, nfcAdapter)
    }
    
    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch to save battery
        nfcManager.disableForegroundDispatch(this, nfcAdapter)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }
    
    private fun handleNfcIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                Log.d("MainActivity", "NFC tag detected")
                nfcTagCallback?.invoke(it)
            }
        }
    }
    
    /**
     * Register a callback to be invoked when an NFC tag is detected.
     * This allows ViewModels to receive NFC tag events.
     */
    fun setNfcTagCallback(callback: ((Tag) -> Unit)?) {
        nfcTagCallback = callback
    }
    
    /**
     * Get the current NFC adapter instance
     */
    fun getNfcAdapter(): NfcAdapter? = nfcAdapter
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ResponderAppTheme {
        Greeting("Android")
    }
}