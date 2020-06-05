package com.gmail.marcosav2010.fth.packet;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class PacketFindFile extends Packet {

    @Getter
    private String fileName;
    @Getter
    private int steps;
    @Getter
    private Set<UUID> checked;

    public PacketFindFile next(Set<UUID> newChecked) {
        if (!hasNext())
            throw new IllegalStateException("Cannot propagate find packet, there are no steps remaining.");

        int steps = this.steps == -1 ? -1 : this.steps - 1;
        checked.addAll(newChecked);
        return new PacketFindFile(fileName, steps, checked);
    }

    public boolean hasNext() {
        return steps > 0;
    }

    @Override
    protected void encodeContent(PacketEncoder out) throws IOException {
        out.write(fileName);
        out.write((short) steps);
        out.write(checked.size());
        for (UUID u : checked)
            out.write(u);
    }

    @Override
    protected void decodeContent(PacketDecoder in) throws IOException {
        fileName = in.readString();
        steps = in.readShort();
        checked = new HashSet<>(in.readInt());
        /*for (int i = 0; i < checked.size(); i++)
            checked.add(in.readUUID());*/
    }
}
