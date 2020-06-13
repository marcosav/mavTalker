package com.gmail.marcosav2010.communicator.packet.packets

import com.gmail.marcosav2010.communicator.packet.StandardPacket
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder

class PacketShutdown : StandardPacket() {

    override fun encodeContent(out: PacketEncoder) {}
    override fun decodeContent(`in`: PacketDecoder) {}
}