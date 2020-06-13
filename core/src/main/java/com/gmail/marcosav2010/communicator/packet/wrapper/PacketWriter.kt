package com.gmail.marcosav2010.communicator.packet.wrapper

import com.gmail.marcosav2010.communicator.packet.AbstractPacket
import com.gmail.marcosav2010.communicator.packet.PacketRegistry.getByClass
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import java.io.ByteArrayOutputStream

class PacketWriter {

    fun write(packet: AbstractPacket): ByteArray {
        try {
            ByteArrayOutputStream().use { out ->
                PacketEncoder(out).use { encoder ->
                    val packetType = getByClass(packet.javaClass)
                            ?: throw PacketWriteException("Packet type ${packet.javaClass.simpleName} not recognized")
                    encoder.write(packetType)
                    packet.encode(encoder)

                    return out.toByteArray()
                }
            }
        } catch (ex: PacketWriteException) {
            throw ex
        } catch (ex: Exception) {
            throw PacketWriteException(ex)
        }
    }
}