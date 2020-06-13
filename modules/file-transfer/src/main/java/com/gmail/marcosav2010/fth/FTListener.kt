package com.gmail.marcosav2010.fth

import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketEventHandler
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.fth.FileTransferHandler.FileDownloadResult
import com.gmail.marcosav2010.fth.FileTransferHandler.FileSendResult
import com.gmail.marcosav2010.fth.packet.PacketFileAccept
import com.gmail.marcosav2010.fth.packet.PacketFileRequest
import com.gmail.marcosav2010.fth.packet.PacketFileSend
import com.gmail.marcosav2010.fth.packet.PacketFileSendFailed
import com.gmail.marcosav2010.peer.ConnectedPeer

class FTListener(module: FTModule) : PacketListener {

    private val fth: FileTransferHandler = module.fth

    @PacketEventHandler
    fun onFileRequest(pf: PacketFileRequest, peer: ConnectedPeer) {
        fth.log.log("File request: #${pf.packetID} \"${pf.name}\" ("
                + "${Utils.formatSize(pf.size.toLong())}), accept download? Use /d "
                + "${peer.connection.peer.name} ${peer.name} ${pf.fileID})")
        fth.handleRequest(pf)
    }

    @PacketEventHandler
    fun onFileReceive(p: PacketFileSend, peer: ConnectedPeer) {
        val result = fth.handleReceiveFile(p)
        if (result != FileDownloadResult.SUCCESS)
            fth.log.log("File #${p.fileID} could not be downloaded: $result")
    }

    @PacketEventHandler
    fun onFileAcceptResponse(p: PacketFileAccept, peer: ConnectedPeer) {
        val connection = peer.connection
        val result = fth.handleAcceptResponse(p)

        if (result != FileSendResult.SUCCESS) {
            fth.log.log("File #${p.fileID} could not be sent because: $result")
            if (connection.isConnected()) try {
                connection.sendPacket(PacketFileSendFailed(p.fileID, result))
            } catch (e: PacketWriteException) {
                fth.log.log(e, "Couldn't send result of File #${p.fileID}.")
            }
        }
    }

    @PacketEventHandler
    fun onFileRemoteSendFailed(p: PacketFileSendFailed, peer: ConnectedPeer) {
        fth.log.log("File #${p.fileID} could not be sent because: ${p.cause}")
    }

    /*
     * @PacketEventHandler public void onFileFindPacket(PacketFindFile p,
     * ConnectedPeer peer) { // Si tiene le archivo envia paquete de got y talue
     *
     * if (fth.hasFile(p.getFileName())) { try { peer.sendPacket(new
     * PacketGotFile(p.getFileName(), peer.getConnection().getPeer().getName())); }
     * catch (PacketWriteException e) { fth.getLog().log(e,
     * "There was a problem while sending find packet to " + peer.getName() + ".");
     * } return; }
     *
     * if (p.hasNext()) { var connectedPeers =
     * peer.getConnection().getPeer().getConnectionManager().getIdentificator()
     * .getConnectedPeers(); var uuids = p.getChecked(); var newPacket =
     * p.next(connectedPeers.stream().map(ConnectedPeer::getUUID).collect(Collectors
     * .toSet()));
     *
     * connectedPeers.stream().filter(c -> !uuids.contains(c.getUUID())).forEach(c
     * -> { try { c.sendPacket(newPacket); } catch (PacketWriteException e) {
     * fth.getLog().log(e, "There was a problem while sending find packet to " +
     * c.getName() + "."); } }); } }
     *
     * @PacketEventHandler public void onFileGotPacket(PacketGotFile p,
     * ConnectedPeer peer) { fth.getLog().log(p.getFileName() + " is own by " +
     * p.getOwner()); }
     */
}