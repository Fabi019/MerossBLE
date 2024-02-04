package dev.fabik.merossble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import dev.fabik.merossble.fragments.LogFragment
import dev.fabik.merossble.protocol.Header
import dev.fabik.merossble.protocol.Packet
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context, private val bleCallback: BleCallback, private val logFragment: LogFragment) {

    companion object {
        const val TAG = "BleManager"

        const val SERVICE_UUID = "0000A00A-0000-1000-8000-00805F9B34FB"
        const val READ_CHARACTERISTIC_UUID = "0000B003-0000-1000-8000-00805F9B34FB"
        const val WRITE_CHARACTERISTIC_UUID = "0000B002-0000-1000-8000-00805F9B34FB"
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var maxPacketSize = 20
    private var receivingData = false

    private lateinit var currentPacket: StringBuffer

    init {
        // Initialize BluetoothAdapter
        val bluetoothManager = context.getSystemService(Activity.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Check if Bluetooth is supported and enabled
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            log("Bluetooth is not supported or not enabled")
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            log("onConnectionStateChange: $status, $newState")
            bleCallback.onConnectionStateChanged(newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                log("Connected to device")
                gatt?.requestMtu(203)
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                log("Disconnected from device")
                gatt?.close()
            }
            super.onConnectionStateChange(gatt, status, newState)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("onCharacteristicWrite: Success")
            } else {
                log("onCharacteristicWrite: Failure")
            }
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            log("onCharacteristicRead: ${String(value)}, $status")
            super.onCharacteristicRead(gatt, characteristic, value, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (value.size >= 4 && value[0] == 0x55.toByte() && value[1] == 0xAA.toByte()) {
                log("Received start of packet...")
                currentPacket = StringBuffer()
                receivingData = true

                val packetSizeHI = value[2].toInt()
                val packetSizeLO = value[3].toInt()

                val packetSize = ((packetSizeHI and 0xFF) shl 8) or packetSizeLO and 0xFF
                log("Total packet size: $packetSize")

                currentPacket.append(Packet.bytes2ascii(value.sliceArray(4 until value.size)))
            } else if (value.size >= 6 && value[value.size - 2] == 0xAA.toByte() && value[value.size - 1] == 0x55.toByte()) {
                log("Received end of packet")
                receivingData = false

                val crc = value.sliceArray(value.size - 6 until value.size - 2)
                log("Checksum (CRC32) is: ${Packet.bytes2hex(crc)} [not validated]")

                currentPacket.append(Packet.bytes2ascii(value.sliceArray(0 until value.size - 6)))

                log("Received: $currentPacket")
                bleCallback.onPacketReceived(currentPacket.toString())
            } else {
                log("Received data chunk (length: ${value.size})")
                currentPacket.append(Packet.bytes2ascii(value))
            }

            super.onCharacteristicChanged(gatt, characteristic, value)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("onMtuChanged: Success")
                log("MTU changed to $mtu")

                maxPacketSize = mtu - 3

                log("Discovering services...")
                gatt?.discoverServices()
            } else {
                log("onMtuChanged: Failure ($status)")
            }
            super.onMtuChanged(gatt, mtu, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("onDescriptorWrite: Success")
            } else {
                log("onDescriptorWrite: Failure ($status)")
            }
            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val service = gatt?.getService(UUID.fromString(SERVICE_UUID))

            if (service == null) {
                log("onServicesDiscovered: Service UUID not found")
                return
            }

            log("onServicesDiscovered: $service found")

            readCharacteristic = service.getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID))
            writeCharacteristic = service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID))

            if (readCharacteristic == null || writeCharacteristic == null) {
                log("onServicesDiscovered: RX/TX characteristics not found")
                return
            }

            log("Enabling notifications...")

            gatt.setCharacteristicNotification(readCharacteristic, true)
            readCharacteristic?.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val s = gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    log("WriteDescriptor returned $s")
                } else {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val s = gatt.writeDescriptor(it)
                    log("legacy WriteDescriptor returned $s")
                }
            }

            bleCallback.onConnectionStateChanged(99)

            super.onServicesDiscovered(gatt, status)
        }
    }

    fun scan() {
        log("Scanning for devices...")

        val scanner = bluetoothAdapter?.bluetoothLeScanner

        // Set up scan filters and settings
        val filters = mutableListOf<ScanFilter>()
        filters.add(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERVICE_UUID)).build())
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        // Start scanning
        scanner?.startScan(filters, settings, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let {
                    log("Device found: ${it.name} - ${it.address}")
                    scanner.stopScan(this)
                    connectToDevice(it.address)
                }
            }
        })
    }

    private fun connectToDevice(address: String) {
        runCatching {
            bluetoothGatt?.close()
            bluetoothDevice = bluetoothAdapter?.getRemoteDevice(address)
            bleCallback.onNewDevice(bluetoothDevice)
            bluetoothGatt = bluetoothDevice?.connectGatt(context, false, bluetoothGattCallback)
        }.onFailure {
            log("Failed to connect to device: ${it.message}")
        }
    }

    fun sendPacket(packet: Packet) {
        if (receivingData) {
            log("Cannot send packet while receiving data!")
            return
        }

        log("Preparing to send packet...")
        log("Packet: $packet")

        packet.calculateSignature()
        val data = packet.serializePacket()

        if (data.size >= maxPacketSize) {
            log("Data is too large to send in a single packet, splitting...")
            Packet.splitIntoChunks(data, maxPacketSize).forEach(::writeData)
        } else {
            writeData(data)
        }
    }

    private fun writeData(data: ByteArray) {
        log("Sending data (length: ${data.size})...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeCharacteristic?.let {
                bluetoothGatt?.writeCharacteristic(it, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            }
        } else {
            writeCharacteristic?.let {
                it.value = data
                bluetoothGatt?.writeCharacteristic(it)
            }
        }
    }

    fun disconnect() {
        logFragment.log("Disconnecting...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        bluetoothDevice = null
        receivingData = false
        bleCallback.onNewDevice(null)
    }

    fun log(message: String) {
        Log.d(TAG, message)
        (context as Activity).runOnUiThread {
            logFragment.log(message)
        }
    }
}

interface BleCallback {
    fun onPacketReceived(packetJson: String)
    fun onNewDevice(device: BluetoothDevice?)
    fun onConnectionStateChanged(state: Int)
}