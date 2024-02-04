package dev.fabik.merossble.protocol.payloads

import android.util.Base64
import dev.fabik.merossble.protocol.Packet
import org.json.JSONObject

data class Wifi(
    val ssid: String,
    val bssid: String,
    val channel: Int,
    val signal: Int,
    val encryption: Int,
    val cipher: Int
)

fun JSONObject.toWifiList(): List<Wifi> {
    val wifiList = getJSONArray("wifiList")
    val wifiNetworks = mutableListOf<Wifi>()
    for (i in 0 until wifiList.length()) {
        val wifi = wifiList.getJSONObject(i)
        wifiNetworks.add(wifi.toWifi())
    }
    return wifiNetworks
}

fun JSONObject.toWifi() = Wifi(
    ssid = Base64.decode(getString("ssid"), Base64.DEFAULT).toString(Charsets.UTF_8),
    bssid = getString("bssid"),
    channel = getInt("channel"),
    signal = getInt("signal"),
    encryption = getInt("encryption"),
    cipher = getInt("cipher")
)

fun Wifi.toJSONObject(password: String): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("cipher", cipher)
    jsonObject.put("password", password)
    jsonObject.put("encryption", encryption)
    jsonObject.put("bssid", bssid)
    jsonObject.put("channel", channel)
    jsonObject.put("ssid", Base64.encodeToString(ssid.toByteArray(Charsets.UTF_8), Base64.DEFAULT))
    return jsonObject
}