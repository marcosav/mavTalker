package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.AbstractPacket;
import com.gmail.marcosav2010.communicator.packet.PacketRegistry;

public class PacketWritter {

	public byte[] write(AbstractPacket packet) throws PacketWriteException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PacketEncoder encoder = new PacketEncoder(out)) {

			Byte packetType = PacketRegistry.getByClass(packet.getClass());
			if (packetType == null)
				throw new PacketWriteException("Packet type not recognized");

			encoder.write(packetType);
			packet.encode(encoder);

			return out.toByteArray();
			
		} catch (IOException | OverExceededByteLimitException ex) {
			throw new PacketWriteException(ex);
		}
	}
}
