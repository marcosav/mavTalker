package com.gmail.marcosav2010.module.fth.packet;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
@NoArgsConstructor
public class PacketFileAccept extends Packet {

    @Getter
    private int fileID;

    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
        out.write(fileID);
    }

    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
        fileID = in.readInt();
    }
}
