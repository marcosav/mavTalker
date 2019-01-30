package com.gmail.marcosav2010.communicator.packet.wrapper;

import com.gmail.marcosav2010.communicator.packet.AbstractPacket;

public class OverExceededByteLimitException extends RuntimeException {
    
	private static final long serialVersionUID = -3543010566122316736L;

	public OverExceededByteLimitException(long incoming) {
        super("Exceded max byte array length: " + AbstractPacket.MAX_SIZE + " < " + incoming + ".");
    }
}
