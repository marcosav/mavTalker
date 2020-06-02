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
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;

/**
 * This class handles a @Connection @IPacket traffic.
 * 
 * @author Marcos
 *
 */
public class PacketMessager {

	private final ILog log;

	private AtomicInteger lastPacket;

	private final Connection connection;
	private final CipheredCommunicator communicator;

	private final PacketEventHandlerManager eventHandlerManager;
	private final PacketWritter writter;
	private final PacketActionHandler actionHandler;

	public PacketMessager(Connection connection, CipheredCommunicator communicator) {
		this.communicator = communicator;
		this.connection = connection;

		log = new Log(connection, "PM");
		writter = new PacketWritter();
		eventHandlerManager = new PacketEventHandlerManager(connection);
		actionHandler = new PacketActionHandler(connection);
		lastPacket = new AtomicInteger();
	}

	public void onReceive(AbstractPacket p) {
		if (p.isStandard())
			handleStandardPacket(p);
		else
			handlePacket((Packet) p);
	}

	private void handleStandardPacket(AbstractPacket sp) {
		if (sp instanceof PacketRespose) {
			PacketRespose pr = (PacketRespose) sp;
			long id = pr.getResposePacketId();

			actionHandler.handleRespose(id);
			log.log("Sucessfully sent packet #" + id + ".", VerboseLevel.HIGH);

		} else if (sp instanceof PacketIdentify) {
			PacketIdentify pi = (PacketIdentify) sp;

			connection.getIdentificationController().identifyConnection(pi);

		} else if (sp instanceof PacketShutdown) {
			connection.disconnect(true);

		}
	}

	private void handlePacket(Packet packet) {
		int id = packet.getPacketID();
		log.log("Received packet #" + id + ".", VerboseLevel.HIGH);
		eventHandlerManager.handlePacket(packet);

		if (packet.shouldSendRespose())
			try {
				sendStandardPacket(new PacketRespose(id));
			} catch (PacketWriteException ex) {
				log.log(ex, "There was an exception sending receive respose in packet #" + id + ".");
			}
	}

	public int sendPacket(Packet packet, Runnable action, Runnable onTimeOut, long timeout, TimeUnit timeUnit)
			throws PacketWriteException {
		int id = lastPacket.incrementAndGet();
		log.log("Sending packet #" + id + ".", VerboseLevel.HIGH);
		communicator.write(writter.write(packet.setPacketID(id)));

		if (action != null)
			actionHandler.handleSend(connection.getPeer(), id, packet, action, onTimeOut, timeout, timeUnit);

		return lastPacket.get();
	}

	public void sendStandardPacket(StandardPacket packet) throws PacketWriteException {
		communicator.write(writter.write(packet));
	}

	public void setupEventHandler() {
		log.log("Registering packet handlers...", VerboseLevel.MEDIUM);

		// TODO: Mejorar esta mierda
		eventHandlerManager.registerListeners(connection.getModuleManager().getListeners());
		eventHandlerManager.registerListeners(connection.getPeer().getModuleManager().getListeners());
		eventHandlerManager.registerListeners(Main.getInstance().getModuleManager().getListeners());

		log.log("Registered " + eventHandlerManager.getHandlerCount() + " handlers in "
				+ eventHandlerManager.getListenerCount() + " listeners.", VerboseLevel.LOW);
	}

	public void stopEventHandler() {
		eventHandlerManager.unregisterEvents();
	}
}
