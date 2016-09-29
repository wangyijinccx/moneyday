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
import com.ipeaksoft.moneyday.task.thread.AutoTaskYeahMobiThread;

@Lazy(false)
@Component
public class AutoTaskYeahMobiService {
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


	@Scheduled(cron = "0 */20 * * * ?")
	public void schedule() {
		logger.info("schedule start...");
		threadPoolTaskExecutor.execute(new AutoTaskYeahMobiThread(taskAutoService, taskFastService, util));
		logger.info("schedule end...");
	}


}
