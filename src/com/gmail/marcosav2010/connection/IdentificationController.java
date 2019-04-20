package com.gmail.marcosav2010.connection;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.packet.handling.PacketMessager;
import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.tasker.Task;

public class IdentificationController {

	private static final long IDENTIFICATION_TIMEOUT = 5L;
	
	private Connection connection;
	private PacketMessager messager;

	private boolean uuidProvider;

	private UUID connectionUUID;

	private Task idTask;
	
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
		ConnectionManager cManager = connection.getPeer().getConnectionManager();
		if (!cManager.getIdentificator().hasPeer(uuid)) { // se supone que si ya está usada no deja, pero en una UUID es algo
																								// dificil :(
			connectionUUID = uuid;
			
			return true;
		}
		return false;
	}

	public void startIdentification() throws PacketWriteException {
		if (uuidProvider) {
			log("Generating new connection UUID and sending identification packet...", VerboseLevel.MEDIUM);
			
			ConnectionManager cManager = connection.getPeer().getConnectionManager();
			cManager.removeConnection(connectionUUID);
			
			UUID newUUID = UUID.randomUUID();
			while (!setUUID(newUUID))
				newUUID = UUID.randomUUID();
			
			cManager.registerConnection(connection);
			
			messager.sendStandardPacket(new PacketIdentify(connection.getPeer().getName(), newUUID, PacketIdentify.SUCCESS));
		} else {
			log("Waiting for UUID renewal and identification, timeout set to " + IDENTIFICATION_TIMEOUT + "s...", VerboseLevel.MEDIUM);
			startIdentificationCountdown();
		}
	}
	
	private void startIdentificationCountdown() {
		idTask = Main.getInstance().getTasker().schedule(connection.getPeer(), () -> {
			log("Identification failure, remote peer didn't send identification at time.");
			try {
				sendIdentifyRespose(PacketIdentify.TIMED_OUT);
			} catch (PacketWriteException e) {
			}
			connection.disconnect(true);
		}, IDENTIFICATION_TIMEOUT, TimeUnit.SECONDS);
	}

	public ConnectedPeer identifyConnection(PacketIdentify info) throws PacketWriteException {
		ConnectionManager cManager = connection.getPeer().getConnectionManager();
		
		if (info.providesUUID()) {
			idTask.cancel();
			
			log("Received identification from peer \"" + info.getName() + "\".", VerboseLevel.HIGH);
			
			log("Updating new remote connection UUID and setting info...", VerboseLevel.MEDIUM);
			cManager.removeConnection(getUUID());
			UUID newUUID = info.getNewUUID();

			if (!setUUID(newUUID)) {
				log("Identification failure, UUID could not be renewed because it was already identified, sending respose...", VerboseLevel.LOW);
				sendIdentifyRespose(PacketIdentify.INVALID_UUID);
				connection.disconnect(true);
				return null;
			}

			cManager.registerConnection(connection);

			log("Identification successfully, sending respose...", VerboseLevel.MEDIUM);
			sendIdentifyRespose(PacketIdentify.SUCCESS);

		} else {
			log("Received identification respose from peer \"" + info.getName() + "\".", VerboseLevel.HIGH);
			
			switch (info.getResult()) {
				case PacketIdentify.SUCCESS:
					log("Setting remote peer info...", VerboseLevel.MEDIUM);
					break;
					
				case PacketIdentify.INVALID_UUID:
					log("There was an error identifying peer, invalid UUID (try again), aborting.", VerboseLevel.MEDIUM);
					connection.disconnect(true);
					return null;
					
				case PacketIdentify.TIMED_OUT:
					log("Identification timed out, aborting.", VerboseLevel.MEDIUM);
					connection.disconnect(true);
					return null;
			}
		}
		ConnectedPeer cp = cManager.getIdentificator().identifyConnection(connection, info);
		connection.setConnectedPeer(cp);
		log("Identification completed, peers paired successfully.");
		connection.onPairCompleted();
		return cp;
	}
	
	private void sendIdentifyRespose(byte result) throws PacketWriteException {
		messager.sendStandardPacket(new PacketIdentify(connection.getPeer().getName(), null, result));
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
