package dev.fabik.merossble.protocol

import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class Packet(
    private val header: Header,
    private val payload: JSONObject = JSONObject(),
    private val key: String = ""
) {

    fun calculateSignature() {
        val md5 = MessageDigest.getInstance("MD5")

        val messageId = bytes2hex(md5.digest(UUID.randomUUID().toString().toByteArray()))
        val timestamp = (System.currentTimeMillis() / 1000).toInt()
        val signature = bytes2hex(md5.digest((messageId + key + timestamp).toByteArray()))

        header.messageId = messageId
        header.timestamp = timestamp
        header.sign = signature
    }

    fun serializePacket(): ByteArray {
        val json = toString()
        val data = ByteArray(json.length + 10)

        insertAt(data, byteArrayOf(0x55.toByte(), 0xAA.toByte()), 0)
        insertAt(data, short2bytes(json.length.toShort()), 2)
        insertAt(data, json.toByteArray(), 4)

        val offset = json.length + 4
        insertAt(data, int2bytes(crc32(json.toByteArray())), offset)
        insertAt(data, byteArrayOf(0xAA.toByte(), 0x55.toByte()), offset + 4)

        return data
    }

    override fun toString() = JSONObject().apply {
        put("header", header.toJSONObject())
        put("payload", payload)
    }.toString()

}