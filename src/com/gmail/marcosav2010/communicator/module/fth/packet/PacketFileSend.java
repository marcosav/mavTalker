package com.gmail.marcosav2010.communicator.module.fth.packet;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.module.fth.FileTransferHandler;
import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketFileSend extends Packet {

	/**
	 * Data fields + byte arrays + array length
	 */
	public static final int MAX_BLOCK_SIZE = Packet.MAX_SIZE - 2 * Integer.BYTES - FileTransferHandler.HASH_SIZE - 2 * Integer.BYTES;

	private int fileId;
	private int pointer;
	private byte[] file;
	private byte[] hash;

	public PacketFileSend() {
	}

	public PacketFileSend(int fileId, int pointer, byte[] file, byte[] hash) {
		this.fileId = fileId;
		this.pointer = pointer;
		if (file.length > MAX_BLOCK_SIZE)
			throw new IllegalArgumentException("Byte block size cannot exceed " + MAX_BLOCK_SIZE + " bytes");
		if (hash.length > FileTransferHandler.HASH_SIZE)
			throw new IllegalArgumentException("Hash size cannot exceed " + FileTransferHandler.HASH_SIZE + " bytes");
		this.file = file;
		this.hash = hash;
	}

	public int getFileID() {
		return fileId;
	}

	public int getPointer() {
		return pointer;
	}

	public byte[] getBytes() {
		return file;
	}

	public byte[] getHash() {
		return hash;
	}

	@Override
	public boolean shouldSendRespose() {
		return false;
	}

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
		out.write(fileId);
		out.write(pointer);
		out.write(file);
		out.write(hash);
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
		fileId = in.readInt();
		pointer = in.readInt();
		file = in.readBytes();
		hash = in.readBytes();
	}
}
