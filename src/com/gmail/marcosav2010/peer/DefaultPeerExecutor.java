package com.gmail.marcosav2010.peer;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultPeerExecutor extends ThreadPoolExecutor {

    public DefaultPeerExecutor(String name) {
        this(name, null);
    }

    public DefaultPeerExecutor(String name, ThreadGroup parentThreadGroup) {

        super(1, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new PeerThreadFactory(name, parentThreadGroup));
    }
}