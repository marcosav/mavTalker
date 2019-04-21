package com.gmail.marcosav2010.communicator.module.fth.packet;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.module.fth.FileSendInfo;
import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketFileRequest extends Packet {

	private String name;
	private int size;
	private int blocks;

	public PacketFileRequest() {
	}

	public PacketFileRequest(FileSendInfo info) {
		this.name = info.getPath().getFileName().toString();
		this.size = info.getSize();
		this.blocks = info.getBlocks();
	}

	public String getName() {
		return name;
	}

	public int getSize() {
		return size;
	}

	public int getBlocks() {
		return blocks;
	}

	public boolean isSingle() {
		return blocks == 1;
	}

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
		out.write(name);
		out.write(size);
		out.write(blocks);
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
		name = in.readString();
		size = in.readInt();
		blocks = in.readInt();
	}
}
