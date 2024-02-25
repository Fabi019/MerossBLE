package dev.fabik.merossble.protocol

import org.json.JSONObject

class Packet(
    private var header: Header,
    private val payload: JSONObject = JSONObject(),
) {

    fun serializePacket(): ByteArray {
        val json = toString()
        val data = ByteArray(json.length + 10)

        insertAt(data, byteArrayOf(0x55.toByte(), 0xAA.toByte()), 0)
        insertAt(data, short2bytes(json.length.toShort()), 2)
        insertAt(data, json.toByteArray(), 4)

        val offset = json.length + 4
        insertAt(data, int2bytes(crc32(json.toByteArray()).toInt()), offset)
        insertAt(data, byteArrayOf(0xAA.toByte(), 0x55.toByte()), offset + 4)

        return data
    }

    override fun toString() = JSONObject().apply {
        put("header", header.toJSONObject())
        put("payload", payload)
    }.toString()

}
