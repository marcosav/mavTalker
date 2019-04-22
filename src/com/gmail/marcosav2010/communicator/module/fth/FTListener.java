package com.gmail.marcosav2010.communicator.module.fth;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.module.fth.FileTransferHandler.FileDownloadResult;
import com.gmail.marcosav2010.communicator.module.fth.FileTransferHandler.FileSendResult;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileAccept;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileRequest;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileSend;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileSendFailed;
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
		fth.log("File request: #" + pf.getID() + " \"" + pf.getName() + "\" (" + Utils.formatSize(pf.getSize()) + "), accept download? Use download command");
		fth.handleRequest(pf);
	}

	@PacketEventHandler
	public void onFileReceive(PacketFileSend p, ConnectedPeer peer) {
		FileDownloadResult result = fth.handleReceiveFile(p);
		if (result != FileDownloadResult.SUCCESS)
			fth.log("File #" + p.getFileId() + " could not be downloaded: " + result.toString());
	}

	@PacketEventHandler
	public void onFileAcceptRespose(PacketFileAccept p, ConnectedPeer peer) {
		Connection connection = peer.getConnection();
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
		FileSendResult result = p.getCause();
		fth.log("File #" + p.getFileId() + " could not be sent because: " + result.toString());
	}
}
