package com.gmail.marcosav2010.tasker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gmail.marcosav2010.logger.Logger;

public class Task implements Runnable {

	private final Tasker tasker;
	private final int id;
	private String name;
	private final TaskOwner owner;
	private final Runnable task;
	private Thread thread;

	private final long delay;
	private final long period;
	private final AtomicBoolean running = new AtomicBoolean(true);

	public Task(Tasker tasker, TaskOwner owner, int id, Runnable task, long delay, long period, TimeUnit unit) {
		this.tasker = tasker;
		this.owner = owner;
		this.id = id;
		this.task = task;
		this.delay = unit.toMillis(delay);
		this.period = unit.toMillis(period);
	}

	public String getName() {
		return name;
	}
	
	public Task setName(String name) {
		if (name == null)
			throw new IllegalStateException("This task is already named.");
		this.name = name;
		return this;
	}
	
	public int getId() {
		return id;
	}

	public TaskOwner getOwner() {
		return owner;
	}
	
	public Thread getThread() {
		return thread;
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
				Logger.log(t, "Task " + (name == null ? "" : name + " ") + "#" + id + " encountered an exception");
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