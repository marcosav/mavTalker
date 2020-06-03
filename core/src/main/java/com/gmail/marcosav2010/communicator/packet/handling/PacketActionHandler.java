package com.gmail.marcosav2010.communicator.packet.handling;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.tasker.TaskOwner;
import com.gmail.marcosav2010.tasker.Tasker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the action is done when a packet is received by remote
 * peer.
 *
 * @author Marcos
 */
public class PacketActionHandler {

    private static final long MAX_TIMEOUT = 10L;

    private final ILog log;

    private final Map<Long, PacketAction> pendingActions = new ConcurrentHashMap<>();

    public PacketActionHandler(Connection connection) {
        log = new Log(connection, "PacketActionHandler");
    }

    public boolean isPending(long id) {
        return pendingActions.containsKey(id);
    }

    public void handleRespose(long id) {
        if (!isPending(id))
            return;

        PacketAction pa = pendingActions.remove(id);

        try {
            pa.onReceive();
        } catch (Exception e) {
            log.log(e, "There was an error while handling action:\n\tID: " + id + "\n\tPacket: "
                    + pa.getType().getName() + "\n\tStacktrace: ");
        }
    }

    public void handleSend(TaskOwner owner, long id, Packet packet, Runnable action, Runnable onTimeOut,
                           long expireTimeout, TimeUnit timeUnit) {
        PacketAction pa = new PacketAction(action, onTimeOut);
        pa.setType(packet.getClass());
        pendingActions.put(id, pa);
        if (timeUnit == null || expireTimeout < 0) {
            expireTimeout = MAX_TIMEOUT;
            timeUnit = TimeUnit.SECONDS;
        }

        Tasker.getInstance().schedule(owner, () -> onExpire(id), expireTimeout, timeUnit);
    }

    public void onExpire(long id) {
        if (!isPending(id))
            return;

        PacketAction pa = pendingActions.remove(id);

        try {
            pa.onTimeOut();
        } catch (Exception e) {
            log.log(e, "There was an error while handling time out action:\n\tID: " + id + "\n\tPacket: "
                    + pa.getType().getName() + "\n\tStacktrace: ");
        }
    }
}