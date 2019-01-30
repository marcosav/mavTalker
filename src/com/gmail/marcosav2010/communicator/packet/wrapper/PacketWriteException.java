package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.IOException;

public class PacketWriteException extends IOException {
    
	private static final long serialVersionUID = 3252190216554456161L;

	public PacketWriteException(Exception exception) {
        super(exception);
    }
}
