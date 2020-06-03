package com.gmail.marcosav2010.communicator.packet.packets;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PacketPing extends Packet {

    @Override
    protected void encodeContent(PacketEncoder out) {
    }

    @Override
    protected void decodeContent(PacketDecoder in) {
    }
}
