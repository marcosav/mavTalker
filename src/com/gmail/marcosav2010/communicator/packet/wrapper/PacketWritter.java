package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.AbstractPacket;
import com.gmail.marcosav2010.communicator.packet.PacketRegistry;
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.OverExceededByteLimitException;
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException;

public class PacketWritter {

	public byte[] write(AbstractPacket packet) throws PacketWriteException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(); PacketEncoder encoder = new PacketEncoder(out)) {

			Byte packetType = PacketRegistry.getByClass(packet.getClass());
			if (packetType == null)
				throw new PacketWriteException("Packet type " + packet.getClass().getSimpleName() + " not recognized");

			encoder.write(packetType);
			packet.encode(encoder);

			return out.toByteArray();

		} catch (PacketWriteException ex) {
			throw ex;
		} catch (IOException | OverExceededByteLimitException ex) {
			throw new PacketWriteException(ex);
		}
	}
}
