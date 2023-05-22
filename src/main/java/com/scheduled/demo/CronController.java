package com.scheduled.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/jobs")
public class CronController {

    @Autowired
    private JobParamTaskService jobParamTaskService;

    private static final Logger log = LoggerFactory.getLogger(JobParamTaskService.class);

    @GetMapping("/job")
    public String doAmazonJob(@RequestParam String cron) {
        log.info(String.format("------ Trying to update new CRON %s----", cron));
        jobParamTaskService.updateSchedule(cron);
        return "ok";
    }

    @GetMapping("/job/cancel")
    public String cancelAmazonJob() {
        log.info(" ===== Trying to Cancelling Scheduled Task =====");
        jobParamTaskService.cancelFutureSchedulerTask();
        return "ok";
    }
}
