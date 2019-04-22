package com.gmail.marcosav2010.communicator.packet.handling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.tasker.TaskOwner;

/**
 * This class handles the action is done when a packet is received by remote peer.
 * 
 * @author Marcos
 *
 */
public class PacketActionHandler {

	private static final long MAX_TIMEOUT = 10L;
	private static final String LOGGER_PREFIX = "[PacketActionHandler] ";

	private final Connection connection;
	private Map<Long, PacketAction> pendingActions;

	public PacketActionHandler(Connection c) {
		this.connection = c;
		pendingActions = new ConcurrentHashMap<>();
	}

	public boolean isPending(long id) {
		return pendingActions.containsKey(id);
	}

	public void handleRespose(long id) {
		if (!isPending(id))
			return;
		
		PacketAction action = pendingActions.remove(id);
		
		try {
			action.onReceive();
		} catch (Exception e) {
			log("There was an error while handling action:\n\tID: " + id + "\n\tPacket: " + action.getType().getName() + "\n\tStacktrace: ");
			Logger.log(e);
		}
	}

	public void handleSend(TaskOwner owner, long id, Packet packet, PacketAction action, long expireTimeout, TimeUnit timeUnit) {
		action.setType(packet.getClass());
		pendingActions.put(id, action);
		if (timeUnit == null || expireTimeout < 0) {
			expireTimeout = MAX_TIMEOUT;
			timeUnit = TimeUnit.SECONDS;
		}
		
		Main.getInstance().getTasker().schedule(owner, () -> onExpire(id), expireTimeout, timeUnit);
	}

	public void onExpire(long id) {
		pendingActions.remove(id);
	}
	
	public void log(String str) {
		connection.log(LOGGER_PREFIX + str);
	}

	public void log(String str, VerboseLevel level) {
		connection.log(LOGGER_PREFIX + str, level);
	}
}
