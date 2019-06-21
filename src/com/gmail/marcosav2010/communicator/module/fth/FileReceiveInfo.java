package com.gmail.marcosav2010.communicator.module.fth;

public class FileReceiveInfo {

	private String filename;
	private int blocks;
	private long firstArrivalTime = -1, lastArrivalTime = -1;

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

	long getFirstArrivalTime() {
		return firstArrivalTime;
	}

	synchronized void setFirstArrivalTime() {
		if (firstArrivalTime == -1)
			lastArrivalTime = firstArrivalTime = System.currentTimeMillis();
	}

	synchronized boolean updateLastArrivalTime(long req) {
		long current = System.currentTimeMillis();
		long diff = current - lastArrivalTime;
		if (diff >= req) {
			lastArrivalTime = current;
			return true;

		} else
			return false;
	}
}
