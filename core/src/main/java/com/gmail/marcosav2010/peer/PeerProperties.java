package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.config.IConfiguration;
import com.gmail.marcosav2010.config.Properties;
import com.gmail.marcosav2010.config.PropertyCategory;
import com.gmail.marcosav2010.handshake.HandshakeAuthenticator.HandshakeRequirementLevel;

public class PeerProperties extends Properties {

    public static final String HANDSHAKE_REQUIREMENT_LEVEL = "defHandshakeRequirementLevel";

    public PeerProperties(IConfiguration configuration) {
        super(PropertyCategory.PEER, configuration);
    }

    public HandshakeRequirementLevel getHRL() {
        return super.get(HANDSHAKE_REQUIREMENT_LEVEL);
    }

    public void setHRL(HandshakeRequirementLevel level) {
        super.set(HANDSHAKE_REQUIREMENT_LEVEL, level);
    }
}