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
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

public class FTCommandRegistry extends CommandRegistry {

	public FTCommandRegistry() {
		super(Set.of(new FileCMD(), new ClearDownloadsCMD(), new DownloadCMD()));
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

			Set<ConnectedPeer> to = BaseCommandRegistry.getTargets(arg[0], arg[1]);
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

			to.forEach(c -> c.getConnection().getModuleManager().getFTH().sendRequest(info));

			Logger.log("INFO: File \"" + info.getFileName() + "\" transfer request has been sent to "
					+ to.stream().map(t -> t.getName()).collect(Collectors.joining(",")) + ".");
		}
	}

	private static class DownloadCMD extends Command {

		DownloadCMD() {
			super("download", new String[] { "d", "dw" }, "<host peer> <remote peer> <file id> <yes/no> (default = yes)");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 3) {
				Logger.log("ERROR: Needed host and remote peer, file id and yes/no option (yes by default).");
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

			FileTransferHandler fth = connection.getModuleManager().getFTH();

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

			boolean yes = args < 4;

			if (!yes) {
				String o = arg[3].toLowerCase();
				yes |= o.equals("yes") || o.equals("y");
			}

			if (yes) {
				Logger.log("Accepted file #" + id + " (" + info.getFileName() + ") transfer request.");
				fth.acceptRequest(id);
			} else {
				fth.rejectRequest(id);
				Logger.log("Rejected file #" + id + " (" + info.getFileName() + ") transfer request.");
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
}
