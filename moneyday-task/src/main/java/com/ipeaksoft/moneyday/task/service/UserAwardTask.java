package com.ipeaksoft.moneyday.task.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ipeaksoft.moneyday.core.service.UserAwardService;

@Lazy(false)
@Component
public class UserAwardTask {
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	UserAwardService userAwardService;

	// 每月的1号凌晨零点10分1秒执行一次
	@Scheduled(cron = "1 10 0 1 * ?")
	public void execute() {
		logger.info("UserAwardTask start...");
		try {
			userAwardService.monthDo();
		} catch (Exception e) {
			logger.error("ERROR:", e);
		}
		logger.info("UserAwardTask end...");
	}
}
