package com.gmail.marcosav2010.communicator.packet.wrapper

import com.gmail.marcosav2010.communicator.packet.AbstractPacket
import com.gmail.marcosav2010.communicator.packet.PacketRegistry.getById
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketReadException
import java.io.ByteArrayInputStream

class PacketReader {

    fun read(bytes: ByteArray): AbstractPacket {
        var packet: AbstractPacket

        try {
            ByteArrayInputStream(bytes).use {
                PacketDecoder(it).use { decoder ->
                    val packetType = decoder.readByte()
                    val packetClass = getById(packetType)
                            ?: throw PacketReadException("Packet type ${packetType.toInt()} not recognized")

                    packet = packetClass.getConstructor().newInstance()!!
                    packet.decode(decoder)

                    return packet
                }
            }
        } catch (ex: PacketReadException) {
            throw ex
        } catch (ex: Exception) {
            throw PacketReadException(ex)
        }
    }
}