package com.gmail.marcosav2010.communicator.packet.packets

import com.gmail.marcosav2010.communicator.packet.StandardPacket
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import java.util.*
import kotlin.properties.Delegates

class PacketIdentify() : StandardPacket() {

    companion object {
        const val SUCCESS: Byte = 0
        const val INVALID_UUID: Byte = 1
        const val TIMED_OUT: Byte = 2
    }

    var name: String? = null
        private set

    var newUUID: UUID? = null
        private set

    var peerUUID by Delegates.notNull<UUID>()
        private set

    var result: Byte = 0
        private set

    constructor(name: String, newUUID: UUID?, peerUUID: UUID, result: Byte) : this() {
        this.name = name
        this.newUUID = newUUID
        this.peerUUID = peerUUID
        this.result = result
    }

    fun providesUUID() = newUUID != null

    private fun providesName() = name != null && !name!!.isBlank()

    override fun encodeContent(out: PacketEncoder) {
        out.write(providesName())
        if (providesName()) out.write(name!!)
        out.write(providesUUID())
        if (providesUUID()) out.write(newUUID!!)
        out.write(peerUUID)
        out.write(result)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        if (`in`.readBoolean()) name = `in`.readString()
        if (`in`.readBoolean()) newUUID = `in`.readUUID()
        peerUUID = `in`.readUUID()
        result = `in`.readByte()
    }
}