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

fun KeyConfig.toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("userId", userId)

/*    val md5 = MessageDigest.getInstance("MD5")
    md5.update(key.toByteArray())
    val key = md5.digest().joinToString("") { "%02x".format(it) }*/
    jsonObject.put("key", key)

    jsonObject.put("gateway", gateway.toJSONObject())
    return jsonObject
}

fun Gateway.toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("secondHost", secondHost)
    jsonObject.put("secondPort", secondPort)
    jsonObject.put("port", port)
    jsonObject.put("host", host)
    return jsonObject
}