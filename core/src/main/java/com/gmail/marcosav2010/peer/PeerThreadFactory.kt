package com.gmail.marcosav2010.peer

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class PeerThreadFactory @JvmOverloads constructor(peerName: String, parentThreadGroup: ThreadGroup? = null) :
        ThreadFactory {

    private var group: ThreadGroup? = null
    private val threadNumber = AtomicInteger(1)
    private val namePrefix: String

    init {
        val threadGroupName = "peerThreadGroup-$peerName"
        group = parentThreadGroup?.let { ThreadGroup(it, threadGroupName) } ?: ThreadGroup(threadGroupName)
        namePrefix = "peerPool-$peerName-thread-"
    }

    override fun newThread(r: Runnable): Thread {
        val t = Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0)
        if (t.isDaemon) t.isDaemon = false
        if (t.priority != Thread.NORM_PRIORITY) t.priority = Thread.NORM_PRIORITY
        return t
    }
}