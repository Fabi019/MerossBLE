package dev.fabik.merossble.protocol

import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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

fun insertAt(array: ByteArray, value: ByteArray, index: Int, start: Int = 0, length: Int = value.size) {
    runCatching {
        System.arraycopy(value, start, array, index, length)
    }.onFailure {
        Log.e("insertAt", "Failed to insert value at index $index", it)
    }
}

fun crc32(data: ByteArray) = CRC32().apply {
    update(data)
}.value

fun bytes2hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

fun bytes2ascii(bytes: ByteArray) =
    bytes.joinToString("") { it.toInt().toChar().toString() }

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

fun calculateWifiXPassword(
    password: String,
    type: String,
    uuid: String,
    macAddress: String
): String {
    val md5 = MessageDigest.getInstance("MD5").apply {
        update((type + uuid + macAddress).toByteArray())
    }
    val key = bytes2hex(md5.digest())

    val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
    val secretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
    val ivParameterSpec = IvParameterSpec("0000000000000000".toByteArray())

    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

    // pad to multiples of 16
    val padding = (16 - (password.length % 16)) + password.length
    val padded = password.padEnd(padding, '\u0000')

    val encrypted = cipher.doFinal(padded.toByteArray())

    return Base64.encodeToString(encrypted, Base64.NO_WRAP)
}