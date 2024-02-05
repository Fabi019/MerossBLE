package dev.fabik.merossble.protocol.payloads

import org.json.JSONObject
import java.security.MessageDigest

//      "userId": "***",
//      "key": "***",
//      "gateway": {
//        "secondHost": "mqtt-eu-3.meross.com",
//        "secondPort": 443,
//        "port": 443,
//        "host": "mqtt-eu-3.meross.com"
//      }

data class KeyConfig(
    val userId: String,
    val key: String,
    val gateway: Gateway
)

data class Gateway(
    val secondHost: String,
    val secondPort: Int,
    val port: Int,
    val host: String
)

fun KeyConfig.toJSONObject() = JSONObject().apply {
    put("userId", userId)
    put("key", key)
    put("gateway", gateway.toJSONObject())
}

fun Gateway.toJSONObject() = JSONObject().apply {
    put("secondHost", secondHost)
    put("secondPort", secondPort)
    put("port", port)
    put("host", host)
}