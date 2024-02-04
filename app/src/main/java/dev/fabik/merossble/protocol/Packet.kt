package dev.fabik.merossble.protocol

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

class Packet(
    private val header: Header,
    private val payload: JSONObject = JSONObject(),
    private val key: String = ""
) {

    companion object {
        fun short2bytes(s: Short): ByteArray {
            return byteArrayOf((s.toInt() shr 8).toByte(), s.toByte())
        }

        fun int2bytes(i: Int): ByteArray {
            return byteArrayOf(
                (i shr 24).toByte(),
                (i shr 16).toByte(),
                (i shr 8).toByte(),
                i.toByte()
            )
        }

        fun string2bytes(string: String): ByteArray {
            check(string.length % 2 == 0) { "Must have an even length" }
            return string.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

        fun insertAt(array: ByteArray, value: ByteArray, index: Int) {
            System.arraycopy(value, 0, array, index, value.size)
        }

        fun crc32(data: ByteArray): Int {
            val crc32 = CRC32()
            crc32.update(data)
            return crc32.value.toInt()
        }

        fun bytes2hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

        fun bytes2ascii(bytes: ByteArray) = bytes.joinToString("") { it.toInt().toChar().toString() }

        fun splitIntoChunks(data: ByteArray, maxSize: Int): List<ByteArray> {
            val chunks = mutableListOf<ByteArray>()
            var offset = 0
            while (offset < data.size) {
                val chunkSize = minOf(maxSize, data.size - offset)
                val chunk = ByteArray(chunkSize)
                System.arraycopy(data, offset, chunk, 0, chunkSize)
                chunks.add(chunk)
                offset += chunkSize
            }
            return chunks
        }

        fun calculateWifiXPassword(password: String, type: String, uuid: String, macAddress: String): String {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update((type + uuid + macAddress).toByteArray())
            val key = bytes2hex(md5.digest())

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val secretKeySpec = SecretKeySpec(key.toByteArray(), "AES-256")
            val ivParameterSpec = IvParameterSpec("0000000000000000".toByteArray())

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
            val padded = password.padEnd(16, '\u0000')
            val encrypted = cipher.doFinal(padded.toByteArray())

            return Base64.encodeToString(encrypted, Base64.DEFAULT).replace("\n", "")
        }

    }

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
        val packet = JSONObject()
        packet.put("header", header.toJSONObject())
        packet.put("payload", payload)
        val json = packet.toString()

        Log.d("MerossBLE", "Packet: $json")

        val data = ByteArray(json.length + 10)

        insertAt(data, byteArrayOf(0x55.toByte(), 0xAA.toByte()), 0)
        insertAt(data, short2bytes(json.length.toShort()), 2)
        insertAt(data, json.toByteArray(), 4)

        val offset = json.length + 4
        insertAt(data, int2bytes(crc32(json.toByteArray())), offset)
        insertAt(data, byteArrayOf(0xAA.toByte(), 0x55.toByte()), offset + 4)

        return data
    }

}