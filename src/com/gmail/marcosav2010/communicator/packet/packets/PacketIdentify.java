package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;
import java.util.UUID;

import com.gmail.marcosav2010.communicator.packet.StandardPacket;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class PacketIdentify extends StandardPacket {

	public static final byte SUCCESS = 0;
	public static final byte INVALID_UUID = 1;
	public static final byte TIMED_OUT = 2;
	
	@Getter
	private String name;
	@Getter
	private UUID newUUID, peerUUID;
	@Getter
	private byte result;

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
