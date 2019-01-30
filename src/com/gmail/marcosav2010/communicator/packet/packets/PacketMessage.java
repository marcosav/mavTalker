package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketMessage extends Packet {
    
    private String message;
    
    public PacketMessage() {
	}
    
    public PacketMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
    	out.write(message);
    }
    
    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
    	message = in.readString();
    }
}
