package com.gmail.marcosav2010.communicator.module.fth;

import java.nio.file.Path;

public class FileSendInfo {

	private int size;
	private Path path;
	private int blocks;
	private int blockSize;
	private int id = -1;

	FileSendInfo(Path path, int size, int blocks, int blockSize) {
		this.size = size;
		this.path = path;
		this.blocks = blocks;
		this.blockSize = blockSize;
	}

	public int getID() {
		return id;
	}

	public void setID(int id) {
		if (this.id == -1)
			this.id = id;
		else
			throw new IllegalStateException("ID is already set.");
	}

	public int getSize() {
		return size;
	}

	public Path getPath() {
		return path;
	}

	public int getBlocks() {
		return blocks;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public String getFileName() {
		return path.getFileName().toString();
	}
}
