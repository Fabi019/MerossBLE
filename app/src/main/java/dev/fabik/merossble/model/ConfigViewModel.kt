package dev.fabik.merossble.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.fabik.merossble.protocol.payloads.DeviceInfo
import dev.fabik.merossble.protocol.payloads.KeyConfig
import dev.fabik.merossble.protocol.payloads.Wifi

class ConfigViewModel : ViewModel() {

    var wifiNetworks: MutableLiveData<List<Wifi>> = MutableLiveData(emptyList())
    var deviceInfo: MutableLiveData<DeviceInfo?> = MutableLiveData()

    var onRefresh: (() -> Unit)? = null
    var onUpdateTimestamp: ((String) -> Unit)? = null // timezone
    var onConfirmMqtt: ((KeyConfig) -> Unit)? = null // mqtt server, port, userId, key
    var onConfirmWifi: ((Wifi, String) -> Unit)? = null // ssid, password

    var onDataLoad: (() -> Unit)? = null

}