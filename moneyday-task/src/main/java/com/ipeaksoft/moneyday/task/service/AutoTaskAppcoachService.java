package com.ipeaksoft.moneyday.task.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.ipeaksoft.moneyday.core.service.TaskAutoService;
import com.ipeaksoft.moneyday.core.service.TaskFastService;
import com.ipeaksoft.moneyday.core.util.AppStoreRankUtil;
import com.ipeaksoft.moneyday.task.thread.AutoTaskAppcoachThread;
import com.ipeaksoft.moneyday.task.thread.AutoTaskMobvistaThread;

@Lazy(false)
@Component
public class AutoTaskAppcoachService {
	private Logger logger = LoggerFactory.getLogger(getClass());
	

	@Autowired
	TaskAutoService taskAutoService;
	@Autowired
	TaskFastService taskFastService;
	@Autowired
	AppStoreRankUtil util;

	@Resource(name = "threadPoolTaskExecutor")
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;

	@PostConstruct
	public void init() {
		schedule();
	}


	@Scheduled(cron = "0 */5 * * * ?")
	public void schedule() {
		logger.info("schedule start...");
		threadPoolTaskExecutor.execute(new AutoTaskAppcoachThread(taskAutoService, taskFastService, util));
		logger.info("schedule end...");
	}


}
