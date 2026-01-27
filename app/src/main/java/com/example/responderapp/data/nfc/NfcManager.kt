package com.example.responderapp.data.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import com.example.responderapp.data.model.NfcCaseData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling NFC operations including reading and writing NFC tags.
 */
@Singleton
class NfcManager @Inject constructor() {
    
    companion object {
        private const val TAG = "NfcManager"
        private const val MIME_TYPE = "application/vnd.com.example.responderapp.case"
    }

    /**
     * Check if NFC is available on the device
     */
    fun isNfcAvailable(adapter: NfcAdapter?): Boolean {
        return adapter != null
    }

    /**
     * Check if NFC is enabled
     */
    fun isNfcEnabled(adapter: NfcAdapter?): Boolean {
        return adapter?.isEnabled == true
    }

    /**
     * Create a PendingIntent for NFC tag discovery
     */
    fun createPendingIntent(activity: Activity): PendingIntent {
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Create NDEF message from NfcCaseData
     */
    fun createNdefMessage(caseData: NfcCaseData): NdefMessage {
        val jsonString = caseData.toJson()
        val mimeBytes = MIME_TYPE.toByteArray(Charsets.US_ASCII)
        val payload = jsonString.toByteArray(Charsets.UTF_8)
        
        val record = NdefRecord.createMime(MIME_TYPE, payload)
        return NdefMessage(arrayOf(record))
    }

    /**
     * Write NfcCaseData to an NFC tag
     * Returns true if successful, false otherwise
     */
    fun writeToTag(tag: Tag, caseData: NfcCaseData): Boolean {
        return try {
            val ndefMessage = createNdefMessage(caseData)
            val ndef = Ndef.get(tag)
            
            if (ndef != null) {
                // Tag is already formatted
                ndef.connect()
                if (!ndef.isWritable) {
                    Log.e(TAG, "Tag is not writable")
                    return false
                }
                if (ndef.maxSize < ndefMessage.byteArrayLength) {
                    Log.e(TAG, "Tag is too small. Required: ${ndefMessage.byteArrayLength}, Available: ${ndef.maxSize}")
                    return false
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                true
            } else {
                // Tag is not formatted, try to format it
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(ndefMessage)
                    formatable.close()
                    true
                } else {
                    Log.e(TAG, "Tag is not NDEF formatable")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to NFC tag", e)
            false
        }
    }

    /**
     * Read NfcCaseData from an NFC tag
     * Returns null if read fails
     */
    fun readFromTag(tag: Tag): NfcCaseData? {
        return try {
            val ndef = Ndef.get(tag) ?: return null
            ndef.connect()
            val ndefMessage = ndef.ndefMessage ?: return null
            
            // Find our MIME type record
            for (record in ndefMessage.records) {
                if (record.tnf == NdefRecord.TNF_MIME_MEDIA) {
                    val mimeType = String(record.type, Charsets.US_ASCII)
                    if (mimeType == MIME_TYPE) {
                        val payload = String(record.payload, Charsets.UTF_8)
                        ndef.close()
                        return NfcCaseData.fromJson(payload)
                    }
                }
            }
            ndef.close()
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from NFC tag", e)
            null
        }
    }

    /**
     * Enable foreground dispatch for NFC tag discovery
     */
    fun enableForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
        if (adapter == null || !adapter.isEnabled) return
        
        val pendingIntent = createPendingIntent(activity)
        val intentFilters = arrayOfNulls<android.content.IntentFilter>(0)
        val techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name)
        )
        
        adapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techLists)
    }

    /**
     * Disable foreground dispatch
     */
    fun disableForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
        adapter?.disableForegroundDispatch(activity)
    }
}
