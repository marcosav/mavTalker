package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.handling.listener.file.FileTransferHandler.FileSendResult;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketFileSendFailed extends Packet {

	private int fileId;
	private FileSendResult cause;

	public PacketFileSendFailed() {
	}
	
	public PacketFileSendFailed(int fileId, FileSendResult cause) {
		this.fileId = fileId;
		this.cause = cause;
	}

	public int getFileId() {
		return fileId;
	}

	public FileSendResult getCause() {
		return cause;
	}

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
		out.write(fileId);
		out.write((byte) cause.ordinal());
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
		fileId = in.readInt();
		cause = FileSendResult.values()[in.readByte()];
	}
}
