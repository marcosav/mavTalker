package com.gmail.marcosav2010.communicator.module.tm;

import com.gmail.marcosav2010.communicator.module.Module;
import com.gmail.marcosav2010.communicator.module.ModuleDescriptor;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketEventHandler;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

@ModuleDescriptor(name = "TextMessager", scope = Peer.class, registry = TMCommandRegistry.class)
public class TextMessagerModule extends Module implements PacketListener {

	static {
		registerPacket(7, PacketMessage.class);
	}

	public TextMessagerModule(ModuleDescriptor moduleDescriptor) {
		super(moduleDescriptor);

		registerListeners(this);
	}

	@PacketEventHandler
	public void onPacketMessage(PacketMessage pm, ConnectedPeer peer) {
		getLog().log("Message: \"" + pm.getMessage() + "\"");
	}
}
