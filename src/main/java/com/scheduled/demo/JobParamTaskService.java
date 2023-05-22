package com.scheduled.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@Configuration
public class JobParamTaskService implements SchedulingConfigurer {


    private String cron = "0/1 * * * * ?";

    public void setCron(String cron) {
        this.cron = cron;
    }

    private static final Logger log = LoggerFactory.getLogger(JobParamTaskService.class);
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledTaskRegistrar scheduledTaskRegistrar;

    private static final Integer CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS = 5;


    public TaskScheduler fooTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("FOO_SYNC_SCHEDULER");
        scheduler.setPoolSize(5);
        scheduler.setAwaitTerminationSeconds(210);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public synchronized void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        Runnable runnable = () -> System.out.println("Trigger task executed at " + LocalDateTime.now());

        if (this.scheduledTaskRegistrar == null) {
            this.scheduledTaskRegistrar = scheduledTaskRegistrar;
        }

        this.scheduledTaskRegistrar.setTaskScheduler(fooTaskScheduler());

        scheduledFuture = this.scheduledTaskRegistrar
                .getScheduler().schedule(
                        runnable,
                        triggerContext -> {
                            CronTrigger crontrigger = new CronTrigger(cron);
                            return crontrigger.nextExecutionTime(triggerContext);
                        }
                );
    }

    public synchronized void updateSchedule(String updateCron) {
        setCron(updateCron);
        long delayInSeconds = this.scheduledFuture.getDelay(TimeUnit.SECONDS);

        if (delayInSeconds < 0) {
            log.info("Sync run is already in process. New schedule will take effect after the current run");

            if (this.scheduledFuture.isDone()) {
                configureTasks(this.scheduledTaskRegistrar);
                log.info("Scheduled Task is running again!");
            }

        } else if (delayInSeconds < CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS) {
            log.info(
                    "Next sync is less than {} seconds away. after the next run, schedule will automatically be adjusted.",
                    CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS);
        } else {
            log.info("Next sync is more than {} seconds away. scheduledFuture.delay() is {}. Hence cancelling the schedule and rescheduling.", CANCEL_SCHEDULED_TASK_DELAY_THRESHOLD_IN_SECONDS, delayInSeconds);

            boolean cancel = this.scheduledFuture.cancel(false);

            log.info("future.cancel() returned {}. isCancelled() : {} isDone : {}",
                    cancel, scheduledFuture.isCancelled(), scheduledFuture.isDone());

            log.info("Reconfiguring sync for {} with new schedule {}", "job", updateCron);

            configureTasks(this.scheduledTaskRegistrar);
        }
    }

    public void cancelFutureSchedulerTask() {
        boolean isCancelled = this.scheduledFuture.cancel(false);
        if (isCancelled) {
            log.info("Scheduled Task has been cancelled");
        } else {
            log.info("It has not been possible to cancel the task");
        }
    }
}
