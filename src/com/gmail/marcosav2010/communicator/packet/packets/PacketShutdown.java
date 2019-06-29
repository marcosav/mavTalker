package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.StandardPacket;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PacketShutdown extends StandardPacket {
    
	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
	}
}
