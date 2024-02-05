package dev.fabik.merossble.protocol

import org.json.JSONObject

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

data class Header(
    val from: String = "",
    var messageId: String = "",
    val method: String,
    val namespace: String,
    val payloadVersion: Int = 1,
    var sign: String = "",
    var timestamp: Int = 0,
    val triggerSrc: String = "AndroidLocal",
)

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
    messageId = getString("messageId"),
    method = getString("method"),
    namespace = getString("namespace"),
    payloadVersion = getInt("payloadVersion"),
    sign = getString("sign"),
    timestamp = getInt("timestamp"),
    triggerSrc = getString("triggerSrc")
)