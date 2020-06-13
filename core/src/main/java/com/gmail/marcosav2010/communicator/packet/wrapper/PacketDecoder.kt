package com.gmail.marcosav2010.communicator.packet.wrapper

import com.gmail.marcosav2010.communicator.packet.AbstractPacket
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.OverExceededByteLimitException
import java.io.Closeable
import java.io.EOFException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.experimental.and

class PacketDecoder(private val input: InputStream) : Closeable {

    private var length = 0

    fun readBoolean(): Boolean {
        return readByte().toInt() == 1
    }

    fun readByte(): Byte {
        addAndCheck(1)
        return input.read().toByte()
    }

    fun readUByte(): Int {
        addAndCheck(1)
        return input.read()
    }

    fun readShort(): Short {
        return deserialize(2).toShort()
    }

    fun readUShort(): Int {
        return deserialize(2).toInt()
    }

    fun readInt(): Int {
        return deserialize(4).toInt()
    }

    fun readUInt(): Long {
        return deserialize(4)
    }

    fun readLong(): Long {
        return deserialize(8)
    }

    fun readBytes(): ByteArray {
        val len = readInt()
        return readFully(len)
    }

    fun readString(): String {
        val data = readBytes()
        return String(data, StandardCharsets.UTF_8)
    }

    fun readUUID(): UUID {
        val high = readLong()
        val low = readLong()
        return UUID(high, low)
    }

    private fun readFully(buffer: ByteArray) {
        var r: Int
        var offset = 0
        while (offset < buffer.size) {
            r = input.read(buffer, offset, buffer.size - offset)
            if (r < 0) throw EOFException()
            offset += r
        }
    }

    private fun readFully(size: Int): ByteArray {
        addAndCheck(size)
        val b = ByteArray(size)
        readFully(b)
        return b
    }

    private fun deserialize(size: Int): Long {
        return deserialize(readFully(size))
    }

    private fun deserialize(data: ByteArray): Long {
        var ret = 0L
        for (i in data.indices)
            ret += (data[i] and 0xFFL.toByte()).toInt() shl 8 * (data.size - i - 1)
        return ret
    }

    private fun addAndCheck(i: Int) {
        length += i
        if (length > AbstractPacket.BASE_SIZE) throw OverExceededByteLimitException(length.toLong())
    }

    override fun close() {
        input.close()
    }
}