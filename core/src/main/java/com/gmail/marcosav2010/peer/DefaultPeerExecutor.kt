package com.gmail.marcosav2010.peer

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class DefaultPeerExecutor @JvmOverloads constructor(name: String, parentThreadGroup: ThreadGroup? = null) :
        ThreadPoolExecutor(1, Int.MAX_VALUE, 10L, TimeUnit.SECONDS,
                SynchronousQueue(),
                PeerThreadFactory(name, parentThreadGroup))