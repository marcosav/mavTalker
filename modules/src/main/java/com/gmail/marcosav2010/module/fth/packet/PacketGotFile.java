package com.gmail.marcosav2010.module.fth.packet;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor
@AllArgsConstructor
public class PacketGotFile extends Packet {

    @Getter
    private String fileName;
    @Getter
    private String owner;

    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
        out.write(fileName);
        out.write(owner);
    }

    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
        fileName = in.readString();
        owner = in.readString();
    }
}
