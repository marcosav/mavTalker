package com.gmail.marcosav2010.communicator.packet.handling;

import com.gmail.marcosav2010.communicator.packet.Packet;

public abstract class PacketAction {
	
	private Class<? extends Packet> type;
	
	void setType(Class<? extends Packet> type) {
		this.type = type;
	}
	
	Class<? extends Packet> getType() {
		return type;
	}
	
	public abstract void onReceive() throws Exception;
}
