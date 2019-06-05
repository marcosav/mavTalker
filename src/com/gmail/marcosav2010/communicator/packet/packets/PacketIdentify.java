package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;
import java.util.UUID;

import com.gmail.marcosav2010.communicator.packet.StandardPacket;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketIdentify extends StandardPacket {

	public static final byte SUCCESS = 0;
	public static final byte INVALID_UUID = 1;
	public static final byte TIMED_OUT = 2;
	
	private String name;
	private UUID newUUID, peerUUID;
	private byte result;

	public PacketIdentify() {
	}

	public PacketIdentify(String name, UUID newUUID, UUID peerUUID, byte result) {
		this.name = name;
		this.newUUID = newUUID;
		this.peerUUID = peerUUID;
		this.result = result;
	}

	public String getName() {
		return name;
	}

	public UUID getPeerUUID() {
		return peerUUID;
	}
	
	public UUID getNewUUID() {
		return newUUID;
	}
	
	public byte getResult() {
		return result;
	}

	public boolean providesUUID() {
		return newUUID != null;
	}
	
	public boolean providesName() {
		return name != null && !name.isBlank();
	}

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
		out.write(providesName());
		if (providesName())
			out.write(name);
		out.write(providesUUID());
		if (providesUUID())
			out.write(newUUID);
		out.write(peerUUID);
		out.write(result);
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
		if (in.readBoolean())
			name = in.readString();
		if (in.readBoolean())
			newUUID = in.readUUID();
		peerUUID = in.readUUID();
		result = in.readByte();
	}
}
