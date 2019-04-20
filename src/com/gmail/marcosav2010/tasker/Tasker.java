package com.gmail.marcosav2010.tasker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.gmail.marcosav2010.common.Utils;

public class Tasker {

	private final Object lock = new Object();
	private final AtomicInteger taskCounter = new AtomicInteger();
	private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
	private final Map<TaskOwner, Set<Task>> tasksByOwner = new ConcurrentHashMap<>();

	public void cancel(int id) {
		Task task = tasks.get(id);
		if (task == null)
			throw new IllegalArgumentException("No task with id " + id);

		task.cancel();
	}

	void cancel0(Task task) {
		synchronized (lock) {
			tasks.remove(task.getId());
			Utils.remove(tasksByOwner, task);
		}
	}

	public void cancel(Task task) {
		task.cancel();
	}

	public int cancel(TaskOwner peer) {
		Set<Task> toRemove = Collections.synchronizedSet(new HashSet<>());
		
		tasksByOwner.get(peer).forEach(toRemove::add);

		toRemove.forEach(this::cancel);
		return toRemove.size();
	}

	public Task run(TaskOwner owner, Runnable task) {
		return schedule(owner, task, 0, TimeUnit.MILLISECONDS);
	}

	public Task schedule(TaskOwner owner, Runnable task, long delay, TimeUnit unit) {
		return schedule(owner, task, delay, 0, unit);
	}

	public Task schedule(TaskOwner owner, Runnable task, long delay, long period, TimeUnit unit) {
		if (owner == null)
			throw new IllegalArgumentException("Owner cannot be null ");
		if (task == null)
			throw new IllegalArgumentException("Task runnable cannot be null ");
		
		Task prepared = new Task(this, owner, taskCounter.getAndIncrement(), task, delay, period, unit);

		synchronized (lock) {
			tasks.put(prepared.getId(), prepared);
			Utils.put(tasksByOwner, owner, prepared);
		}

		owner.getExecutorService().execute(prepared);
		return prepared;
	}
}