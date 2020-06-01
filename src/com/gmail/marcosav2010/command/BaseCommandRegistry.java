package com.gmail.marcosav2010.command;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.gmail.marcosav2010.communicator.packet.packets.PacketPing;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.handshake.HandshakeAuthentificator.HandshakeRequirementLevel;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

public class BaseCommandRegistry extends CommandRegistry {

	public BaseCommandRegistry() {
		super(Set.of(new ExitCMD(), new VerboseCMD(), new InfoCMD(), new StopCMD(), new NewCMD(), new HelpCMD(),
				new DisconnectCMD(), new GenerateAddressCMD(), new ConnectionKeyCMD(), new PeerPropertyCMD(),
				new PingCMD()));
	}

	private static class PingCMD extends Command {

		PingCMD() {
			super("ping");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 2) {
				log.log("ERROR: Needed transmitter and target.");
				return;
			}

			String peerName = arg[0], remoteName = arg[1];
			Peer peer;

			if (Main.getInstance().getPeerManager().exists(peerName)) {
				peer = Main.getInstance().getPeerManager().get(peerName);
			} else {
				log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
				return;
			}

			ConnectionManager cManager = peer.getConnectionManager();
			ConnectionIdentificator cIdentificator = cManager.getIdentificator();

			Connection connection;

			if (!cIdentificator.hasPeer(remoteName)) {
				log.log("ERROR: " + peerName + " peer is not connected to that " + remoteName + ".");
				return;
			}

			connection = cIdentificator.getPeer(remoteName).getConnection();

			long l = System.currentTimeMillis();

			try {
				connection.sendPacket(new PacketPing(), () -> log.log((System.currentTimeMillis() - l) / 2 + "ms"),
						() -> log.log("Ping timed out."), 10L, TimeUnit.SECONDS);
			} catch (PacketWriteException e) {
				log.log(e);
			}
		}
	}

	private static class ExitCMD extends Command {

		ExitCMD() {
			super("exit", new String[] { "e" });
		}

		@Override
		public void execute(String[] arg, int args) {
			System.exit(0);
		}
	}

	private static class PeerPropertyCMD extends Command {

		PeerPropertyCMD() {
			super("peerproperty", new String[] { "pp", "pprop" }, "[peer (if one leave)] <property name> <value>");
		}

		@Override
		public void execute(String[] arg, int args) {
			int pCount = Main.getInstance().getPeerManager().count();

			if (pCount > 1 && args < 1 || pCount == 0) {
				log.log("ERROR: Specify peer, property and value.");
				return;
			}

			boolean autoPeer = pCount == 1 && (args == 2 || args == 0);

			Peer peer;

			if (!autoPeer) {
				String peerName = arg[0];

				if (Main.getInstance().getPeerManager().exists(peerName)) {
					peer = Main.getInstance().getPeerManager().get(peerName);
				} else {
					log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
					return;
				}
			} else
				peer = Main.getInstance().getPeerManager().getFirstPeer();

			var props = peer.getProperties();

			if (pCount > 1 && args < 3 || pCount <= 1 && args < 2) {
				log.log("Showing peer " + peer.getName() + " properties:");
				log.log(props.toString());
				return;
			}

			String prop = arg[autoPeer ? 0 : 1], value = arg[autoPeer ? 1 : 2];

			if (props.exist(prop)) {
				if (props.set(prop, value)) {
					log.log("Property \"" + prop + "\" set to: " + value);
					return;

				} else
					log.log("There was an error while setting the property \"" + prop + "\" in " + peer.getName()
							+ ":");

			} else
				log.log("Unrecognized property \"" + prop + "\", current properties in " + peer.getName() + ":");

			log.log(props.toString());
		}
	}

	private static class ConnectionKeyCMD extends Command {

		ConnectionKeyCMD() {
			super("connectionkey", new String[] { "ck", "ckey" }, "[peer]");
		}

		@Override
		public void execute(String[] arg, int args) {
			int pCount = Main.getInstance().getPeerManager().count();

			if (pCount > 1 && args == 0) {
				log.log("ERROR: Specify peer.");
				return;
			}

			boolean autoPeer = pCount == 1 && args == 0;

			Peer peer;

			if (!autoPeer) {
				String peerName = arg[0];

				if (Main.getInstance().getPeerManager().exists(peerName)) {
					peer = Main.getInstance().getPeerManager().get(peerName);
				} else {
					log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
					return;
				}
			} else
				peer = Main.getInstance().getPeerManager().getFirstPeer();

			String k;

			k = peer.getConnectionManager().getHandshakeAuthentificator().getConnectionKeyString();

			log.log("-----------------------------------------------------");
			log.log("\tConnection Key => " + k);
			log.log("-----------------------------------------------------");
		}
	}

	private static class GenerateAddressCMD extends Command {

		GenerateAddressCMD() {
			super("generate", new String[] { "g", "gen" }, "[peer] [-p (public)]");
		}

		@Override
		public void execute(String[] arg, int args) {
			int pCount = Main.getInstance().getPeerManager().count();

			if (pCount > 1 && args == 0) {
				log.log("ERROR: Specify peer.");
				return;
			}

			boolean generatePublic = args == 1 && arg[0].equalsIgnoreCase("-p");
			boolean autoPeer = pCount == 1 && (args == 0 || generatePublic);
			generatePublic |= args == 2 && arg[1].equalsIgnoreCase("-p");

			Peer peer;

			if (!autoPeer) {
				String peerName = arg[0];

				if (Main.getInstance().getPeerManager().exists(peerName)) {
					peer = Main.getInstance().getPeerManager().get(peerName);
				} else {
					log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
					return;
				}
			} else
				peer = Main.getInstance().getPeerManager().getFirstPeer();

			String addressKey;

			if (generatePublic) {
				if (peer.getProperties().getHRL().compareTo(HandshakeRequirementLevel.PRIVATE) >= 0) {
					log.log("Peer \"" + peer.getName()
							+ "\" does only allow private keys, you can change this in peer configuration.");
					return;

				} else
					try {
						addressKey = peer.getConnectionManager().getHandshakeAuthentificator()
								.generatePublicAddressKey();

					} catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException
							| NoSuchPaddingException | IllegalBlockSizeException e) {
						log.log("There was an error generating the public address key, " + e.getMessage() + ".");
						return;
					}

			} else {

				log.log("Enter requester Connection Key: ");

				var requesterConnectionKey = System.console().readPassword();
				if (requesterConnectionKey == null || requesterConnectionKey.length == 0) {
					log.log("Please enter a Connection Key.");
					return;
				}

				try {
					addressKey = peer.getConnectionManager().getHandshakeAuthentificator()
							.generatePrivateAddressKey(requesterConnectionKey);

				} catch (IllegalArgumentException e) {
					log.log(e.getMessage());
					return;
				} catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
						| IllegalBlockSizeException e) {
					log.log("There was an error reading the provided address key, " + e.getMessage() + ".");
					return;
				}
			}

			log.log("--------------------------------------------------------");
			log.log("\tAddress Key => " + addressKey);
			log.log("--------------------------------------------------------");
		}
	}

	private static class VerboseCMD extends Command {

		VerboseCMD() {
			super("verbose", new String[] { "v" }, "[level]");
		}

		@Override
		public void execute(String[] arg, int args) {
			VerboseLevel level;
			var levels = VerboseLevel.values();
			if (arg.length == 0) {
				level = Logger.getVerboseLevel() == levels[0] ? levels[levels.length - 1] : levels[0];
			} else {
				try {
					level = VerboseLevel.valueOf(arg[0].toUpperCase());
				} catch (IllegalArgumentException ex) {
					log.log("ERROR: Invalid verbose level, use "
							+ Stream.of(levels).map(Enum::toString).collect(Collectors.joining(", ")));
					return;
				}
			}

			Main.getInstance().getGeneralConfig().set(GeneralConfiguration.VERBOSE_LEVEL, level.name());
			Logger.setVerboseLevel(level);
		}
	}

	private static class HelpCMD extends Command {

		HelpCMD() {
			super("help");
		}

		@Override
		public void execute(String[] arg, int args) {
			log.log(">> cmd <required> [optional] (info)");
			Main.getInstance().getCommandManager().getCommands().forEach(c -> log.log(">> " + c.getUsage()));
		}
	}

	private static class InfoCMD extends Command {

		InfoCMD() {
			super("info", new String[] { "i" }, "[peer] (empty = manager)");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args == 0) {
				Main.getInstance().getPeerManager().printInfo();

			} else {
				String target = arg[0];
				if (Main.getInstance().getPeerManager().exists(target)) {
					Main.getInstance().getPeerManager().get(target).printInfo();
				}
			}
		}
	}

	private static class StopCMD extends Command {

		StopCMD() {
			super("stop", new String[] { "st", "shutdown" }, "[peer] (empty = all)");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args > 0) {
				String target = arg[0];
				Main.getInstance().getPeerManager().shutdown(target);
			} else {
				Main.getInstance().getPeerManager().shutdown();
			}
		}
	}

	private static class DisconnectCMD extends Command {

		DisconnectCMD() {
			super("disconnect", new String[] { "dis" }, "<peer> <remote peer>/<address:port> (no address = localhost)");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 2) {
				log.log("ERROR: Specify local and remote peer or address.");
				return;
			}

			Peer peer;
			String peerName = arg[0];

			if (Main.getInstance().getPeerManager().exists(peerName)) {
				peer = Main.getInstance().getPeerManager().get(peerName);
			} else {
				log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
				return;
			}

			Connection connection;

			String remoteName = arg[1];

			ConnectionManager cManager = peer.getConnectionManager();
			ConnectionIdentificator cIdentificator = cManager.getIdentificator();

			if (cIdentificator.hasPeer(remoteName))
				connection = cIdentificator.getPeer(remoteName).getConnection();
			else {
				String[] rawAddress = remoteName.split(":");
				boolean local = rawAddress.length == 1;

				int port;
				try {
					port = Integer.valueOf(rawAddress[local ? 0 : 1]);
				} catch (NumberFormatException ex) {
					log.log("ERROR: Invalid address format or peer.");
					return;
				}

				try {
					InetSocketAddress address = local ? new InetSocketAddress(InetAddress.getLocalHost(), port)
							: new InetSocketAddress(rawAddress[0], port);

					if (cManager.isConnectedTo(address))
						connection = cManager.getConnection(address);
					else {
						log.log("ERROR: " + peerName + " peer is not connected to that address.");
						return;
					}

				} catch (UnknownHostException ex) {
					log.log("ERROR: Invalid address.");
					return;
				}
			}

			connection.disconnect(false);
		}
	}

	private static class NewCMD extends Command {

		NewCMD() {
			super("new", new String[] { "create" },
					"<peer [name] [port]/connection [peer] [address:port] (no address = localhost) [-k]>");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 1) {
				log.log("ERROR: Specify entity type.");
				return;
			}
			switch (arg[0].toLowerCase()) {
				case "peer": {
					Peer peer;
					if (args >= 3) {
						int port;
						try {
							port = Integer.valueOf(arg[2]);
						} catch (NumberFormatException ex) {
							log.log("ERROR: Invalid port.");
							return;
						}
						peer = Main.getInstance().getPeerManager().create(arg[1], port);
					} else if (args >= 2) {
						peer = Main.getInstance().getPeerManager().create(arg[1]);
					} else {
						peer = Main.getInstance().getPeerManager().create();
					}

					if (peer == null)
						return;
					peer.start();
					break;
				}
				case "c":
				case "conn":
				case "connection":
					boolean autoPeer = Main.getInstance().getPeerManager().count() == 1 && args == 2;

					if (autoPeer || args >= 3) {
						Peer peer;

						if (!autoPeer) {
							String peerName = arg[1];

							if (Main.getInstance().getPeerManager().exists(peerName)) {
								peer = Main.getInstance().getPeerManager().get(peerName);
							} else {
								log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
								return;
							}
						} else
							peer = Main.getInstance().getPeerManager().getFirstPeer();

						boolean addressKeyConnection = arg[autoPeer ? 1 : 2].equalsIgnoreCase("-k");

						if (addressKeyConnection) {

							log.log("Enter remote Address Key: ");

							var read = System.console().readPassword();
							if (read == null || read.length == 0) {
								log.log("Please enter an Address Key.");
								return;
							}

							InetSocketAddress address;

							try {
								address = peer.getConnectionManager().getHandshakeAuthentificator()
										.parseAddressKey(read).getAddress();

							} catch (IllegalArgumentException e) {
								log.log(e.getMessage());
								return;
							} catch (UnknownHostException e) {
								log.log("This address key references an unknown host.");
								return;
							} catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException
									| NoSuchPaddingException | IllegalBlockSizeException e) {
								log.log("There was an error reading the provided address key: " + e.getMessage() + ".");
								return;
							} catch (Exception e) {
								log.log(e, "There was an unknown error reading the provided address key.");
								return;
							}

							try {
								peer.connect(address);
							} catch (Exception e) {
								log.log(e);
							}

						} else {

							String[] rawAddress = arg[autoPeer ? 1 : 2].split(":");
							boolean local = rawAddress.length == 1;

							int port;
							try {
								port = Integer.valueOf(rawAddress[local ? 0 : 1]);
							} catch (NumberFormatException ex) {
								log.log("ERROR: Invalid address format.");
								return;
							}

							try {
								InetSocketAddress address = local
										? new InetSocketAddress(InetAddress.getLocalHost(), port)
										: new InetSocketAddress(rawAddress[0], port);

								if (peer.getConnectionManager().isConnectedTo(address)) {
									log.log("ERROR: \"" + peer.getName()
											+ "\" peer is already connected to this address.");
									return;
								}

								if (local)
									log.log("INFO: No hostname provided, connecting to localhost.");

								peer.connect(address);
							} catch (UnknownHostException e) {
								log.log("ERROR: Invalid address.");

							} catch (IOException | GeneralSecurityException e) {
								log.log(e);
							}
						}
					}
			}
		}
	}

	public static Set<ConnectedPeer> getTargets(ILog log, String fromName, String targets) {
		Set<ConnectedPeer> to = new HashSet<>();
		Peer from;

		if (Main.getInstance().getPeerManager().exists(fromName)) {
			from = Main.getInstance().getPeerManager().get(fromName);
		} else {
			log.log("ERROR: Peer \"" + fromName + "\" doesn't exists.");
			return to;
		}

		if (targets.equalsIgnoreCase("b")) {
			to = from.getConnectionManager().getIdentificator().getConnectedPeers();

		} else {
			String[] toNames = targets.split(",");

			for (String toName : toNames)
				if (from.getConnectionManager().getIdentificator().hasPeer(toName)) {
					to.add(from.getConnectionManager().getIdentificator().getPeer(toName));
				} else {
					log.log("ERROR: \"" + from.getName() + "\" isn't connected to \"" + toName + "\".");
				}
		}

		return to;
	}
}
