package com.gmail.marcosav2010.tasker

import com.gmail.marcosav2010.logger.Loggable
import java.util.concurrent.ExecutorService

interface TaskOwner : Loggable {

    val executorService: ExecutorService
}