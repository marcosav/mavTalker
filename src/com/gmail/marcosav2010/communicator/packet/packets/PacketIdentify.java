package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;
import java.util.UUID;

import com.gmail.marcosav2010.communicator.packet.StandardPacket;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketIdentify extends StandardPacket {

	private String name;
	private UUID newUUID;

	public PacketIdentify() {
	}

	public PacketIdentify(String name, UUID newUUID) {
		this.name = name;
		this.newUUID = newUUID;
	}

	public String getName() {
		return name;
	}

	public UUID getNewUUID() {
		return newUUID;
	}

	public boolean providesUUID() {
		return newUUID != null;
	}

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
		out.write(name);
		out.write(providesUUID());
		if (providesUUID())
			out.write(newUUID);
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
		name = in.readString();
		if (in.readBoolean())
			newUUID = in.readUUID();
	}
}
