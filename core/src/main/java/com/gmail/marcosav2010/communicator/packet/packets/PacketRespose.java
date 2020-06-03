package com.gmail.marcosav2010.communicator.packet.packets;

import com.gmail.marcosav2010.communicator.packet.StandardPacket;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
@NoArgsConstructor
public class PacketRespose extends StandardPacket {

    @Getter
    private long resposePacketId;

    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
        out.write(resposePacketId);
    }

    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
        resposePacketId = in.readLong();
    }
}
