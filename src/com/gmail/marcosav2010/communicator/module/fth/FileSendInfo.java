package com.gmail.marcosav2010.communicator.module.fth;

import java.nio.file.Path;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FileSendInfo {

	@Getter
	private final Path path;
	@Getter
	private final int size;
	@Getter
	private final int blocks;
	@Getter
	private final int blockSize;
	@Getter
	private int fileID = -1;

	public void setFileID(int fileID) {
		if (this.fileID == -1)
			this.fileID = fileID;
		else
			throw new IllegalStateException("ID is already set.");
	}

	public String getFileName() {
		return path.getFileName().toString();
	}
}
