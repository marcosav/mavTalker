package com.gmail.marcosav2010.communicator.packet;

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import java.io.IOException;

public abstract class AbstractPacket {

    public static int BASE_SIZE = Short.MAX_VALUE * 32 * 4; // ~4 MB
    public static int MAX_SIZE = BASE_SIZE - Byte.BYTES;

    public abstract void encode(PacketEncoder out) throws IOException;

    public abstract void decode(PacketDecoder in) throws IOException;

    public abstract boolean isStandard();
}