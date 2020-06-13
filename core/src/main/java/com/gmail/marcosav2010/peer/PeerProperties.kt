package com.gmail.marcosav2010.peer

import com.gmail.marcosav2010.config.IConfiguration
import com.gmail.marcosav2010.config.Properties
import com.gmail.marcosav2010.config.PropertyCategory
import com.gmail.marcosav2010.handshake.HandshakeAuthenticator

class PeerProperties(configuration: IConfiguration) : Properties(PropertyCategory.PEER, configuration) {

    var hrl: HandshakeAuthenticator.HandshakeRequirementLevel
        get() = super.get(HANDSHAKE_REQUIREMENT_LEVEL)
        set(level) {
            super.set(HANDSHAKE_REQUIREMENT_LEVEL, level)
        }

    companion object {
        const val HANDSHAKE_REQUIREMENT_LEVEL = "defHandshakeRequirementLevel"
    }
}