package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketPing extends Packet {

	public PacketPing() {
	}

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
	}
}