package com.gmail.marcosav2010.communicator.packet.handling.listener

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.peer.ConnectedPeer
import java.lang.reflect.Method
import java.util.*

/**
 * This class handles packets when are received.
 *
 * @author Marcos
 */
class PacketEventHandlerManager(private val connection: Connection) {

    private val log: ILog = Log(connection, "PEH")

    private val methodListener: MutableMap<Method, PacketListener> = HashMap()
    private val packetMethods: MutableMap<Class<out Packet>, MutableSet<Method>> = HashMap()

    val listenerCount: Int
        get() = HashSet(methodListener.values).size

    val handlerCount: Long
        get() = packetMethods.values.size.toLong()

    private fun put(m: Method, listener: PacketListener) {
        methodListener[m] = listener
    }

    fun registerListeners(packetListeners: Collection<PacketListener>) {
        packetListeners.forEach { l ->
            l.javaClass.methods.forEach { m ->
                if (m.isAnnotationPresent(PacketEventHandler::class.java) &&
                        m.parameters.size == 2 &&
                        Packet::class.java.isAssignableFrom(m.parameterTypes[0])
                        && ConnectedPeer::class.java.isAssignableFrom(m.parameterTypes[1])) {

                    @Suppress("UNCHECKED_CAST")
                    val packetType = m.parameters[0].type as Class<out Packet>

                    if (!packetMethods.containsKey(packetType)) {
                        packetMethods[packetType] = mutableSetOf(m)
                    } else {
                        val mList = packetMethods[packetType]!!
                        mList.add(m)
                        packetMethods[packetType] = mList
                    }

                    put(m, l)
                }
            }
        }
    }

    fun unregisterEvents() {
        packetMethods.clear()
        methodListener.clear()
    }

    fun handlePacket(packet: Packet) {
        val pClass = packetMethods[packet.javaClass] ?: return
        pClass.forEach { me ->
            try {
                me.invoke(methodListener[me], packet, connection.connectedPeer)
            } catch (e: IllegalAccessException) {
                log.log(e,
                        """There was an error while handling event:
	Method: ${me.name}
	Class: ${me.declaringClass.name}
	Packet: ${packet.javaClass.simpleName}
	Stacktrace: """)
            }
        }
    }
}