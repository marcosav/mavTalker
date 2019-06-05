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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.config.GeneralConfiguration.PropertyCategory;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

public class BaseCommandRegistry extends CommandRegistry {

	public BaseCommandRegistry() {
		super(Set.of(new ExitCMD(), new VerboseCMD(), new InfoCMD(), new StopCMD(), new NewCMD(), new HelpCMD(), new DisconnectCMD(), new GenerateAddressCMD(),
				new ConnectionKeyCMD(), new PeerPropertyCMD()));
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
			super("peerproperty", new String[] { "pp", "pprop" }, "[peer] <property name> <value>");
		}

		@Override
		public void execute(String[] arg, int args) {
			int pCount = Main.getInstance().getPeerManager().count();

			if (pCount > 1 && args < 3 || pCount <= 1 && args < 2) {
				Logger.log("ERROR: Specify peer, property and value.");
				return;
			}

			boolean autoPeer = pCount == 1 && args == 2;
			
			Peer peer;

			if (!autoPeer) {
				String peerName = arg[0];

				if (Main.getInstance().getPeerManager().exists(peerName)) {
					peer = Main.getInstance().getPeerManager().get(peerName);
				} else {
					Logger.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
					return;
				}
			} else
				peer = Main.getInstance().getPeerManager().getFirstPeer();

			String prop = arg[autoPeer ? 0 : 1], value = arg[autoPeer ? 1 : 2];
			
			var c = Main.getInstance().getGeneralConfig();
			
			if (GeneralConfiguration.isCategory(prop, PropertyCategory.PEER)) {
				
				
			} else {
				Logger.log("Unrecognized property, try with the following ones:");
				Logger.log(GeneralConfiguration.propsToString(PropertyCategory.PEER, c));
			}
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
				Logger.log("ERROR: Specify peer.");
				return;
			}

			boolean autoPeer = pCount == 1 && args == 0;

			Peer peer;

			if (!autoPeer) {
				String peerName = arg[0];

				if (Main.getInstance().getPeerManager().exists(peerName)) {
					peer = Main.getInstance().getPeerManager().get(peerName);
				} else {
					Logger.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
					return;
				}
			} else
				peer = Main.getInstance().getPeerManager().getFirstPeer();

			String k;

			k = peer.getConnectionManager().getHandshakeAuthentificator().getConnectionKeyString();

			System.out.println("-----------------------------------------------------");
			System.out.println("\tConnection Key => " + k);
			System.out.println("-----------------------------------------------------");
		}
	}

	private static class GenerateAddressCMD extends Command {

		GenerateAddressCMD() {
			super("generate", new String[] { "g", "gen" }, "[peer]");
		}

		@Override
		public void execute(String[] arg, int args) {
			int pCount = Main.getInstance().getPeerManager().count();

			if (pCount > 1 && args == 0) {
				Logger.log("ERROR: Specify peer.");
				return;
			}

			boolean autoPeer = pCount == 1 && args == 0;
			boolean generatePublic = args >= 1 && arg[autoPeer ? 1 : 2].equalsIgnoreCase("-p");
			
			Peer peer;

			if (!autoPeer) {
				String peerName = arg[0];

				if (Main.getInstance().getPeerManager().exists(peerName)) {
					peer = Main.getInstance().getPeerManager().get(peerName);
				} else {
					Logger.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
					return;
				}
			} else
				peer = Main.getInstance().getPeerManager().getFirstPeer();

			
			String addressKey;
			
			if (generatePublic) {
				
				addressKey = peer.getConnectionManager().getHandshakeAuthentificator().generatePublicAddressKey();
				
			} else {
			
				Logger.log("Enter requester Connection Key: ");
	
				var requesterConnectionKey = System.console().readPassword();
				if (requesterConnectionKey == null)
					return;
	
				try {
					addressKey = peer.getConnectionManager().getHandshakeAuthentificator().generatePrivateAddressKey(requesterConnectionKey);
	
				} catch (IllegalArgumentException e) {
					Logger.log(e.getMessage());
					return;
				} catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException e) {
					Logger.log("There was an error reading the provided address key.");
					return;
				}
			}

			System.out.println("-----------------------------------------------------");
			System.out.println("\tAddress Key => " + addressKey);
			System.out.println("-----------------------------------------------------");
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
					Logger.log("ERROR: Invalid verbose level, use " + Stream.of(levels).map(Enum::toString).collect(Collectors.joining(", ")));
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
			Logger.log(">> cmd <required> [optional] (info)");
			Main.getInstance().getCommandManager().getCommands().forEach(c -> Logger.log(">> " + c.getUsage()));
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
				Logger.log("ERROR: Specify local and remote peer or address.");
				return;
			}

			Peer peer;
			String peerName = arg[0];

			if (Main.getInstance().getPeerManager().exists(peerName)) {
				peer = Main.getInstance().getPeerManager().get(peerName);
			} else {
				Logger.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
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
					Logger.log("ERROR: Invalid address format or peer.");
					return;
				}

				try {
					InetSocketAddress address = local ? new InetSocketAddress(InetAddress.getLocalHost(), port) : new InetSocketAddress(rawAddress[0], port);

					if (cManager.isConnectedTo(address))
						connection = cManager.getConnection(address);
					else {
						Logger.log("ERROR: " + peerName + " peer is not connected to that address.");
						return;
					}

				} catch (UnknownHostException ex) {
					Logger.log("ERROR: Invalid address.");
					return;
				}
			}

			connection.disconnect(false);
		}
	}

	private static class NewCMD extends Command {

		NewCMD() {
			super("new", new String[] { "create" }, "<peer [name] [port]/connection [peer] [address:port] (no address = localhost) [-k]>");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 1) {
				Logger.log("ERROR: Specify entity type.");
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
						Logger.log("ERROR: Invalid port.");
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
							Logger.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
							return;
						}
					} else
						peer = Main.getInstance().getPeerManager().getFirstPeer();

					boolean addressKeyConnection = arg[autoPeer ? 1 : 2].equalsIgnoreCase("-k");

					if (addressKeyConnection) {

						Logger.log("Enter remote Address Key: ");

						var read = System.console().readPassword();
						if (read == null)
							return;

						InetSocketAddress address;

						try {
							address = peer.getConnectionManager().getHandshakeAuthentificator().parseAddressKey(read).getAddress();

						} catch (IllegalArgumentException e) {
							Logger.log(e.getMessage());
							return;
						} catch (UnknownHostException e) {
							Logger.log("This address key references an unknown host.");
							return;
						} catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException e) {
							Logger.log("There was an error reading the provided address key.");
							return;
						} catch (Exception e) {
							Logger.log(e, "There was an unknown error reading the provided address key.");
							return;
						}

						try {
							peer.connect(address);
						} catch (Exception e) {
							Logger.log(e);
						}

					} else {

						String[] rawAddress = arg[autoPeer ? 1 : 2].split(":");
						boolean local = rawAddress.length == 1;

						int port;
						try {
							port = Integer.valueOf(rawAddress[local ? 0 : 1]);
						} catch (NumberFormatException ex) {
							Logger.log("ERROR: Invalid address format.");
							return;
						}

						try {
							InetSocketAddress address = local ? new InetSocketAddress(InetAddress.getLocalHost(), port)
									: new InetSocketAddress(rawAddress[0], port);

							if (peer.getConnectionManager().isConnectedTo(address)) {
								Logger.log("ERROR: \"" + peer.getName() + "\" peer is already connected to this address.");
								return;
							}

							if (local)
								Logger.log("INFO: No hostname provided, connecting to localhost.");

							peer.connect(address);
						} catch (UnknownHostException e) {
							Logger.log("ERROR: Invalid address.");

						} catch (IOException | GeneralSecurityException e) {
							Logger.log(e);
						}
					}
				}
			}
		}
	}

	public static Set<ConnectedPeer> getTargets(String fromName, String targets) {
		Set<ConnectedPeer> to = new HashSet<>();
		Peer from;

		if (Main.getInstance().getPeerManager().exists(fromName)) {
			from = Main.getInstance().getPeerManager().get(fromName);
		} else {
			Logger.log("ERROR: Peer \"" + fromName + "\" doesn't exists.");
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
					Logger.log("ERROR: \"" + from.getName() + "\" isn't connected to \"" + toName + "\".");
				}
		}

		return to;
	}
}
