package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.NetworkIdentificator;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Represents a connected @NetworkPeer which is at the other side of
 * the @Connection (with a @Peer), acting like a node.
 *
 * @author Marcos
 */
public class ConnectedPeer extends KnownPeer {

    @Getter
    private final ILog log;
    @Getter
    private final Connection connection;
    @Getter
    private final NetworkIdentificator<NetworkPeer> networkIdentificator;

    public ConnectedPeer(String name, UUID uuid, Connection connection) {
        super(name, connection.getRemotePort(), uuid, connection.getRemoteAddress());
        log = new Log(Logger.getGlobal(), name);
        networkIdentificator = new NetworkIdentificator<>();
        this.connection = connection;
    }

    public void disconnect(boolean silent) {
        connection.disconnect(silent);
    }

    public void sendPacket(Packet packet) throws PacketWriteException {
        connection.sendPacket(packet);
    }

    public void sendPacket(Packet packet, Runnable action) throws PacketWriteException {
        connection.sendPacket(packet, action);
    }

    public void sendPacket(Packet packet, Runnable action, Runnable onTimeOut, long timeout, TimeUnit timeUnit)
            throws PacketWriteException {
        connection.sendPacket(packet, action, onTimeOut, timeout, timeUnit);
    }
}
