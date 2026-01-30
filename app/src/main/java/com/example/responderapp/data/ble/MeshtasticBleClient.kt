package com.example.responderapp.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.geeksville.mesh.MeshProtos.FromRadio
import com.geeksville.mesh.MeshProtos.MeshPacket
import com.geeksville.mesh.MeshProtos.ToRadio
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * BLE client for connecting to Meshtastic radios and receiving mesh packets.
 * 
 * This client implements the Meshtastic BLE protocol:
 * 1. Scan for nearby BLE devices
 * 2. Connect to selected device
 * 3. Request MTU 512
 * 4. Discover services and find Meshtastic characteristics
 * 5. Write ToRadio.startConfig to initiate session
 * 6. Drain FromRadio until empty
 * 7. Enable notifications on FromNum
 * 8. On each FromNum notify, drain FromRadio again
 */
@Singleton
class MeshtasticBleClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager: BluetoothManager? = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var toRadioCharacteristic: BluetoothGattCharacteristic? = null
    private var fromRadioCharacteristic: BluetoothGattCharacteristic? = null
    private var fromNumCharacteristic: BluetoothGattCharacteristic? = null
    
    private val operationQueue = BleOperationQueue()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()
    
    private val _incomingPackets = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<MeshPacket> = _incomingPackets.asSharedFlow()
    
    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false
    private var connectedDeviceAddress: String? = null
    
    // Continuation for waiting on GATT callbacks
    private var connectionContinuation: CancellableContinuation<Boolean>? = null
    private var mtuContinuation: CancellableContinuation<Int>? = null
    private var servicesContinuation: CancellableContinuation<Boolean>? = null
    private var writeContinuation: CancellableContinuation<Boolean>? = null
    private var readContinuation: CancellableContinuation<ByteArray>? = null
    private var descriptorContinuation: CancellableContinuation<Boolean>? = null
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    
    /**
     * Start scanning for nearby BLE devices
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!isBluetoothEnabled()) {
            _connectionState.value = BleConnectionState.BluetoothOff
            return
        }
        
        stopScan()
        _discoveredDevices.value = emptyList()
        _connectionState.value = BleConnectionState.Scanning
        
        scanner = bluetoothAdapter?.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        scanner?.startScan(null, settings, scanCallback)
        isScanning = true
        
        // Auto-stop scan after 30 seconds
        scope.launch {
            delay(30_000)
            if (isScanning) {
                stopScan()
            }
        }
    }
    
    /**
     * Stop scanning for BLE devices
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            scanner?.stopScan(scanCallback)
            isScanning = false
            if (_connectionState.value is BleConnectionState.Scanning) {
                _connectionState.value = BleConnectionState.Idle
            }
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val bleDevice = BleDevice(
                name = device.name,
                address = device.address,
                rssi = result.rssi,
                device = device
            )
            
            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == bleDevice.address }
            
            if (existingIndex >= 0) {
                currentList[existingIndex] = bleDevice
            } else {
                currentList.add(bleDevice)
            }
            
            // Sort by signal strength (strongest first)
            _discoveredDevices.value = currentList.sortedByDescending { it.rssi }
        }
        
        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = BleConnectionState.Error("Scan failed with error: $errorCode")
            isScanning = false
        }
    }
    
    /**
     * Connect to a specific BLE device
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BleDevice) {
        stopScan()
        
        scope.launch {
            try {
                _connectionState.value = BleConnectionState.Connecting(device.displayName)
                connectedDeviceAddress = device.address
                
                // Connect GATT
                val connected = connectGatt(device.device)
                if (!connected) {
                    _connectionState.value = BleConnectionState.Error("Failed to connect to device")
                    return@launch
                }
                
                // Request MTU
                val mtu = requestMtu(MeshtasticBleConstants.RECOMMENDED_MTU)
                if (mtu < 0) {
                    _connectionState.value = BleConnectionState.Error("Failed to negotiate MTU")
                    disconnect()
                    return@launch
                }
                
                // Discover services
                _connectionState.value = BleConnectionState.DiscoveringServices
                val servicesFound = discoverServices()
                if (!servicesFound) {
                    _connectionState.value = BleConnectionState.Error("Failed to discover services")
                    disconnect()
                    return@launch
                }
                
                // Find characteristics
                if (!findCharacteristics()) {
                    _connectionState.value = BleConnectionState.Error("Meshtastic service not found")
                    disconnect()
                    return@launch
                }
                
                // Start Meshtastic session
                _connectionState.value = BleConnectionState.Syncing
                if (!startMeshtasticSession()) {
                    _connectionState.value = BleConnectionState.Error("Failed to start Meshtastic session")
                    disconnect()
                    return@launch
                }
                
                // Enable notifications
                if (!enableFromNumNotifications()) {
                    _connectionState.value = BleConnectionState.Error("Failed to enable notifications")
                    disconnect()
                    return@launch
                }
                
                _connectionState.value = BleConnectionState.Listening
                
            } catch (e: Exception) {
                _connectionState.value = BleConnectionState.Error("Connection error: ${e.message}")
                disconnect()
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun connectGatt(device: BluetoothDevice): Boolean {
        return suspendCancellableCoroutine { continuation ->
            connectionContinuation = continuation
            
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            
            continuation.invokeOnCancellation {
                connectionContinuation = null
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun requestMtu(mtu: Int): Int {
        return suspendCancellableCoroutine { continuation ->
            mtuContinuation = continuation
            bluetoothGatt?.requestMtu(mtu)
            
            continuation.invokeOnCancellation {
                mtuContinuation = null
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun discoverServices(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            servicesContinuation = continuation
            bluetoothGatt?.discoverServices()
            
            continuation.invokeOnCancellation {
                servicesContinuation = null
            }
        }
    }
    
    private fun findCharacteristics(): Boolean {
        val service = bluetoothGatt?.getService(MeshtasticBleConstants.SERVICE_UUID)
            ?: return false
            
        toRadioCharacteristic = service.getCharacteristic(MeshtasticBleConstants.TORADIO_UUID)
        fromRadioCharacteristic = service.getCharacteristic(MeshtasticBleConstants.FROMRADIO_UUID)
        fromNumCharacteristic = service.getCharacteristic(MeshtasticBleConstants.FROMNUM_UUID)
        
        return toRadioCharacteristic != null && 
               fromRadioCharacteristic != null && 
               fromNumCharacteristic != null
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun startMeshtasticSession(): Boolean {
        // Build startConfig message
        val startConfig = ToRadio.newBuilder()
            .setWantConfigId(true)
            .build()
        
        // Write to ToRadio
        val written = operationQueue.execute {
            writeToRadio(startConfig.toByteArray())
        }
        if (!written) return false
        
        // Drain FromRadio
        drainFromRadio()
        
        return true
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun writeToRadio(data: ByteArray): Boolean {
        val characteristic = toRadioCharacteristic ?: return false
        
        return suspendCancellableCoroutine { continuation ->
            writeContinuation = continuation
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeCharacteristic(characteristic)
            }
            
            continuation.invokeOnCancellation {
                writeContinuation = null
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun readFromRadio(): ByteArray {
        val characteristic = fromRadioCharacteristic ?: return ByteArray(0)
        
        return suspendCancellableCoroutine { continuation ->
            readContinuation = continuation
            bluetoothGatt?.readCharacteristic(characteristic)
            
            continuation.invokeOnCancellation {
                readContinuation = null
            }
        }
    }
    
    /**
     * Drain the FromRadio mailbox until it returns empty.
     * This is the "mailbox rule" - must read until empty after each notification.
     */
    private suspend fun drainFromRadio() {
        while (true) {
            val bytes = operationQueue.execute { readFromRadio() }
            if (bytes.isEmpty()) break
            
            try {
                val fromRadio = FromRadio.parseFrom(bytes)
                handleFromRadio(fromRadio)
            } catch (e: Exception) {
                // Log parse error but continue draining
            }
        }
    }
    
    private fun handleFromRadio(fromRadio: FromRadio) {
        when {
            fromRadio.hasPacket() -> {
                val packet = fromRadio.packet
                scope.launch {
                    _incomingPackets.emit(packet)
                }
            }
            // Handle other message types as needed (config, nodeInfo, etc.)
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun enableFromNumNotifications(): Boolean {
        val characteristic = fromNumCharacteristic ?: return false
        
        // Enable local notifications
        bluetoothGatt?.setCharacteristicNotification(characteristic, true)
        
        // Write to CCCD to enable remote notifications
        val descriptor = characteristic.getDescriptor(MeshtasticBleConstants.CCCD_UUID)
            ?: return false
            
        return suspendCancellableCoroutine { continuation ->
            descriptorContinuation = continuation
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeDescriptor(descriptor)
            }
            
            continuation.invokeOnCancellation {
                descriptorContinuation = null
            }
        }
    }
    
    /**
     * Disconnect from the current device
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        toRadioCharacteristic = null
        fromRadioCharacteristic = null
        fromNumCharacteristic = null
        connectedDeviceAddress = null
        _connectionState.value = BleConnectionState.Disconnected()
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionContinuation?.resume(true)
                    connectionContinuation = null
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectionContinuation?.resume(false)
                    connectionContinuation = null
                    
                    if (_connectionState.value is BleConnectionState.Listening) {
                        _connectionState.value = BleConnectionState.Disconnected("Connection lost")
                        // Could implement auto-reconnect here
                    }
                }
            }
        }
        
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuContinuation?.resume(mtu)
            } else {
                mtuContinuation?.resume(-1)
            }
            mtuContinuation = null
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            servicesContinuation?.resume(status == BluetoothGatt.GATT_SUCCESS)
            servicesContinuation = null
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            writeContinuation?.resume(status == BluetoothGatt.GATT_SUCCESS)
            writeContinuation = null
        }
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readContinuation?.resume(characteristic.value ?: ByteArray(0))
            } else {
                readContinuation?.resume(ByteArray(0))
            }
            readContinuation = null
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readContinuation?.resume(value)
            } else {
                readContinuation?.resume(ByteArray(0))
            }
            readContinuation = null
        }
        
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            descriptorContinuation?.resume(status == BluetoothGatt.GATT_SUCCESS)
            descriptorContinuation = null
        }
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == MeshtasticBleConstants.FROMNUM_UUID) {
                // FromNum notification - drain the mailbox
                scope.launch {
                    drainFromRadio()
                }
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == MeshtasticBleConstants.FROMNUM_UUID) {
                // FromNum notification - drain the mailbox
                scope.launch {
                    drainFromRadio()
                }
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopScan()
        disconnect()
        scope.cancel()
    }
}
