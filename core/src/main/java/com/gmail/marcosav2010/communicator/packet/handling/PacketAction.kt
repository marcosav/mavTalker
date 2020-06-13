package com.gmail.marcosav2010.communicator.packet.handling

import com.gmail.marcosav2010.communicator.packet.Packet

class PacketAction(private val action: () -> Unit,
                   private val onTimeOut: (() -> Unit)?,
                   internal var type: Class<out Packet?>
) {

    fun onReceive() = action.invoke()

    fun onTimeOut() = onTimeOut?.invoke()
}