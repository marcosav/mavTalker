package com.gmail.marcosav2010.communicator.module.tm;

import com.gmail.marcosav2010.communicator.module.LoadModule;
import com.gmail.marcosav2010.communicator.module.Module;
import com.gmail.marcosav2010.communicator.module.ModuleManager;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketEventHandler;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.peer.ConnectedPeer;

@LoadModule(
		registry = TMCommandRegistry.class
)
public class TextMessagerModule extends Module implements PacketListener {
	
	static {
		registerPacket(7, PacketMessage.class);
	}
	
	public TextMessagerModule(ModuleManager manager) {
		super(manager, "TextMessager");
		
		registerListeners(this);
	}
	
	@PacketEventHandler
	public void onPacketMessage(PacketMessage pm, ConnectedPeer peer) {
		Logger.log("Message: \"" + pm.getMessage() + "\"");
	}
}
