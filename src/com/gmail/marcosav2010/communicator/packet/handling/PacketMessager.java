package com.gmail.marcosav2010.communicator.packet.handling;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.gmail.marcosav2010.cipher.CipheredCommunicator;
import com.gmail.marcosav2010.communicator.packet.AbstractPacket;
import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.StandardPacket;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketEventHandlerManager;
import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify;
import com.gmail.marcosav2010.communicator.packet.packets.PacketRespose;
import com.gmail.marcosav2010.communicator.packet.packets.PacketShutdown;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWritter;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

/**
 * This class handles a @Connection @IPacket traffic.
 * 
 * @author Marcos
 *
 */
public class PacketMessager {

	private static final String LOGGER_PREFIX = "[PM] ";
	
	private AtomicInteger lastPacket;

	private final Connection connection;
	private final CipheredCommunicator communicator;

	private final PacketEventHandlerManager eventHandlerManager;
	private final PacketWritter writter;
	private final PacketActionHandler actionHandler;

	public PacketMessager(Connection connection, CipheredCommunicator communicator) {
		this.communicator = communicator;
		this.connection = connection;
		
		writter = new PacketWritter();
		eventHandlerManager = new PacketEventHandlerManager(connection);
		actionHandler = new PacketActionHandler(connection);
		lastPacket = new AtomicInteger();
	}

	public void onReceive(AbstractPacket p) {
		if (p instanceof StandardPacket)
			handleStandardPacket((StandardPacket) p);
		else
			handlePacket((Packet) p);
	}

	private void handleStandardPacket(StandardPacket sp) {
		if (sp instanceof PacketRespose) {
			PacketRespose pr = (PacketRespose) sp;
			long id = pr.getResposePacketId();
			
			actionHandler.handleRespose(id);
			log("Sucessfully sent packet #" + id + ".", VerboseLevel.HIGH);

		} else if (sp instanceof PacketIdentify) {
			PacketIdentify pi = (PacketIdentify) sp;

			connection.getIdentificationController().identifyConnection(pi);

		} else if (sp instanceof PacketShutdown) {
			connection.disconnect(true);
			
		}
	}

	private void handlePacket(Packet packet) {
		int id = packet.getID();
		log("Received packet #" + id + ".", VerboseLevel.HIGH);
		eventHandlerManager.handlePacket(packet, connection.getConnectedPeer());
		
		if (packet.shouldSendRespose())
			try {
				sendStandardPacket(new PacketRespose(id));
			} catch (PacketWriteException ex) {
				log("There was an exception sending receive respose in packet #" + id + ".");
				Logger.log(ex);
			}
	}

	public int sendPacket(Packet packet, Runnable action, Runnable onTimeOut, long timeout, TimeUnit timeUnit) throws PacketWriteException {
		int id = lastPacket.incrementAndGet();
		log("Sending packet #" + id + ".", VerboseLevel.HIGH);
		communicator.write(writter.write(packet.setID(id)));
		
		if (action != null)
			actionHandler.handleSend(connection.getPeer(), id, packet, action, onTimeOut, timeout, timeUnit);

		return lastPacket.get();
	}

	public void sendStandardPacket(StandardPacket packet) throws PacketWriteException {
		communicator.write(writter.write(packet));
	}

	public void setupEventHandler() {
		log("Registering packet handlers...", VerboseLevel.MEDIUM);

		eventHandlerManager.registerListeners(connection.getModuleManager().getListeners());

		log("Registered " + eventHandlerManager.getHandlerCount() + " handlers in " + eventHandlerManager.getListenerCount() + " listeners.", VerboseLevel.LOW);
	}

	public void stopEventHandler() {
		eventHandlerManager.unregisterEvents();
	}

	public void log(String str) {
		connection.log(LOGGER_PREFIX + str);
	}

	public void log(String str, VerboseLevel level) {
		connection.log(LOGGER_PREFIX + str, level);
	}
}
