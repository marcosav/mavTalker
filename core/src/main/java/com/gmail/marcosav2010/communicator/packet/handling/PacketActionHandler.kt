package com.gmail.marcosav2010.communicator.packet.handling

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.tasker.TaskOwner
import com.gmail.marcosav2010.tasker.Tasker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * This class handles the action is done when a packet is received by remote
 * peer.
 *
 * @author Marcos
 */
class PacketActionHandler(connection: Connection) {

    companion object {
        private const val MAX_TIMEOUT_SECONDS = 10L
    }

    private val log: ILog = Log(connection, "PAH")
    private val pendingActions: MutableMap<Long, PacketAction> = ConcurrentHashMap()

    private fun isPending(id: Long): Boolean = pendingActions.containsKey(id)

    fun handleResponse(id: Long) {
        if (!isPending(id)) return
        val pa = pendingActions.remove(id)
        try {
            pa!!.onReceive()
        } catch (e: Exception) {
            log.log(e, """There was an error while handling action:
	ID: $id
	Packet: ${pa!!.type.name}
	Stacktrace: """)
        }
    }

    fun handleSend(owner: TaskOwner,
                   id: Long,
                   packet: Packet,
                   action: () -> Unit,
                   onTimeOut: (() -> Unit)?,
                   expireTimeout: Long,
                   timeUnit: TimeUnit) {

        check(timeUnit.toSeconds(expireTimeout) <= MAX_TIMEOUT_SECONDS) {
            "Expire timeout must be lower or equal than $MAX_TIMEOUT_SECONDS seconds"
        }

        val pa = PacketAction(action, onTimeOut, packet.javaClass)
        pendingActions[id] = pa

        Tasker.schedule(owner, { onExpire(id) }, expireTimeout, timeUnit)
    }

    private fun onExpire(id: Long) {
        if (!isPending(id)) return

        val pa = pendingActions.remove(id)
        try {
            pa!!.onTimeOut()
        } catch (e: Exception) {
            log.log(e, """There was an error while handling time out action:
	ID: $id
	Packet: ${pa!!.type.name}
	Stacktrace: """)
        }
    }
}