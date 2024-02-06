package dev.fabik.merossble.protocol

import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.sign

//   "header": {
//    "from": "",
//    "messageId": "65b5bf558d6eba17cd65cf7e24353b73",
//    "method": "GET",
//    "namespace": "Appliance.System.All",
//    "payloadVersion": 1,
//    "sign": "6d9e7c0cdaee219bd68a07b0d83a117f",
//    "timestamp": 1706641926,
//    "triggerSrc": "AndroidLocal"
//   },

class Header(
    val from: String = "",
    val method: String,
    val namespace: String,
    val payloadVersion: Int = 1,
    val triggerSrc: String = "AndroidLocal",
) {
    var messageId: String
    var sign: String
    var timestamp: Int

    companion object {
        const val KEY = "key"
    }

    init {
        val md5 = MessageDigest.getInstance("MD5")
        messageId = bytes2hex(md5.digest(UUID.randomUUID().toString().toByteArray()))
        timestamp = (System.currentTimeMillis() / 1000).toInt()
        sign = bytes2hex(md5.digest((messageId + KEY + timestamp).toByteArray()))
    }

}

fun Header.toJSONObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("from", from)
    jsonObject.put("messageId", messageId)
    jsonObject.put("method", method)
    jsonObject.put("namespace", namespace)
    jsonObject.put("payloadVersion", payloadVersion)
    jsonObject.put("sign", sign)
    jsonObject.put("timestamp", timestamp)
    jsonObject.put("triggerSrc", triggerSrc)
    return jsonObject
}

fun JSONObject.toHeader() = Header(
    from = getString("from"),
    method = getString("method"),
    namespace = getString("namespace"),
    payloadVersion = getInt("payloadVersion"),
    triggerSrc = getString("triggerSrc")
).apply {
    messageId = getString("messageId")
    sign = getString("sign")
    timestamp = getInt("timestamp")
}