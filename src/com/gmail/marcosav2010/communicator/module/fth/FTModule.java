package com.gmail.marcosav2010.communicator.module.fth;

import com.gmail.marcosav2010.communicator.module.LoadModule;
import com.gmail.marcosav2010.communicator.module.Module;
import com.gmail.marcosav2010.communicator.module.ModuleManager;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileAccept;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileRequest;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileSend;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFileSendFailed;
import com.gmail.marcosav2010.connection.Connection;

@LoadModule(
		registry = FTCommandRegistry.class
)
public class FTModule extends Module {

	static {
		registerPacket(3, PacketFileAccept.class);
		registerPacket(4, PacketFileRequest.class);
		registerPacket(5, PacketFileSend.class);
		registerPacket(6, PacketFileSendFailed.class);
	}
	
	private FileTransferHandler fth;
	
	public FTModule(ModuleManager manager) {
		super(manager, "FTH");
		
		registerListeners(new FTListener());
	}

	@Override
	protected void onEnable(Connection connection) {
		fth = new FileTransferHandler(this, connection);
	}
	
	public FileTransferHandler getFTH() {
		return fth;
	}
}
