package com.gmail.marcosav2010.communicator.packet;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import lombok.Getter;

public abstract class Packet extends AbstractPacket {

	public static int MAX_SIZE = AbstractPacket.MAX_SIZE - Integer.BYTES;

	@Getter
	private int packetID = 0;

	public Packet setPacketID(int packetID) {
		if (hasPacketID())
			throw new RuntimeException("This packet already has an ID set");

		this.packetID = packetID;
		return this;
	}

	private boolean hasPacketID() {
		return packetID != 0;
	}

	public boolean shouldSendRespose() {
		return true;
	}

	@Override
	public boolean isStandard() {
		return false;
	}

	@Override
	public void encode(PacketEncoder out) throws IOException {
		if (!hasPacketID())
			throw new RuntimeException("Cannot encode without ID set");

		out.write(packetID);
		encodeContent(out);
	}

	protected abstract void encodeContent(PacketEncoder out) throws IOException;

	@Override
	public void decode(PacketDecoder in) throws IOException {
		packetID = in.readInt();
		decodeContent(in);
	}

	protected abstract void decodeContent(PacketDecoder in) throws IOException;
}
