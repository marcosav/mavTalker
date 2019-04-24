package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.IOException;

public class PacketReadException extends IOException {
    
	private static final long serialVersionUID = 4737890216554456161L;

	public PacketReadException(Exception exception) {
        super(exception);
    }
	
	public PacketReadException(String msg) {
        super(msg);
    }
}
