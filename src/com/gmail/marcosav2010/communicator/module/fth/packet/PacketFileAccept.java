package com.gmail.marcosav2010.communicator.module.fth.packet;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

public class PacketFileAccept extends Packet {
    
    private int fileId;
    
    public PacketFileAccept() {
	}
    
    public PacketFileAccept(int fileId) {
        this.fileId = fileId;
    }
    
    public int getFileID() {
    	return fileId;
    }
    
    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
    	out.write(fileId);
    }
    
    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
    	fileId = in.readInt();
    }
}
