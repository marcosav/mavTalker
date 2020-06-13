package com.gmail.marcosav2010.communicator.packet.wrapper

import com.gmail.marcosav2010.communicator.packet.AbstractPacket
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.OverExceededByteLimitException
import java.io.Closeable
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*

class PacketEncoder(private val out: OutputStream) : Closeable {

    private var length = 0

    fun write(b: Boolean) {
        write((if (b) 1 else 0).toByte())
    }

    fun write(b: Byte) {
        addAndCheck(1)
        out.write(b.toInt())
    }

    fun write(b: Short) {
        out.write(serialize(b.toLong(), 2))
    }

    fun write(b: Int) {
        out.write(serialize(b.toLong(), 4))
    }

    fun write(b: Long) {
        out.write(serialize(b, 8))
    }

    fun write(buf: ByteArray) {
        addAndCheck(buf.size)
        write(buf.size)
        out.write(buf)
    }

    fun write(s: String) {
        write(s.toByteArray(StandardCharsets.UTF_8))
    }

    fun write(uuid: UUID) {
        write(uuid.mostSignificantBits)
        write(uuid.leastSignificantBits)
    }

    private fun serialize(o: Long, size: Int): ByteArray {
        addAndCheck(size)
        val array = ByteArray(size)
        for (i in 0 until size) array[i] = (o shr 8 * (size - i - 1) and 0xFFL).toByte()
        return array
    }

    private fun addAndCheck(i: Int) {
        length += i
        if (length > AbstractPacket.BASE_SIZE) throw OverExceededByteLimitException(length.toLong())
    }

    override fun close() {
        out.close()
    }
}