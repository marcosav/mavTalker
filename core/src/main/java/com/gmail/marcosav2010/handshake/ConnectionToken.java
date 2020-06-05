package com.gmail.marcosav2010.handshake;

import com.gmail.marcosav2010.common.Utils;
import lombok.Getter;

import java.net.InetSocketAddress;

public class ConnectionToken {

    @Getter
    private final byte[] handshakeKey;
    private final String handshakeKeyStr;
    @Getter
    private byte[] baseKey;
    @Getter
    private InetSocketAddress address;
    @Getter
    private boolean isPublic = false;

    public ConnectionToken(byte[] handshakeKey) {
        this.handshakeKey = handshakeKey;
        handshakeKeyStr = Utils.encode(handshakeKey);
    }

    public ConnectionToken(byte[] handshakeKey, byte[] baseKey) {
        this(handshakeKey);
        this.baseKey = baseKey;
    }

    public ConnectionToken(byte[] handshakeKey, InetSocketAddress address, byte[] baseKey) {
        this(handshakeKey, baseKey);
        this.address = address;
    }

    public String getHandshakeKeyAsString() {
        return handshakeKeyStr;
    }

    public ConnectionToken setPublic(boolean b) {
        isPublic = b;
        return this;
    }
}