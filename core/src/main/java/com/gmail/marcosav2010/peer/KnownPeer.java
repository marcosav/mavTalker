package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.logger.Loggable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Represents the base class of a known peer, whose name and host port are
 * known, it can be a @Peer or a @ConnectedPeer.
 *
 * @author Marcos
 */
@AllArgsConstructor
public abstract class KnownPeer implements NetworkPeer, Loggable {

    @Getter
    private final String name;
    @Getter
    private final int port;
    @Getter
    private final UUID UUID;
    @Getter
    private InetAddress address;

    public KnownPeer(String name, int port, UUID uuid) {
        this(name, port, uuid, null);

        try {
            this.address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            getLog().log(e);
        }
    }

    public String getDisplayID() {
        return Utils.toBase64(UUID);
    }
}
