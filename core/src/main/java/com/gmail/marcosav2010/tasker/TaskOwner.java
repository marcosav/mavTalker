package com.gmail.marcosav2010.tasker;

import com.gmail.marcosav2010.logger.Loggable;

import java.util.concurrent.ExecutorService;

public interface TaskOwner extends Loggable {

    ExecutorService getExecutorService();
}