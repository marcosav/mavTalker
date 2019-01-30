package com.gmail.marcosav2010.connection;

import java.io.IOException;
import java.util.UUID;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.packet.handling.PacketMessager;
import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.ConnectedPeer;

public class IdentificationController {

	private Connection connection;
	private PacketMessager messager;
	
	private boolean uuidProvider;

	private UUID connectionUUID;
	
	public IdentificationController(Connection connection) {
		this.connection = connection;
		uuidProvider = false;
	}
	
	public IdentificationController(Connection connection, UUID uuid) {
		this(connection);
		connectionUUID = uuid;
	}
	
	public void handleTemporaryUUID() throws IOException {
		if (hasUUID()) {
			log("Providing temporary connection UUID...", VerboseLevel.MEDIUM);

			connection.writeRawBytes(Utils.getBytesFromUUID(connectionUUID));

		} else {
			log("Generating and providing temporary connection UUID...", VerboseLevel.MEDIUM);

			uuidProvider = true;
			UUID uuid = UUID.randomUUID();
			setUUID(uuid);
			connection.writeRawBytes(Utils.getBytesFromUUID(uuid));
		}
	}
	
	public boolean setUUID(UUID uuid) {
		if (!connection.getPeer().getConnectionManager().getIdentificator().hasPeer(connectionUUID)) {
			connectionUUID = uuid;
			return true;
		}
		return false;
	}

	public void startIdentification() throws PacketWriteException {
		if (uuidProvider) {
			log("Generating new connection UUID and sending identification packet...", VerboseLevel.MEDIUM);
			UUID newUUID = UUID.randomUUID();
			setUUID(newUUID);
			messager.sendStandardPacket(new PacketIdentify(connection.getPeer().getName(), newUUID));
		}
	}

	public void respondIdentification() throws PacketWriteException {
		log("Responding to identification packet...", VerboseLevel.MEDIUM);
		messager.sendStandardPacket(new PacketIdentify(connection.getPeer().getName(), null));
	}
	
	public ConnectedPeer identifyConnection(PacketIdentify info) throws PacketWriteException {
		ConnectionManager cManager = connection.getPeer().getConnectionManager();
		if (info.providesUUID()) {
			log("Updating new remote connection UUID and setting info...", VerboseLevel.MEDIUM);
			cManager.removeConnection(getUUID());
			UUID newUUID = info.getNewUUID();
			if (!setUUID(newUUID))
				throw new ConnectionIdentificationException("UUID could not be renewed because it was already identified");
			cManager.registerConnection(connection);
			respondIdentification();

		} else {
			log("Setting remote peer info...", VerboseLevel.MEDIUM);
		}
		ConnectedPeer cp = cManager.getIdentificator().identifyConnection(connection, info);
		connection.setConnectedPeer(cp);
		connection.log("Identification completed, peers paired successfully.");
		connection.onPairCompleted();
		return cp;
	}
	
	private boolean hasUUID() {
		return connectionUUID != null;
	}
	
	public UUID getUUID() {
		return connectionUUID;
	}
	
	protected void setMessager(PacketMessager messager) {
		this.messager = messager;
	}
	
	public void log(String str) {
		connection.log("[IC] " + str);
	}
	
	public void log(String str, VerboseLevel level) {
		connection.log("[IC] " + str, level);
	}
}
