package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.PacketRegistry.register
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Loggable
import kotlin.properties.Delegates

abstract class Module(moduleDescriptor: ModuleDescriptor) : Comparable<Module>, Loggable {

    val name: String = moduleDescriptor.name
    val priority: Int = moduleDescriptor.priority

    final override var log by Delegates.notNull<ILog>()
        private set

    /**
     * Called when module is created: Connection, Peer: instantiation. Sets
     * up logger.
     *
     * @param scope in which module lives.
     */
    open fun onInit(scope: ModuleScope) {
        log = Log(scope, name)
    }

    /**
     * Called on: Connection: pairing complete. Peer: start.
     *
     * @param scope in which module lives.
     */
    fun onEnable(scope: ModuleScope) {}

    /**
     * Called on: Connection: beginning of disconnection. Peer: beginning
     * of shutdown.
     *
     * @param scope in which module lives.
     */
    fun onDisable(scope: ModuleScope) {}

    override fun compareTo(other: Module): Int {
        return priority - other.priority
    }

    companion object {

        fun registerPacket(id: Int, packet: Class<out Packet>) {
            require(id <= Byte.MAX_VALUE) { "ID must be byte" }
            register(id.toByte(), packet)
        }
    }
}