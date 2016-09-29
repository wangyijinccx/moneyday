package com.ipeaksoft.moneyday.task.service;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.entity.UserValidate;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.core.service.UserValidateService;
import com.ipeaksoft.moneyday.core.util.AppStoreRankUtil;

@Lazy(false)
@Component
public class AppleStoreRankTask {
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	UserService userService;
	@Autowired
	UserValidateService userValidateService;
	@Autowired
	AppStoreRankUtil appStoreRankUtil;

	// 起始执行一次，之后每隔10分钟执行一次
	@Scheduled(cron = "0 */1 * * * ?")
	public void execute() {
		logger.info("AppleStoreRankTask start...");
		try {
			Date date = new Date();
			// 查找出下载到现在够3个小时45分的记录
			List<UserValidate> list = userValidateService
					.selectNeedCheckFor345();
			for (UserValidate validate : list) {
				String appId = validate.getAppId();
				int rank = appStoreRankUtil.queryRankByApp(appId, validate.getAppcate());
				logger.debug("rank:" + rank);
				// rank小于0表示大于1500名
				UserValidate tmp = new UserValidate();
				tmp.setId(validate.getId());
				tmp.setModifyTime(date);

				if (rank > 0) {
					tmp.setRankLater(rank);
					if (validate.getRank() - rank > 150) {
						tmp.setEnable("Y");
					} else {
						tmp.setEnable("N");
					}
				} else {
					tmp.setRankLater(1500);
					tmp.setEnable("N");
				}

				userValidateService.updateByPrimaryKeySelective(tmp);

				if ("Y".equals(tmp.getEnable())) {
					User user = new User();
					user.setMobile(validate.getMobile());
					user.setWeightFlag(2);
//					userService.updateUser(user);
					userService.updateByMobile(user);

				} else if ("N".equals(tmp.getEnable())) {
					User user = new User();
					user.setMobile(validate.getMobile());
					user.setWeightFlag(3);
//					userService.updateUser(user);
					userService.updateByMobile(user);
				}
				logger.debug("AppleStoreRankTask Thead: id: "
						+ validate.getId() + ", appid: " + validate.getAppId()
						+ ", appname: " + validate.getAppname()
						+ ", rankFirst: " + validate.getRank()
						+ ", rankLater: " + rank);
			}

			// 查找出下载到现在够6个小时40分的记录
			list = userValidateService.selectNeedCheckFor640();
			for (UserValidate validate : list) {
				String appId = validate.getAppId();
				int rank = appStoreRankUtil.queryRankByApp(appId, validate.getAppcate());
				logger.debug("rank:" + rank);
				// rank小于0表示大于1500名
				UserValidate tmp = new UserValidate();
				tmp.setId(validate.getId());
				tmp.setModifyTime(date);

				// 判断下载时间够6小时40分后排名是否有变化
				if (rank > 0) {
					tmp.setRankLaterSecond(rank);
					if (validate.getRank() - rank > 150
							|| (validate.getRankLater() != null && validate
									.getRank() - validate.getRankLater() > 150)) {
						tmp.setEnable("Y");
					} else {
						tmp.setEnable("N");
					}
				} else {
					tmp.setRankLaterSecond(1500);
					if (validate.getRankLater() != null
							&& validate.getRank() - validate.getRankLater() > 150) {
						tmp.setEnable("Y");
					} else {
						tmp.setEnable("N");
					}
				}

				userValidateService.updateByPrimaryKeySelective(tmp);

				if ("Y".equals(tmp.getEnable())) {
					User user = new User();
					user.setMobile(validate.getMobile());
					user.setWeightFlag(2);
//					userService.updateUser(user);
					userService.updateByMobile(user);
				} else if ("N".equals(tmp.getEnable())) {
					User user = new User();
					user.setMobile(validate.getMobile());
					user.setWeightFlag(3);
//					userService.updateUser(user);
					userService.updateByMobile(user);
				}
				logger.debug("AppleStoreRankTask Thead: id: "
						+ validate.getId() + ", appid: " + validate.getAppId()
						+ ", appname: " + validate.getAppname()
						+ ", rankFirst: " + validate.getRank()
						+ ", rankLater: " + rank);
			}
		} catch (Exception e) {
			logger.error("ERROR:", e);
		}
		logger.info("AppleStoreRankTask end...");
	}
}
