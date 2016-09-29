package com.ipeaksoft.moneyday.weixin.thread;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ipeaksoft.moneyday.core.entity.UserLoginHistory;
import com.ipeaksoft.moneyday.core.service.UserLoginHistoryService;
import com.ipeaksoft.moneyday.core.util.AnalysisIpUtil;

public class AddUserLoginHistoryThread implements Runnable {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static boolean isrunning = false;

	private Long userId;

	private String ip;
	
	private UserLoginHistoryService userLoginHistoryService;

	public AddUserLoginHistoryThread(Long userId, String ip, UserLoginHistoryService userLoginHistoryService) {
		
		this.userId = userId;
		this.ip = ip;
		this.userLoginHistoryService = userLoginHistoryService;
	}

	@Override
	public void run() {
		logger.debug("====================================进入AddUserLoginHistoryThread线程"+isrunning);
		if (!isrunning) {
			isrunning = true;
			logger.info("AddUserLoginHistoryThread begain");
			Long begin = System.currentTimeMillis();
			try {
				UserLoginHistory userLoginHistory = new UserLoginHistory();
				Map<String, String> address = AnalysisIpUtil.getAddress(ip);
				String country = address.get("country");
				String area = address.get("area");
				String province = address.get("province");
				String city = address.get("city");
				String county = address.get("county");
				String isp = address.get("isp");
				userLoginHistory.setUserId(userId);
				userLoginHistory.setIp(ip);
				userLoginHistory.setCountry(country);
				userLoginHistory.setArea(area);
				userLoginHistory.setProvince(province);
				userLoginHistory.setCity(city);
				userLoginHistory.setCounty(county);
				userLoginHistory.setIsp(isp);
				this.userLoginHistoryService.addUserLoginHistory(userLoginHistory);
			} catch (Exception e) {
				logger.error("ERROR:", e);
			}
			logger.info("AddUserLoginHistoryThread end, it costs time {}ms", (System.currentTimeMillis() - begin));
			isrunning = false;
		} else {
			logger.info("AddUserLoginHistoryThread is running...");
		}
	}
}
