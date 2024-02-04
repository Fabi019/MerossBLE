package dev.fabik.merossble.protocol.payloads

import org.json.JSONObject

data class DeviceInfo(
    val type: String,
    val version: String,
    val chip: String,
    val mac: String,
    val mqtt: String,
    val port: Int,
    val firm: String,
    val uuid: String,
    val userId: String
)

fun JSONObject.toDeviceInfo(): DeviceInfo {
    val system = getJSONObject("all").getJSONObject("system")

    val hardware = system.getJSONObject("hardware")
    val type = hardware.getString("type")
    val version = hardware.getString("version")
    val chip = hardware.getString("chipType")
    val mac = hardware.getString("macAddress")
    val uuid = hardware.getString("uuid")

    val firmware = system.getJSONObject("firmware")
    val firm = firmware.getString("version")
    val mqtt = firmware.getString("server")
    val port = firmware.getInt("port")
    val userId = firmware.getString("userId")


    return DeviceInfo(
        type,
        version,
        chip,
        mac,
        mqtt,
        port,
        firm,
        uuid,
        userId
    )
}