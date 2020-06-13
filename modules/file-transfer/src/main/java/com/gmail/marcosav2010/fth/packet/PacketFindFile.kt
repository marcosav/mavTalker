package com.gmail.marcosav2010.fth.packet

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import java.util.*
import kotlin.properties.Delegates

class PacketFindFile() : Packet() {

    var fileName by Delegates.notNull<String>()
        private set

    var steps by Delegates.notNull<Int>()
        private set

    private var checked: MutableSet<UUID> by Delegates.notNull()

    constructor(fileName: String, steps: Int, checked: MutableSet<UUID>) : this() {
        this.fileName = fileName
        this.steps = steps
        this.checked = checked
    }

    fun next(newChecked: Set<UUID>): PacketFindFile {
        check(hasNext()) { "Cannot propagate find packet, there are no steps remaining." }
        val steps = if (steps == -1) -1 else steps - 1
        checked.addAll(newChecked)
        return PacketFindFile(fileName, steps, checked)
    }

    operator fun hasNext() = steps > 0

    override fun encodeContent(out: PacketEncoder) {
        out.write(fileName)
        out.write(steps.toShort())
        out.write(checked.size)
        for (u in checked) out.write(u)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        fileName = `in`.readString()
        steps = `in`.readShort().toInt()
        checked = HashSet(`in`.readInt())
        /*for (int i = 0; i < checked.size(); i++)
            checked.add(in.readUUID());*/
    }
}