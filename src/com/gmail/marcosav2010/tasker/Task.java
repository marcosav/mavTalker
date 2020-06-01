package com.gmail.marcosav2010.tasker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;

import lombok.Getter;

public class Task implements Runnable {

	private final ILog log;

	private final Tasker tasker;
	@Getter
	private final TaskOwner owner;
	@Getter
	private final int id;
	private final Runnable task;
	private final long delay;
	private final long period;
	@Getter
	private String name;
	@Getter
	private Thread thread;

	private final AtomicBoolean running = new AtomicBoolean(true);

	public Task(Tasker tasker, TaskOwner owner, int id, Runnable task, long delay, long period, TimeUnit unit) {
		log = new Log(owner, "Task #" + id);

		this.tasker = tasker;
		this.owner = owner;
		this.id = id;
		this.task = task;
		this.delay = unit.toMillis(delay);
		this.period = unit.toMillis(period);
	}

	public Task setName(String name) {
		if (name == null)
			throw new IllegalStateException("This task is already named.");
		this.name = name;
		return this;
	}

	public void cancel() {
		boolean wasRunning = running.getAndSet(false);

		if (wasRunning)
			tasker.cancel0(this);
	}

	public void cancelNow() {
		thread.interrupt();
		cancel();
	}

	@Override
	public void run() {
		thread = Thread.currentThread();

		if (delay > 0)
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

		while (running.get()) {
			try {
				task.run();
			} catch (Throwable t) {
				log.log(t, "Task " + (name == null ? "" : name + " ") + "#" + id + " encountered an exception");
			}

			if (period <= 0)
				break;

			try {
				Thread.sleep(period);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		cancel();
	}
}