package com.gmail.marcosav2010.communicator.packet;

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import java.io.IOException;

public abstract class StandardPacket extends AbstractPacket {

    @Override
    public boolean isStandard() {
        return true;
    }

    @Override
    public void encode(PacketEncoder out) throws IOException {
        encodeContent(out);
    }

    protected abstract void encodeContent(PacketEncoder out) throws IOException;

    @Override
    public void decode(PacketDecoder in) throws IOException {
        decodeContent(in);
    }

    protected abstract void decodeContent(PacketDecoder in) throws IOException;
}
