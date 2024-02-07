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
import androidx.fragment.app.FragmentActivity
import dev.fabik.merossble.fragments.LogFragment
import dev.fabik.merossble.protocol.Packet
import dev.fabik.merossble.protocol.*
import java.util.UUID
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BleManager(
    private val context: Context,
    private val bleCallback: BleCallback,
    private var logFragment: LogFragment? = null
) {

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

    private var writeIndex = 0
    private lateinit var receiveBuffer: ByteArray

    init {
        // Initialize BluetoothAdapter
        val bluetoothManager =
            context.getSystemService(Activity.BLUETOOTH_SERVICE) as BluetoothManager
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

                val packetSizeHI = value[2].toInt()
                val packetSizeLO = value[3].toInt()

                val packetSize = ((packetSizeHI and 0xFF) shl 8) + (packetSizeLO and 0xFF)
                log("Total packet size: $packetSize (${bytes2hex(value.sliceArray(2 until 4))})")

                receiveBuffer = ByteArray(packetSize + 6)
                System.arraycopy(value, 0, receiveBuffer, 0, value.size - 4) // strip length and magic
                writeIndex = value.size - 4
            } else if (value.size >= 2 && value[value.size - 2] == 0xAA.toByte() && value[value.size - 1] == 0x55.toByte()) {
                log("Received end of packet")

                System.arraycopy(value, 0, receiveBuffer, writeIndex, value.size - 2)
                writeIndex = 0

                val crc = receiveBuffer.sliceArray(receiveBuffer.size - 6 until receiveBuffer.size - 2)
                log("Checksum (CRC32) is: ${bytes2hex(crc)} [not validated]")

                val json = bytes2ascii(receiveBuffer.sliceArray(4 until receiveBuffer.size - 6))
                log("Received: $json")
                bleCallback.onPacketReceived(json)
            } else {
                log("Received data chunk (length: ${value.size})")

                System.arraycopy(value, 0, receiveBuffer, writeIndex, value.size)
                writeIndex += value.size
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

            log("onServicesDiscovered: ${service.uuid} found")

            readCharacteristic =
                service.getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID))
            writeCharacteristic =
                service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID))

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
                    val s =
                        gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
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
        filters.add(
            ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERVICE_UUID)).build()
        )
        val settings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

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
        if (writeIndex > 0) {
            log("Cannot send packet while receiving data!")
            return
        }

        log("Preparing to send packet: $packet")

        val data = packet.serializePacket()

        if (data.size >= maxPacketSize) {
            log("Data is too large to send in a single packet, splitting...")
            Executors.newSingleThreadExecutor().execute {
                splitIntoChunks(data, maxPacketSize).forEach {
                    writeData(it)
                    Thread.sleep(100)
                }
            }
        } else {
            writeData(data)
        }
    }

    private fun writeData(data: ByteArray) {
        log("Sending data (length: ${data.size})...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeCharacteristic?.let {
                bluetoothGatt?.writeCharacteristic(
                    it,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
            }
        } else {
            writeCharacteristic?.let {
                it.value = data
                bluetoothGatt?.writeCharacteristic(it)
            }
        }
    }

    fun disconnect() {
        log("Disconnecting...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        bluetoothDevice = null
        writeCharacteristic = null
        readCharacteristic = null
        writeIndex = 0
        bleCallback.onNewDevice(null)
    }

    fun log(message: String) {
        Log.d(TAG, message)
        if (logFragment == null) {
            (context as FragmentActivity).supportFragmentManager.findFragmentByTag("f1")?.let {
                logFragment = it as LogFragment
                logFragment?.log(message)
            }
        } else {
            logFragment?.log(message)
        }
    }
}

interface BleCallback {
    fun onPacketReceived(packetJson: String)
    fun onNewDevice(device: BluetoothDevice?)
    fun onConnectionStateChanged(state: Int)
}