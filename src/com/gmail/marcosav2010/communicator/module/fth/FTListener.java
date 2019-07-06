package com.gmail.marcosav2010.communicator.module.fth;

import java.util.stream.Collectors;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.module.fth.FileTransferHandler.FileDownloadResult;
import com.gmail.marcosav2010.communicator.module.fth.FileTransferHandler.FileSendResult;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileAccept;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileRequest;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileSend;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileSendFailed;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFindFile;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketGotFile;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketEventHandler;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.peer.ConnectedPeer;

public class FTListener implements PacketListener {

	private FileTransferHandler fth;

	public void setFTH(FileTransferHandler fth) {
		this.fth = fth;
	}

	@PacketEventHandler
	public void onFileRequest(PacketFileRequest pf, ConnectedPeer peer) {
		fth.log("File request: #" + pf.getPacketID() + " \"" + pf.getName() + "\" (" + Utils.formatSize(pf.getSize()) + "), accept download? Use /d "
				+ peer.getConnection().getPeer().getName() + " " + peer.getName() + " " + pf.getFileID());
		fth.handleRequest(pf);
	}

	@PacketEventHandler
	public void onFileReceive(PacketFileSend p, ConnectedPeer peer) {
		FileDownloadResult result = fth.handleReceiveFile(p);
		if (result != FileDownloadResult.SUCCESS)
			fth.log("File #" + p.getFileID() + " could not be downloaded: " + result.toString());
	}

	@PacketEventHandler
	public void onFileAcceptRespose(PacketFileAccept p, ConnectedPeer peer) {
		Connection connection = peer.getConnection();
		FileSendResult result = fth.handleAcceptRespose(p);

		if (result != FileSendResult.SUCCESS) {
			fth.log("File #" + p.getFileID() + " could not be sent because: " + result.toString());
			if (connection.isConnected())
				try {
					connection.sendPacket(new PacketFileSendFailed(p.getFileID(), result));
				} catch (PacketWriteException e) {
					fth.log("Couldn't send result of File #" + p.getFileID() + ".");
					Logger.log(e);
				}
		}
	}

	@PacketEventHandler
	public void onFileRemoteSendFailed(PacketFileSendFailed p, ConnectedPeer peer) {
		FileSendResult result = p.getCause();
		fth.log("File #" + p.getFileID() + " could not be sent because: " + result.toString());
	}

	@PacketEventHandler
	public void onFileFindPacket(PacketFindFile p, ConnectedPeer peer) {
		// Si tiene le archivo envia paquete de got y talue

		if (p.hasNext()) {
			var connectedPeers = peer.getConnection().getPeer().getConnectionManager().getIdentificator().getConnectedPeers();
			var uuids = p.getChecked();
			var newPacket = p.next(connectedPeers.stream().map(ConnectedPeer::getUUID).collect(Collectors.toSet()));

			connectedPeers.stream().filter(c -> !uuids.contains(c.getUUID())).forEach(c -> {
				try {
					c.sendPacket(newPacket);
				} catch (PacketWriteException e) {
					Logger.log(e, "There was a problem while sending find packet to " + c.getName() + ".");
				}
			});

		}
	}

	@PacketEventHandler
	public void onFileGotPacket(PacketGotFile p, ConnectedPeer peer) {
		Logger.log(p.getFileName() + " is own by " + p.getOwner());
	}
}
