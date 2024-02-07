package dev.fabik.merossble.model

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.fabik.merossble.BleCallback
import dev.fabik.merossble.BleManager

class MainViewModel : ViewModel() {

    var bleManager: BleManager? = null

    var packetData: MutableLiveData<String> = MutableLiveData()
    var newDevice: MutableLiveData<BluetoothDevice?> = MutableLiveData()
    var connectionState: MutableLiveData<Int> = MutableLiveData()

    val bleCallback: BleCallback = object : BleCallback {
        override fun onPacketReceived(packetJson: String) {
            packetData.postValue(packetJson)
        }

        override fun onNewDevice(device: BluetoothDevice?) {
            newDevice.postValue(device)
        }

        override fun onConnectionStateChanged(state: Int) {
            connectionState.postValue(state)
        }
    }

    fun disconnect() {
        bleManager?.disconnect()
        connectionState.postValue(0)
    }

}