package com.gmail.marcosav2010.communicator.packet;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public abstract class Packet extends AbstractPacket {

	public static int MAX_SIZE = AbstractPacket.MAX_SIZE - Integer.BYTES;
	
	private int packetId = 0;

	public Packet setID(int packetId) {
		if (hasID())
			throw new RuntimeException("This packet already has an ID set");
		
		this.packetId = packetId;
		return this;
	}

	private boolean hasID() {
		return packetId != 0;
	}

	public int getID() {
		return packetId;
	}
	
	public boolean shouldSendRespose() {
		return true;
	}

	@Override
	public void encode(PacketEncoder out) throws IOException {
		if (!hasID())
			throw new RuntimeException("Cannot encode without ID set");
		
		out.write(packetId);
		encodeContent(out);
	}
	
	protected abstract void encodeContent(PacketEncoder out) throws IOException;

	@Override
	public void decode(PacketDecoder in) throws IOException {
		packetId = in.readInt();
		decodeContent(in);
	}
	
	protected abstract void decodeContent(PacketDecoder in) throws IOException;
}
