package com.gmail.marcosav2010.communicator.packet;

import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify;
import com.gmail.marcosav2010.communicator.packet.packets.PacketPing;
import com.gmail.marcosav2010.communicator.packet.packets.PacketRespose;
import com.gmail.marcosav2010.communicator.packet.packets.PacketShutdown;

import java.util.HashMap;
import java.util.Map;

public class PacketRegistry {

    private static final Map<Byte, Class<? extends AbstractPacket>> packetsById;
    private static final Map<Class<? extends AbstractPacket>, Byte> packetsByClass;

    static {
        packetsById = new HashMap<>();
        packetsByClass = new HashMap<>();

        register((byte) 0, PacketIdentify.class);
        register((byte) 1, PacketRespose.class);
        register((byte) 2, PacketShutdown.class);

        register((byte) -1, PacketPing.class);
    }

    public static void register(byte id, Class<? extends AbstractPacket> packet) {
        if (packetsById.containsKey(id) || packetsByClass.containsKey(packet))
            throw new IllegalArgumentException("Duplicated id or packet type");

        packetsById.put(id, packet);
        packetsByClass.put(packet, id);
    }

    public static Class<? extends AbstractPacket> getById(byte id) {
        return packetsById.get(id);
    }

    public static Byte getByClass(Class<? extends AbstractPacket> clazz) {
        return packetsByClass.get(clazz);
    }
}