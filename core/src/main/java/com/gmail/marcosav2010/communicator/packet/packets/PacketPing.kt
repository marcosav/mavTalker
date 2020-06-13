package com.gmail.marcosav2010.communicator.packet.packets

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder

class PacketPing : Packet() {

    override fun encodeContent(out: PacketEncoder) {}
    override fun decodeContent(`in`: PacketDecoder) {}
}