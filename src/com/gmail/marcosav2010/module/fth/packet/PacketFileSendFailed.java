package com.gmail.marcosav2010.module.fth.packet;

import java.io.IOException;

import com.gmail.marcosav2010.module.fth.FileTransferHandler.FileSendResult;
import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class PacketFileSendFailed extends Packet {

	@Getter
	private int fileID;
	@Getter
	private FileSendResult cause;

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
		out.write(fileID);
		out.write((byte) cause.ordinal());
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
		fileID = in.readInt();
		cause = FileSendResult.values()[in.readByte()];
	}
}
