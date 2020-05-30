package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.ByteArrayInputStream;

import com.gmail.marcosav2010.communicator.packet.AbstractPacket;
import com.gmail.marcosav2010.communicator.packet.PacketRegistry;

public class PacketReader {

	public AbstractPacket read(byte[] bytes) throws PacketReadException {
		AbstractPacket packet = null;

		try (ByteArrayInputStream in = new ByteArrayInputStream(bytes); PacketDecoder decoder = new PacketDecoder(in)) {

			byte packetType = decoder.readByte();

			Class<? extends AbstractPacket> packetClass = PacketRegistry.getById(packetType);
			if (packetClass == null)
				throw new PacketReadException("Packet type not recognized");

			packet = packetClass.getConstructor().newInstance();

			packet.decode(decoder);

		} catch (Exception ex) {
			throw new PacketReadException(ex);
		}

		return packet;
	}
}
