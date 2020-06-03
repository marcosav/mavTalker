package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.connection.NetworkIdentificator;

import java.util.UUID;

/**
 * Represents the base of any kind of remote peer, which can be connected or not to a @Peer,
 * resembles a node.
 *
 * @author Marcos
 */
public interface NetworkPeer {

    NetworkIdentificator<? extends NetworkPeer> getNetworkIdentificator();

    UUID getUUID();
}
