package com.gmail.marcosav2010.communicator.module.fth;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.marcosav2010.command.BaseCommandRegistry;
import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFindFile;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

public class FTCommandRegistry extends CommandRegistry {

	public FTCommandRegistry() {
		super(Set.of(new FileCMD(), new ClearDownloadsCMD(), new DownloadCMD(), new FindCMD()));
	}

	private static FileTransferHandler getFTH(Connection c) {
		return ((FTModule) c.getModuleManager().getModule("FTH")).getFTH();
	}

	private static class FindCMD extends Command {

		FindCMD() {
			super("find", "[peer] <filename>");
		}

		@Override
		public void execute(String[] arg, int args) {
			int pCount = Main.getInstance().getPeerManager().count();

			if (args < 1 && pCount == 1 || args < 2 && pCount > 1 || pCount == 0) {
				log.log("ERROR: Needed filename at least.");
				return;
			}

			boolean autoPeer = pCount > 1;
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

			String filename = arg[autoPeer ? 0 : 1];

			log.log("Finding file \"" + filename + "\"...");

			var connectedPeers = peer.getConnectionManager().getIdentificator().getConnectedPeers();
			var p = new PacketFindFile(filename, 1,
					connectedPeers.stream().map(ConnectedPeer::getUUID).collect(Collectors.toSet()));

			connectedPeers.forEach(c -> {
				try {
					c.sendPacket(p);
				} catch (PacketWriteException e) {
					log.log(e, "There was a problem while sending find packet to " + c.getName() + ".");
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
				log.log("ERROR: Needed transmitter, targets, and a file name.");
				return;
			}

			Set<ConnectedPeer> to = BaseCommandRegistry.getTargets(log, arg[0], arg[1]);
			if (to.isEmpty())
				return;

			String filename = arg[2];

			File fIn = new File(filename);

			FileSendInfo info;
			try {
				info = FileTransferHandler.createRequest(fIn);
			} catch (IllegalArgumentException ex) {
				log.log("ERROR: " + ex.getMessage());
				return;
			}

			to.forEach(c -> getFTH(c.getConnection()).sendRequest(info));

			log.log("INFO: File \"" + info.getFileName() + "\" transfer request has been sent to "
					+ to.stream().map(t -> t.getName()).collect(Collectors.joining(",")) + ".");
		}
	}

	private static class DownloadCMD extends Command {

		DownloadCMD() {
			super("download", new String[] { "d", "dw" },
					"<host peer> <remote peer> <file id> <yes/no> (default = yes)");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 3) {
				log.log("ERROR: Needed host and remote peer, file id and yes/no option (yes by default).");
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

			String remoteName = arg[1];
			ConnectionManager cManager = peer.getConnectionManager();
			ConnectionIdentificator cIdentificator = cManager.getIdentificator();

			Connection connection;

			if (!cIdentificator.hasPeer(remoteName)) {
				log.log("ERROR: " + peerName + " peer is not connected to that " + remoteName + ".");
				return;
			}
			connection = cIdentificator.getPeer(remoteName).getConnection();

			FileTransferHandler fth = getFTH(connection);

			int id;
			try {
				id = Integer.valueOf(arg[2]);
			} catch (NumberFormatException ex) {
				log.log("ERROR: Invalid file id.");
				return;
			}

			if (!fth.isPendingRequest(id)) {
				log.log("ERROR: File ID #" + id + " hasn't got requests.");
				return;
			}
			FileReceiveInfo info = fth.getRequest(id);

			boolean yes = args < 4;

			if (!yes) {
				String o = arg[3].toLowerCase();
				yes |= o.equals("yes") || o.equals("y");
			}

			if (yes) {
				log.log("Accepted file #" + id + " (" + info.getFileName() + ") transfer request.");
				fth.acceptRequest(id);
			} else {
				fth.rejectRequest(id);
				log.log("Rejected file #" + id + " (" + info.getFileName() + ") transfer request.");
			}
		}
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
					long totalSize = Files.walk(p).sorted(Collections.reverseOrder())
							.mapToLong(sp -> sp.toFile().length()).sum();
					Files.walk(p).sorted(Collections.reverseOrder()).map(Path::toFile).forEach(File::delete);
					log.log("INFO: Successfully removed " + Utils.formatSize(totalSize) + " of downloaded files.");

				} catch (IOException e) {
					log.log(e);
				}
			else
				log.log("INFO: There are currently no downloads.");
		}
	}
}
