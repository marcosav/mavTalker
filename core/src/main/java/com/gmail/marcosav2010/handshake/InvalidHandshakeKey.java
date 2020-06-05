package com.gmail.marcosav2010.handshake;

public class InvalidHandshakeKey extends Exception {

    private static final long serialVersionUID = -1705689877146623444L;

    public InvalidHandshakeKey() {
        super("Invalid handshake key");
    }
}