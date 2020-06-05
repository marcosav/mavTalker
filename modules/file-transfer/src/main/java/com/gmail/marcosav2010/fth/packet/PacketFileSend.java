package com.gmail.marcosav2010.fth.packet;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;
import com.gmail.marcosav2010.fth.FileTransferHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor
public class PacketFileSend extends Packet {

    /**
     * Data fields + byte arrays + array length
     */
    public static final int MAX_BLOCK_SIZE = Packet.MAX_SIZE - 2 * Integer.BYTES - FileTransferHandler.HASH_SIZE - 2 * Integer.BYTES;

    @Getter
    private int fileID;
    @Getter
    private int pointer;
    @Getter
    private byte[] bytes;
    @Getter
    private byte[] hash;

    public PacketFileSend(int fileID, int pointer, byte[] bytes, byte[] hash) {
        this.fileID = fileID;
        this.pointer = pointer;
        if (bytes.length > MAX_BLOCK_SIZE)
            throw new IllegalArgumentException("Byte block size cannot exceed " + MAX_BLOCK_SIZE + " bytes");
        if (hash.length > FileTransferHandler.HASH_SIZE)
            throw new IllegalArgumentException("Hash size cannot exceed " + FileTransferHandler.HASH_SIZE + " bytes");
        this.bytes = bytes;
        this.hash = hash;
    }

    @Override
    public boolean shouldSendRespose() {
        return false;
    }

    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
        out.write(fileID);
        out.write(pointer);
        out.write(bytes);
        out.write(hash);
    }

    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
        fileID = in.readInt();
        pointer = in.readInt();
        bytes = in.readBytes();
        hash = in.readBytes();
    }
}
