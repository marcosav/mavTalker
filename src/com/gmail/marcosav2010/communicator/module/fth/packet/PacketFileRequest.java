package com.gmail.marcosav2010.communicator.module.fth.packet;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.module.fth.FileSendInfo;
import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PacketFileRequest extends Packet {

	@Getter
	private String name;
	@Getter
	private int size;
	@Getter
	private int blocks;
	@Getter
	private int fileID;

	public PacketFileRequest(FileSendInfo info) {
		this.name = info.getPath().getFileName().toString();
		this.size = info.getSize();
		this.blocks = info.getBlocks();
		this.fileID = info.getFileID();
	}

	public boolean isSingle() {
		return blocks == 1;
	}

	@Override
	protected void encodeContent(PacketEncoder out) throws IOException {
		out.write(name);
		out.write(size);
		out.write(blocks);
		out.write(fileID);
	}

	@Override
	protected void decodeContent(PacketDecoder in) throws IOException {
		name = in.readString();
		size = in.readInt();
		blocks = in.readInt();
		fileID = in.readInt();
	}
}
