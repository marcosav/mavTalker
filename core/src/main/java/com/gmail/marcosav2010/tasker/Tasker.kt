package com.gmail.marcosav2010.tasker

import com.gmail.marcosav2010.common.Utils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

object Tasker {

    private val lock = Any()
    private val taskCounter = AtomicInteger()
    private val tasks: MutableMap<Int, Task> = ConcurrentHashMap()
    private val tasksByOwner: MutableMap<TaskOwner, MutableSet<Task>> = ConcurrentHashMap()

    fun cancel(id: Int) = (tasks[id] ?: throw IllegalArgumentException("No task with id $id")).cancel()

    internal fun cancel0(task: Task) {
        synchronized(lock) {
            tasks.remove(task.id)
            Utils.remove(tasksByOwner, task)
        }
    }

    private fun cancel(task: Task) = task.cancel()

    fun cancel(peer: TaskOwner): Int {
        val toRemove = Collections.synchronizedSet(HashSet<Task>())
        toRemove.addAll(tasksByOwner[peer] ?: error(""))
        toRemove.forEach(Consumer { task -> cancel(task) })
        return toRemove.size
    }

    fun run(owner: TaskOwner, task: () -> Unit) = schedule(owner, task, 0)

    fun schedule(owner: TaskOwner, task: () -> Unit, delay: Long, unit: TimeUnit) = schedule(owner, task, delay, 0, unit)

    fun schedule(owner: TaskOwner,
                 task: () -> Unit,
                 delay: Long,
                 period: Long = 0,
                 unit: TimeUnit = TimeUnit.MILLISECONDS): Task {

        val prepared = Task(this, owner, taskCounter.getAndIncrement(), task, delay, period, unit)

        synchronized(lock) {
            tasks[prepared.id] = prepared
            Utils.put(tasksByOwner, owner, prepared)
        }

        owner.executorService.execute(prepared)
        return prepared
    }
}