package com.gmail.marcosav2010.peer;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class PeerThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    PeerThreadFactory(String peerName) {
        this(peerName, null);
    }

    PeerThreadFactory(String peerName, ThreadGroup parentThreadGroup) {
        String threadGroupName = "peerThreadGroup-" + peerName;
        if (parentThreadGroup != null)
            group = new ThreadGroup(parentThreadGroup, threadGroupName);
        else
            group = new ThreadGroup(threadGroupName);

        namePrefix = "peerPool-" + peerName + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);

        return t;
    }
}