package com.gmail.marcosav2010.communicator.packet.handling.listener;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.packet.handling.listener.file.FileTransferHandler;
import com.gmail.marcosav2010.communicator.packet.handling.listener.file.FileTransferHandler.FileDownloadResult;
import com.gmail.marcosav2010.communicator.packet.handling.listener.file.FileTransferHandler.FileSendResult;
import com.gmail.marcosav2010.communicator.packet.packets.PacketFileAccept;
import com.gmail.marcosav2010.communicator.packet.packets.PacketFileRequest;
import com.gmail.marcosav2010.communicator.packet.packets.PacketFileSend;
import com.gmail.marcosav2010.communicator.packet.packets.PacketFileSendFailed;
import com.gmail.marcosav2010.communicator.packet.packets.PacketMessage;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.peer.ConnectedPeer;

public class DefaultPacketListener implements PacketListener {

	@PacketEventHandler
	public void onPacketMessage(PacketMessage pm, ConnectedPeer peer) {
		Logger.log("Message: \"" + pm.getMessage() + "\"");
	}

	@PacketEventHandler
	public void onFileRequest(PacketFileRequest pf, ConnectedPeer peer) {
		FileTransferHandler fth = peer.getConnection().getFileTransferHandler();
		fth.log("File request: #" + pf.getID() + " \"" + pf.getName() + "\" (" + Utils.formatSize(pf.getSize()) + "), accept download? Use download command");
		fth.handleRequest(pf);
	}

	@PacketEventHandler
	public void onFileReceive(PacketFileSend p, ConnectedPeer peer) {
		FileTransferHandler fth = peer.getConnection().getFileTransferHandler();
		FileDownloadResult result = fth.handleReceiveFile(p);
		if (result != FileDownloadResult.SUCCESS)
			fth.log("File #" + p.getFileId() + " could not be downloaded: " + result.toString());
	}

	@PacketEventHandler
	public void onFileAcceptRespose(PacketFileAccept p, ConnectedPeer peer) {
		Connection connection = peer.getConnection();
		FileTransferHandler fth = connection.getFileTransferHandler();
		FileSendResult result = fth.handleAcceptRespose(p);
		if (result != FileSendResult.SUCCESS) {
			fth.log("File #" + p.getFileId() + " could not be sent because: " + result.toString());
			if (connection.isConnected())
				try {
					connection.sendPacket(new PacketFileSendFailed(p.getFileId(), result));
				} catch (PacketWriteException e) {
					fth.log("Couldn't send result of File #" + p.getFileId() + ".");
					Logger.log(e);
				}
		}
	}

	@PacketEventHandler
	public void onFileRemoteSendFailed(PacketFileSendFailed p, ConnectedPeer peer) {
		FileTransferHandler fth = peer.getConnection().getFileTransferHandler();
		FileSendResult result = p.getCause();
		fth.log("File #" + p.getFileId() + " could not be sent because: " + result.toString());
	}
}
