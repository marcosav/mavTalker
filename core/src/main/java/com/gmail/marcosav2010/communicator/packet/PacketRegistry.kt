package com.gmail.marcosav2010.communicator.packet

import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify
import com.gmail.marcosav2010.communicator.packet.packets.PacketPing
import com.gmail.marcosav2010.communicator.packet.packets.PacketResponse
import com.gmail.marcosav2010.communicator.packet.packets.PacketShutdown
import java.util.*

object PacketRegistry {

    private var packetsById: MutableMap<Byte, Class<out AbstractPacket?>> = HashMap()
    private var packetsByClass: MutableMap<Class<out AbstractPacket?>, Byte> = HashMap()

    init {
        register(0.toByte(), PacketIdentify::class.java)
        register(1.toByte(), PacketResponse::class.java)
        register(2.toByte(), PacketShutdown::class.java)

        register((-1).toByte(), PacketPing::class.java)
    }

    fun register(id: Byte, packet: Class<out AbstractPacket?>) {
        require(!(packetsById.containsKey(id) || packetsByClass.containsKey(packet))) {
            "Duplicated id or packet type"
        }

        packetsById[id] = packet
        packetsByClass[packet] = id
    }

    fun getById(id: Byte) = packetsById[id]

    fun getByClass(clazz: Class<out AbstractPacket?>) = packetsByClass[clazz]
}