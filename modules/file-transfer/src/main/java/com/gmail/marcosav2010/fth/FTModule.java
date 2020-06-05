package com.gmail.marcosav2010.fth;

import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.fth.command.FTCommandRegistry;
import com.gmail.marcosav2010.fth.packet.PacketFileAccept;
import com.gmail.marcosav2010.fth.packet.PacketFileRequest;
import com.gmail.marcosav2010.fth.packet.PacketFileSend;
import com.gmail.marcosav2010.fth.packet.PacketFileSendFailed;
import com.gmail.marcosav2010.module.Module;
import com.gmail.marcosav2010.module.ModuleCommandRegistry;
import com.gmail.marcosav2010.module.ModuleDescriptor;
import com.gmail.marcosav2010.module.ModuleScope;

@ModuleCommandRegistry(FTCommandRegistry.class)
@ModuleDescriptor(name = "FTH", scope = Connection.class, listeners = {
        FTListener.class})
public class FTModule extends Module {

    static {
        registerPacket(3, PacketFileAccept.class);
        registerPacket(4, PacketFileRequest.class);
        registerPacket(5, PacketFileSend.class);
        registerPacket(6, PacketFileSendFailed.class);
		
		/*registerPacket(7, PacketFindFile.class);
		registerPacket(8, PacketGotFile.class);*/
    }

    private FileTransferHandler fth;

    public FTModule(ModuleDescriptor moduleDescriptor) {
        super(moduleDescriptor);
    }

    @Override
    protected final void onInit(ModuleScope connection) {
        super.onInit(connection);
        fth = new FileTransferHandler(this, (Connection) connection);
    }

    public FileTransferHandler getFTH() {
        return fth;
    }
}
