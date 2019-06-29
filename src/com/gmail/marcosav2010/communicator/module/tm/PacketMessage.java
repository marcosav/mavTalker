package com.gmail.marcosav2010.communicator.module.tm;

import java.io.IOException;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class PacketMessage extends Packet {
    
	@Getter
    private String message;
    
    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
    	out.write(message);
    }
    
    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
    	message = in.readString();
    }
}
