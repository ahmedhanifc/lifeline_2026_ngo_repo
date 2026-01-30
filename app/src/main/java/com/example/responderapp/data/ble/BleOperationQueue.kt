package com.example.responderapp.data.ble

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes BLE GATT operations to ensure only one runs at a time.
 * 
 * BLE GATT operations on Android must be executed one at a time -
 * starting a new operation before the previous one completes will
 * cause undefined behavior. This queue uses a Mutex to ensure
 * sequential execution.
 */
class BleOperationQueue {
    private val mutex = Mutex()
    
    /**
     * Execute a BLE operation with exclusive access.
     * 
     * @param operation The suspend function to execute
     * @return The result of the operation
     */
    suspend fun <T> execute(operation: suspend () -> T): T {
        return mutex.withLock {
            operation()
        }
    }
}
