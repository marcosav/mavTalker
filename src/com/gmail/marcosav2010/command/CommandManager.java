package com.gmail.marcosav2010.command;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.packet.handling.listener.file.FileReceiveInfo;
import com.gmail.marcosav2010.communicator.packet.handling.listener.file.FileSendInfo;
import com.gmail.marcosav2010.communicator.packet.handling.listener.file.FileTransferHandler;
import com.gmail.marcosav2010.communicator.packet.packets.PacketMessage;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

public class CommandManager {

	private final Set<Command> registeredCommands;

	public CommandManager() {
		registeredCommands = Set.of(new ExitCMD(), new VerboseCMD(), new InfoCMD(), new StopCMD(), new MessageCMD(), new FileCMD(), new NewCMD(), new HelpCMD(),
				new DisconnectCMD(), new ClearDownloadsCMD(), new DownloadCMD());
	}

	public Set<Command> getCommands() {
		return registeredCommands;
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
			Logger.setVerboseLevel(level);
			Logger.log("Verbose level: " + level);
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
			super("new", new String[] { "create" }, "<peer [name] [port]/connection [peer] [address:port] (no address = localhost)>");
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
			case "conn":
			case "connection":
				boolean autoPeer = Main.getInstance().getPeerManager().count() == 1 && args == 2;

				if (autoPeer || args >= 3) {
					String[] rawAddress = arg[autoPeer ? 1 : 2].split(":");
					boolean local = rawAddress.length == 1;

					int port;
					try {
						port = Integer.valueOf(rawAddress[local ? 0 : 1]);
					} catch (NumberFormatException ex) {
						Logger.log("ERROR: Invalid address format.");
						return;
					}

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
						peer = Main.getInstance().getPeerManager().getPeer();

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

	private static class MessageCMD extends Command {

		MessageCMD() {
			super("message", new String[] { "msg", "m" }, "<from> <to (P1,P2...) (B = all)> <msg>");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 3) {
				Logger.log("ERROR: Needed transmitter, targets, and a message.");
				return;
			}
			Set<ConnectedPeer> to = getTargets(arg[0], arg[1]);
			if (to.isEmpty())
				return;

			String toWrite = "";
			for (int i = 2; i < args; i++) {
				toWrite += arg[i] + " ";
			}

			String finalMsg = toWrite.trim();

			Logger.log("INFO: Sending to \"" + to.stream().map(t -> t.getName()).collect(Collectors.joining(",")) + "\" message \"" + finalMsg + "\".",
					VerboseLevel.MEDIUM);

			to.forEach(c -> {
				try {
					c.sendPacket(new PacketMessage(finalMsg));
				} catch (PacketWriteException e) {
					Logger.log(e);
				}
			});
		}
	}

	private static class FileCMD extends Command {

		FileCMD() {
			super("file", new String[] { "f" }, "<from> <to (P1,P2...) (B = all)> <filename>");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 3) {
				Logger.log("ERROR: Needed transmitter, targets, and a file name.");
				return;
			}

			Set<ConnectedPeer> to = getTargets(arg[0], arg[1]);
			if (to.isEmpty())
				return;

			String filename = arg[2];

			File fIn = new File(filename);

			FileSendInfo info;
			try {
				info = FileTransferHandler.createRequest(fIn);
			} catch (IllegalArgumentException ex) {
				Logger.log("ERROR: " + ex.getMessage());
				return;
			}

			to.forEach(c -> c.getConnection().getFileTransferHandler().sendRequest(info));

			Logger.log("INFO: File \"" + info.getFileName() + "\" transfer request has been sent to "
					+ to.stream().map(t -> t.getName()).collect(Collectors.joining(",")) + ".", VerboseLevel.MEDIUM);
		}
	}

	private static Set<ConnectedPeer> getTargets(String fromName, String targets) {
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

	private static class ClearDownloadsCMD extends Command {

		ClearDownloadsCMD() {
			super("cleardownloads", new String[] { "cd", "cleard", "cdownloads" });
		}

		@Override
		public void execute(String[] arg, int args) {
			Path p = Paths.get(FileTransferHandler.DOWNLOAD_FOLDER);
			if (p.toFile().exists())
				try {
					long totalSize = Files.walk(p).sorted(Collections.reverseOrder()).mapToLong(sp -> sp.toFile().length()).sum();
					Files.walk(p).sorted(Collections.reverseOrder()).map(Path::toFile).forEach(File::delete);
					Logger.log("INFO: Successfully removed " + Utils.formatSize(totalSize) + " of downloaded files.");

				} catch (IOException e) {
					Logger.log(e);
				}
			else
				Logger.log("INFO: There are currently no downloads.");
		}
	}

	private static class DownloadCMD extends Command {

		DownloadCMD() {
			super("download", new String[] { "d", "dw" }, "<host peer> <remote peer> <file id> <yes/no>");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 4) {
				Logger.log("ERROR: Needed host and remote peer, file id and yes/no option.");
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

			String remoteName = arg[1];
			ConnectionManager cManager = peer.getConnectionManager();
			ConnectionIdentificator cIdentificator = cManager.getIdentificator();

			Connection connection;

			if (!cIdentificator.hasPeer(remoteName)) {
				Logger.log("ERROR: " + peerName + " peer is not connected to that " + remoteName + ".");
				return;
			}
			connection = cIdentificator.getPeer(remoteName).getConnection();

			FileTransferHandler fth = connection.getFileTransferHandler();

			int id;
			try {
				id = Integer.valueOf(arg[2]);
			} catch (NumberFormatException ex) {
				Logger.log("ERROR: Invalid file id.");
				return;
			}

			if (!fth.isPendingRequest(id)) {
				Logger.log("ERROR: File ID #" + id + " hasn't got requests.");
				return;
			}
			FileReceiveInfo info = fth.getRequest(id);

			String o = arg[3].toLowerCase();
			boolean yes = o.equals("yes") || o.equals("y");
			if (yes) {
				Logger.log("Accepted file #" + id + " (" + info.getFileName() + ") transfer request.");
				fth.acceptRequest(id);
			} else {
				fth.rejectRequest(id);
				Logger.log("Rejected file #" + id + " (" + info.getFileName() + ") transfer request.");
			}
		}
	}
}
