package com.gmail.marcosav2010.communicator.module.fth;

public class FileReceiveInfo {

	private String filename;
	private int blocks;
	
	FileReceiveInfo(String filename, int blocks) {
		this.filename = filename;
		this.blocks = blocks;
	}
	
	public String getFileName() {
		return filename;
	}
	
	public int getBlocks() {
		return blocks;
	}
	
	public boolean isSingle() {
		return blocks == 1;
	}
}
