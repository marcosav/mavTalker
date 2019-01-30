package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.gmail.marcosav2010.communicator.packet.AbstractPacket;
import com.gmail.marcosav2010.communicator.packet.PacketRegistry;

public class PacketReader {

	public AbstractPacket read(byte[] bytes) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		AbstractPacket packet = null;

		try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); PacketDecoder decoder = new PacketDecoder(in)) {

			byte packetType = decoder.readByte();

			Class<? extends AbstractPacket> packetClass = PacketRegistry.getById(packetType);
			if (packetClass == null)
				throw new IOException("Packet type not recognized");

			packet = packetClass.getConstructor().newInstance();

			packet.decode(decoder);
		}

		return packet;
	}
}
