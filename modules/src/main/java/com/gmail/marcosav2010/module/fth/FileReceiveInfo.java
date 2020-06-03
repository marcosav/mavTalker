package com.gmail.marcosav2010.module.fth;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FileReceiveInfo {

    @Getter
    private final String fileName;
    @Getter
    private final int blocks;
    @Getter(AccessLevel.PACKAGE)
    private long firstArrivalTime = -1, lastArrivalTime = -1;

    public boolean isSingle() {
        return blocks == 1;
    }

    synchronized void setFirstArrivalTime() {
        if (firstArrivalTime == -1)
            lastArrivalTime = firstArrivalTime = System.currentTimeMillis();
    }

    synchronized boolean updateLastArrivalTime(long req) {
        long current = System.currentTimeMillis();
        long diff = current - lastArrivalTime;
        if (diff >= req) {
            lastArrivalTime = current;
            return true;

        } else
            return false;
    }
}
