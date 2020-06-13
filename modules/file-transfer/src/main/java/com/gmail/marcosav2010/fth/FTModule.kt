package com.gmail.marcosav2010.fth

import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.fth.command.FTCommandRegistry
import com.gmail.marcosav2010.fth.packet.PacketFileAccept
import com.gmail.marcosav2010.fth.packet.PacketFileRequest
import com.gmail.marcosav2010.fth.packet.PacketFileSend
import com.gmail.marcosav2010.fth.packet.PacketFileSendFailed
import com.gmail.marcosav2010.module.Module
import com.gmail.marcosav2010.module.ModuleCommandRegistry
import com.gmail.marcosav2010.module.ModuleDescriptor
import com.gmail.marcosav2010.module.ModuleScope

@ModuleCommandRegistry(FTCommandRegistry::class)
@ModuleDescriptor(name = "FTH", scope = Connection::class, listeners = [FTListener::class])
class FTModule(moduleDescriptor: ModuleDescriptor) : Module(moduleDescriptor) {
    companion object {
        init {
            registerPacket(3, PacketFileAccept::class.java)
            registerPacket(4, PacketFileRequest::class.java)
            registerPacket(5, PacketFileSend::class.java)
            registerPacket(6, PacketFileSendFailed::class.java)

            /*registerPacket(7, PacketFindFile.class);
		registerPacket(8, PacketGotFile.class);*/
        }
    }

    lateinit var fth: FileTransferHandler
        private set

    override fun onInit(scope: ModuleScope) {
        super.onInit(scope)
        fth = FileTransferHandler(this, scope as Connection)
    }
}