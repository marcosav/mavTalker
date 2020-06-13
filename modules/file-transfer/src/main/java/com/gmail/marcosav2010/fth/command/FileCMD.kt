package com.gmail.marcosav2010.fth.command

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.command.base.BaseCommandUtils.getTargets
import com.gmail.marcosav2010.fth.FileTransferHandler
import com.gmail.marcosav2010.peer.ConnectedPeer
import java.io.File
import java.util.stream.Collectors

internal class FileCMD : Command("file", arrayOf("f"), "<from> <to (P1,P2...) (B = all)> <filename>") {

    override fun execute(arg: Array<String>, length: Int) {
        if (length < 3) {
            log.log("ERROR: Needed transmitter, targets, and a file name.")
            return
        }

        val to = getTargets(log, arg[0], arg[1])
        if (to.isEmpty()) return

        val filename = arg[2]
        val fIn = File(filename)

        val info = try {
            FileTransferHandler.createRequest(fIn)
        } catch (ex: IllegalArgumentException) {
            log.log("ERROR: " + ex.message)
            return
        }

        to.forEach { c -> FTCommandRegistry.getFTH(c.connection).sendRequest(info) }

        log.log("INFO: File \"" + info.fileName + "\" transfer request has been sent to "
                + to.stream().map { obj: ConnectedPeer -> obj.name }.collect(Collectors.joining(",")) + ".")
    }
}