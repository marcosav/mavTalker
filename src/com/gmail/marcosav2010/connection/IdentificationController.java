package com.gmail.marcosav2010.connection;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.packet.handling.PacketMessager;
import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify;
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.tasker.Task;
import com.gmail.marcosav2010.tasker.Tasker;

import lombok.AccessLevel;
import lombok.Setter;

public class IdentificationController {

	private static final long IDENTIFICATION_TIMEOUT = 10L;

	private final ILog log;

	private Connection connection;
	@Setter(AccessLevel.PROTECTED)
	private PacketMessager messager;
	private ConnectionManager cManager;

	private boolean uuidProvider;

	private UUID connectionUUID;

	private Task idTask;

	public IdentificationController(Connection connection) {
		this.connection = connection;
		log = new Log(connection, "IC");
		uuidProvider = false;
		cManager = connection.getPeer().getConnectionManager();
	}

	public IdentificationController(Connection connection, UUID uuid) {
		this(connection);
		connectionUUID = uuid;
	}

	public void sendTemporaryUUID() throws IOException {
		if (hasUUID()) {
			log.log("Providing temporary connection UUID...", VerboseLevel.MEDIUM);

			connection.writeRawBytes(Utils.getBytesFromUUID(connectionUUID));

		} else {
			log.log("Generating and providing temporary connection UUID...", VerboseLevel.MEDIUM);

			uuidProvider = true;
			UUID uuid = UUID.randomUUID();
			setUUID(uuid);
			connection.writeRawBytes(Utils.getBytesFromUUID(uuid));
		}
	}

	public boolean setUUID(UUID uuid) {
		if (!cManager.getIdentificator().hasPeer(uuid)) { // se supone que si ya est� usada no deja, pero en una UUID es
															// algo dificil :(
			connectionUUID = uuid;

			return true;
		}
		return false;
	}

	public void startIdentification() throws PacketWriteException {
		if (uuidProvider) {
			log.log("Generating new connection UUID and sending identification packet...", VerboseLevel.MEDIUM);

			cManager.removeConnection(connectionUUID);

			UUID newUUID;
			do {
				newUUID = UUID.randomUUID();
			} while (!setUUID(newUUID));

			cManager.registerConnection(connection);

			messager.sendStandardPacket(new PacketIdentify(connection.getPeer().getName(), newUUID,
					connection.getPeer().getUUID(), PacketIdentify.SUCCESS));
		} else {
			log.log("Waiting for UUID renewal and identification, timeout set to " + IDENTIFICATION_TIMEOUT + "s...",
					VerboseLevel.MEDIUM);
			startIdentificationCountdown();
		}
	}

	private void startIdentificationCountdown() {
		idTask = Tasker.getInstance().schedule(connection.getPeer(), () -> {
			log.log("Identification failure, remote peer didn't send identification at time.");
			try {
				sendIdentifyRespose(PacketIdentify.TIMED_OUT);
			} catch (PacketWriteException e) {
			}
			connection.disconnect(true);
		}, IDENTIFICATION_TIMEOUT, TimeUnit.SECONDS);
	}

	public ConnectedPeer identifyConnection(PacketIdentify info) {
		if (info.providesUUID()) {
			idTask.cancel();

			log.log("Received identification from peer \"" + info.getName() + "\".", VerboseLevel.HIGH);

			log.log("Updating new remote connection UUID and setting info...", VerboseLevel.MEDIUM);
			cManager.removeConnection(getUUID());
			UUID newUUID = info.getNewUUID();

			if (!setUUID(newUUID)) {
				log.log("Identification failure, UUID could not be renewed because it was already identified, sending respose and disconnecting...",
						VerboseLevel.LOW);
				try {
					sendIdentifyRespose(PacketIdentify.INVALID_UUID);
				} catch (PacketWriteException e) {
					log.log("There was an exception sending identification respose, disconnecting anyway.");
				}
				connection.disconnect(true);
				return null;
			}

			cManager.registerConnection(connection);

			log.log("Identification successfully, sending respose...", VerboseLevel.MEDIUM);

			try {
				sendIdentifyRespose(PacketIdentify.SUCCESS);
			} catch (PacketWriteException e) {
				log.log(e, "There was an exception sending identification respose.");
			}

		} else {
			log.log("Received identification respose from peer \"" + info.getName() + "\".", VerboseLevel.HIGH);

			switch (info.getResult()) {
				case PacketIdentify.SUCCESS:
					log.log("Setting remote peer info...", VerboseLevel.MEDIUM);
					break;

				case PacketIdentify.INVALID_UUID:
					log.log("There was an error identifying peer, invalid UUID (try again), aborting.",
							VerboseLevel.MEDIUM);
					connection.disconnect(true);
					return null;

				case PacketIdentify.TIMED_OUT:
					log.log("Identification timed out, aborting.", VerboseLevel.MEDIUM);
					connection.disconnect(true);
					return null;
			}
		}

		ConnectedPeer cp = cManager.getIdentificator().identifyConnection(connection, info);
		connection.setConnectedPeer(cp);
		log.log("Identification completed, peers paired successfully.", VerboseLevel.MEDIUM);
		connection.onPairCompleted();

		return cp;
	}

	private void sendIdentifyRespose(byte result) throws PacketWriteException {
		messager.sendStandardPacket(
				new PacketIdentify(connection.getPeer().getName(), null, connection.getPeer().getUUID(), result));
	}

	private boolean hasUUID() {
		return connectionUUID != null;
	}

	public UUID getUUID() {
		return connectionUUID;
	}
}
