package com.example.beinterviewprep.booking;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class LogAndDropPolicy implements RejectedExecutionHandler {

  @Override
  public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
    log.warn(
        "Dropping booking notification: executor saturated (active={}, queued={})",
        executor.getActiveCount(),
        executor.getQueue().size());
  }
}
