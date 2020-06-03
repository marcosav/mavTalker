package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.config.IConfiguration;
import com.gmail.marcosav2010.config.Properties;
import com.gmail.marcosav2010.config.PropertyCategory;
import com.gmail.marcosav2010.handshake.HandshakeAuthentificator.HandshakeRequirementLevel;

public class PeerProperties extends Properties {

    public PeerProperties(IConfiguration configuration) {
        super(PropertyCategory.PEER, configuration);
    }

    public HandshakeRequirementLevel getHRL() {
        return super.get(GeneralConfiguration.HANDSHAKE_REQUIREMENT_LEVEL);
    }

    public void setHRL(HandshakeRequirementLevel level) {
        super.set(GeneralConfiguration.HANDSHAKE_REQUIREMENT_LEVEL, level);
    }
}