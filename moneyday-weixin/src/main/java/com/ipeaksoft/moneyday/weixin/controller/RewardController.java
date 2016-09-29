package com.ipeaksoft.moneyday.weixin.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.LoginAward;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.service.StatCashService;
import com.ipeaksoft.moneyday.core.service.UserAwardService;
import com.ipeaksoft.moneyday.core.service.UserFastService;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;

@Controller
public class RewardController extends BaseController {
	@Autowired
	UserAwardService userAwardService;

	@Autowired
	UserFastService userFastService;
	@Autowired
	StatCashService statCashService;
	@Autowired
	UserService userService;
	@Autowired
	CookieUtils cookieUtils;

	@ResponseBody
	@RequestMapping("goOnLogin")
	public Object goOnLogin() {
		JSONObject result = new JSONObject();
		try {
			String mobile = cookieUtils.getMobile(request);
			if (StringUtils.isEmpty(mobile)) {
				result.put("code", "1008");
				result.put("message", "账号异常！");
			} else {
				User usr = userService.getUserByMobile(mobile);
				if (usr == null) {
					result.put("code", "1008");
					result.put("message", "手机不存在！");
				} else {
					LoginAward loginAward = userAwardService.getLoginAwards(mobile);
					result.put("code", "1000");
					result.put("data", loginAward);
					result.put("message", "success");
				}
			}
		} catch (Exception e) {
			logger.error("ERROR:", e);
			result.put("code", "1001");
			result.put("message", "未知错误");
		}
		logger.info("request url:{}, result:{}", request.getRequestURI(), result);
		return result;
	}

	@ResponseBody
	@RequestMapping("receiveReward")
	public Object receiveReward(String receiveAmount) {
		JSONObject result = new JSONObject();
		try {
			String mobile = cookieUtils.getMobile(request);
			if (StringUtils.isEmpty(mobile)) {
				result.put("code", "1008");
				result.put("message", "账号异常！");
			} else {
				userAwardService.receiveReward(mobile, "0");
				result.put("code", "1000");
				result.put("message", "success");
			}
		} catch (Exception e) {
			logger.error("ERROR:", e);
			result.put("code", "1001");
			result.put("message", "未知错误");
		}
		logger.info("request url:{}, result:{}", request.getRequestURI(), result);
		return result;
	}

}
