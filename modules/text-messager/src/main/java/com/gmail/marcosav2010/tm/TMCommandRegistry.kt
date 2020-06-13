package com.gmail.marcosav2010.tm

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.command.CommandRegistry
import com.gmail.marcosav2010.command.base.BaseCommandUtils.getTargets
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.logger.Logger.VerboseLevel

class TMCommandRegistry : CommandRegistry(setOf(MessageCMD())) {

    private class MessageCMD internal constructor() :
            Command("message", arrayOf("msg", "m"), "<from> <to (P1,P2...) (B = all)> <msg>") {

        override fun execute(arg: Array<String>, length: Int) {
            if (length < 3) {
                log.log("ERROR: Needed transmitter, targets, and a message.")
                return
            }

            val to = getTargets(log, arg[0], arg[1])
            if (to.isEmpty()) return
            val toWrite = StringBuilder()

            for (i in 2 until length)
                toWrite.append(arg[i]).append(" ")

            val finalMsg = toWrite.toString().trim { it <= ' ' }

            log.log("INFO: Sending to \"${to.joinToString(",") { obj -> obj.name }}"
                    + "\" message \" $finalMsg \".", VerboseLevel.MEDIUM)

            to.forEach { c ->
                try {
                    c.sendPacket(PacketMessage(finalMsg))
                } catch (e: PacketWriteException) {
                    log.log(e)
                }
            }
        }
    }
}