package com.gmail.marcosav2010.tasker;

import java.util.concurrent.ExecutorService;

import com.gmail.marcosav2010.logger.Loggable;

public interface TaskOwner extends Loggable {

	public ExecutorService getExecutorService();
}
