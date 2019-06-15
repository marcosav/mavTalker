package com.gmail.marcosav2010.communicator.packet.handling;

import com.gmail.marcosav2010.communicator.packet.Packet;

public class PacketAction {
	
	private Class<? extends Packet> type;
	private Runnable action, onTimeOut;
	
	public PacketAction(Runnable action, Runnable onTimeOut) {
		this.action = action;
		this.onTimeOut = onTimeOut;
	}
	
	void setType(Class<? extends Packet> type) {
		this.type = type;
	}
	
	Class<? extends Packet> getType() {
		return type;
	}
	
	public void onReceive() {
		if (action != null)
			action.run();
	}
	
	public void onTimeOut() {
		if (onTimeOut != null)
			onTimeOut.run();
	}
}
