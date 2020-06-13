package com.gmail.marcosav2010.tasker

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Task(private val tasker: Tasker,
           owner: TaskOwner,
           val id: Int,
           private val task: () -> Unit,
           delay: Long,
           period: Long,
           unit: TimeUnit) : Runnable {

    private val log: ILog = Log(owner, "Task #$id")

    private val delay: Long = unit.toMillis(delay)
    private val period: Long = unit.toMillis(period)

    private val running = AtomicBoolean(true)

    var name: String? = null
        private set

    var thread: Thread? = null
        private set

    fun setName(name: String): Task {
        this.name = name
        return this
    }

    fun cancel() {
        val wasRunning = running.getAndSet(false)
        if (wasRunning) tasker.cancel0(this)
    }

    fun cancelNow() {
        thread!!.interrupt()
        cancel()
    }

    override fun run() {
        thread = Thread.currentThread()

        if (delay > 0) try {
            Thread.sleep(delay)
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        while (running.get()) {
            try {
                task.invoke()
            } catch (t: Throwable) {
                log.log(t, "Task " + (if (name == null) "" else "$name ") + "#" + id + " encountered an exception")
            }

            if (period <= 0) break

            try {
                Thread.sleep(period)
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        cancel()
    }
}