package com.gmail.marcosav2010.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

object Utils {

    fun intToBytes(value: Int) =
            byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte())

    fun bytesToInt(array: ByteArray) = ByteBuffer.wrap(array).int
    //array[0].toInt() shl 24 or (array[1].toInt() and 0xFF shl 16) or (array[2].toInt() and 0xFF shl 8) or (array[3].toInt() and 0xFF)

    fun formatSize(bytes: Long): String {
        if (bytes == 0L) return "0 Byte"
        val k = 1024
        val i = floor(ln(bytes.toDouble()) / ln(k.toDouble())).toInt()
        val sizes = arrayOf("Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
        return round(bytes / k.toDouble().pow(i.toDouble()), 3).toString() + " " + sizes[i]
    }

    fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd = BigDecimal(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    fun concat(ba1: ByteArray, ba2: ByteArray): ByteArray {
        val out = ByteArray(ba1.size + ba2.size)
        System.arraycopy(ba1, 0, out, 0, ba1.size)
        System.arraycopy(ba2, 0, out, ba1.size, ba2.size)
        return out
    }

    fun concat(vararg arrays: ByteArray): ByteArray {
        val out = ByteArray(arrays.map { ba: ByteArray -> ba.size }.sum())
        var pos = 0
        for (ba in arrays) {
            System.arraycopy(ba, 0, out, pos, ba.size)
            pos += ba.size
        }
        return out
    }

    fun split(ba: ByteArray, pos: Int): Array<ByteArray> {
        val ba1 = ByteArray(pos)
        val ba2 = ByteArray(ba.size - pos)
        System.arraycopy(ba, 0, ba1, 0, pos)
        System.arraycopy(ba, pos, ba2, 0, ba2.size)
        return arrayOf(ba1, ba2)
    }

    fun getBytesFromUUID(uuid: UUID): ByteArray {
        val bb = ByteBuffer.allocate(16)
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

    fun getUUIDFromBytes(bytes: ByteArray): UUID {
        val byteBuffer = ByteBuffer.wrap(bytes)
        val high = byteBuffer.long
        val low = byteBuffer.long
        return UUID(high, low)
    }

    fun <K, V> put(map: MutableMap<K, MutableSet<V>>, key: K, value: V) {
        var collection = map[key]
        if (collection == null) collection = HashSet()
        collection.add(value)
        map[key] = collection
    }

    fun <K, V> remove(map: Map<K, MutableSet<V>>, value: V) = map.forEach { (_, v) -> v.remove(value) }

    fun encode(array: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(array) /* .replaceAll("_", "ñ").replaceAll("-", "Ñ") */

    fun decode(str: String): ByteArray = Base64.getUrlDecoder().decode(str /* .replaceAll("ñ", "_").replaceAll("Ñ", "-") */)

    fun toBase64(uuid: UUID) = encode(getBytesFromUUID(uuid))
}