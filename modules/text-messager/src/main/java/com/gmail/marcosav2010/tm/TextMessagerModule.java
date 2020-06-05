package com.gmail.marcosav2010.tm;

import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketEventHandler;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.module.Module;
import com.gmail.marcosav2010.module.ModuleCommandRegistry;
import com.gmail.marcosav2010.module.ModuleDescriptor;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

@ModuleCommandRegistry(TMCommandRegistry.class)
@ModuleDescriptor(name = "TextMessager", scope = Peer.class, listeners = {
        TextMessagerModule.class})
public class TextMessagerModule extends Module implements PacketListener {

    static {
        registerPacket(9, PacketMessage.class);
    }

    public TextMessagerModule(ModuleDescriptor moduleDescriptor) {
        super(moduleDescriptor);
    }

    @PacketEventHandler
    public void onPacketMessage(PacketMessage pm, ConnectedPeer peer) {
        getLog().log("Message: \"" + pm.getMessage() + "\"");
    }
}
