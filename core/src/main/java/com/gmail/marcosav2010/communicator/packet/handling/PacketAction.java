package com.gmail.marcosav2010.communicator.packet.handling;

import com.gmail.marcosav2010.communicator.packet.Packet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class PacketAction {

    private final Runnable action, onTimeOut;
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private Class<? extends Packet> type;

    public void onReceive() {
        if (action != null)
            action.run();
    }

    public void onTimeOut() {
        if (onTimeOut != null)
            onTimeOut.run();
    }
}
