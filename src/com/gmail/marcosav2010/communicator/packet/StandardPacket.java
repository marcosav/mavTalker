package com.gmail.marcosav2010.communicator.packet;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public abstract class StandardPacket extends AbstractPacket {

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
