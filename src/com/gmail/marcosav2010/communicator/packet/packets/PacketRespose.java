package com.gmail.marcosav2010.communicator.packet.packets;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.StandardPacket;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketRespose extends StandardPacket {
    
    private long resposePacketId;
    
    public PacketRespose() {
	}
    
    public PacketRespose(long resposePacketId) {
        this.resposePacketId = resposePacketId;
    }
    
    public long getResposePacketId() {
        return resposePacketId;
    }
    
    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
    	out.write(resposePacketId);
    }
    
    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
    	resposePacketId = in.readLong();
    }
}
