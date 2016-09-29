package com.ipeaksoft.moneyday.weixin.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.AdminUser;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.entity.UserLoginException;
import com.ipeaksoft.moneyday.core.entity.WeixinSession;
import com.ipeaksoft.moneyday.core.entity.WeixinUser;
import com.ipeaksoft.moneyday.core.service.AdminUserService;
import com.ipeaksoft.moneyday.core.service.HttpService;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.core.service.WeixinSessionService;
import com.ipeaksoft.moneyday.core.service.WeixinUserService;
import com.ipeaksoft.moneyday.core.util.Constant;
import com.ipeaksoft.moneyday.core.util.CreateID;
import com.ipeaksoft.moneyday.weixin.model.BindInfo;
import com.ipeaksoft.moneyday.weixin.model.SMSType;
import com.ipeaksoft.moneyday.weixin.service.ConfirmService;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;

@Controller
public class ProtocolController extends BaseController {
	static String PROXY_URL = "http://123.56.109.73/api";
	@Autowired
	WeixinSessionService sessionService;
	@Autowired
	WeixinUserService weixinUserService;
	@Autowired
	AdminUserService adminUserService;
	@Autowired
	UserService userService;
	@Autowired
	ConfirmService confirmService;
	@Autowired
	CookieUtils cookieUtils;
	@Autowired
	HttpService httpService;

	@ResponseBody
	@RequestMapping("test")
	public Object test() {
		JSONObject result = new JSONObject();
		return result;
	}

	// 发送验证码
	@ResponseBody
	@RequestMapping("openidAndMobile")
	public Object openidAndMobile(String mobile) {
		JSONObject result = new JSONObject();
		result.put("newOrOld", 0);
		if (StringUtils.isNotEmpty(mobile)) {
			User user = userService.getUserByMobile(mobile);
			if (user != null) {
				result.put("newOrOld", 1);
			}
			WeixinUser weixinUser = weixinUserService.findByMobile(mobile);
			String openid = cookieUtils.getOpenid(request);
			if (weixinUser != null && !openid.equals(weixinUser.getOpenid())) {
				result.put("code", "1008");
				result.put("message", "该手机号已经绑定其它微信账户");
			} else {
				boolean sms_result = confirmService.sendCaptcha(mobile,
						SMSType.CONFIRM_AUTHENTICATE_MOBILE);
				if (sms_result) {
					result.put("code", "1000");
					result.put("message", "success");
				} else {
					result.put("code", "1008");
				}
			}
		} else {
			result.put("code", "1008");
		}
		logger.info("request url:{}, result:{}", request.getRequestURI(), result);
		return result;
	}

	/**
	 * 验证，绑定openid&mobile 服务器验证通过之后，根据mobile查找user表是否老用户，
	 * 如果是老用户，先检查此openid是否绑定用户， 如果绑定则比对mobile是否一致，不一致返回错误，一致则什么也不做
	 * 如果没有绑定则将此openid与user绑定，同时将mobile写入cookie 如果不是老用户， 新建用户，并将openid和user绑定。
	 * 同时将mobile写入cookie
	 * 
	 * @param mobile
	 * @param code
	 * @param response
	 * @return
	 */
	@ResponseBody
	@RequestMapping("mobileValidation")
	public Object validate(String mobile, String code, String inviteCode,
			HttpServletResponse response) {
		JSONObject result = new JSONObject();
		String mob = confirmService.checkMobile(code, SMSType.CONFIRM_AUTHENTICATE_MOBILE);
		// String mob = mobile;
		if (!StringUtils.isEmpty(mob) && mob.equals(mobile)) {
			String openid = cookieUtils.getOpenid(request);
			WeixinUser weixinUser = weixinUserService.findByOpenid(openid);
			User user = userService.getUserByMobile(mobile);
			// 老用户
			if (user != null) {
				// 老用户，没有绑定过
				if (weixinUser.getMobile() == null) {
					weixinUser.setUserid(user.getId());
					weixinUser.setMobile(mobile);
					// weixinUser.setIdfa(user.getIdfa());
					weixinUserService.updateByOpenid(weixinUser);
					cookieUtils.setLocalMobile(request, response, mobile);
				} else {
					// 此账户已经绑定别的手机号
					if (!weixinUser.getMobile().equals(mobile)) {
						result.put("code", "1001");
						result.put("message", "bind error");
					}
				}
			} else {
				try {
					user = new User();
					user.setUserid(Long.parseLong(CreateID.generate6()));
					user.setMobile(mobile);
					user.setUsername(mobile);
					user.setStatus(1);// 为正常
					user.setCreateTime(new Date());
					user.setAward(0);
					user.setFromto("offline");// 微信
					user.setType("E");
					user.setIsValid("0");
					if (inviteCode != null && !"".equals(inviteCode)) {
						AdminUser ad = adminUserService.getLevel3UserById(Integer
								.valueOf(inviteCode));
						if (ad == null) {
							inviteCode = null;
						}
					}
					inviteCode = (null == inviteCode) ? Constant.ADMIN_LEVEL3 : inviteCode;
					user.setpUser(inviteCode);
					user.setInviteCode(inviteCode);
					userService.addUser(user);

					weixinUser.setUserid(user.getId());
					weixinUser.setMobile(mobile);
					weixinUserService.updateByOpenid(weixinUser);
					cookieUtils.setLocalMobile(request, response, mobile);
				} catch (Exception e) {
				}
			}
			result.put("code", "1000");
			result.put("message", "success");
			result.put("user", user);
		} else {
			result.put("code", "1011");
			result.put("message", "验证码错误");
		}
		logger.info("request url:{}, result:{}", request.getRequestURI(), result);
		return result;
	}

	@ResponseBody
	@RequestMapping("checkSessionStatus")
	public Object checkSessionStatus() {
		String mobile = cookieUtils.getMobile(request);
		if (StringUtils.isEmpty(mobile)) {
			return assembResult("1004", "手机号异常！");
		}
		WeixinUser weixinUser = weixinUserService.findByMobile(mobile);
		if (weixinUser == null) {
			return assembResult("1004", "手机号异常！");
		}
		String idfa = cookieUtils.getIDFA(request);
		if (StringUtils.isEmpty(idfa)) {
			return assembResult("1004", "IDFA异常！");
		}
		if (!idfa.equals(weixinUser.getIdfa())) {
			return assembResult("1004", "IDFA异常！");
		}
		return assembResult("1000", "OK");
	}

	private JSONObject assembResult(String code, String message) {
		JSONObject result = new JSONObject();
		result.put("code", code);
		result.put("message", message);
		return result;
	}

	// @ResponseBody
	// @RequestMapping("User/**")
	// public Object userProxy() {
	// return proxy(request, false);
	// }
	//
	// @ResponseBody
	// @RequestMapping("speedTask")
	// public Object speedTask() {
	// return proxy(request, false);
	// }
	//
	// @ResponseBody
	// @RequestMapping("speedTaskMessage")
	// public Object speedTaskMessage() {
	// return proxy(request, false);
	// }

	// @ResponseBody
	// @RequestMapping("duiba/billinfo")
	// public Object billinfo() {
	// return proxy(request, false);
	// }

	// @ResponseBody
	// @RequestMapping("newReward")
	// public Object newReward() {
	// return proxy(request, false);
	// }

	// @ResponseBody
	// @RequestMapping("unionTaskPlatform")
	// public Object unionTaskPlatform() {
	// return proxy(request, false);
	// }

	// @ResponseBody
	// @RequestMapping("goOnLogin")
	// public Object goOnLogin() {
	// return proxy(request, false);
	// }

	// @ResponseBody
	// @RequestMapping("receiveReward")
	// public Object receiveReward() {
	// return proxy(request, false);
	// }
	//
	// @ResponseBody
	// @RequestMapping("newAnnounce")
	// public Object newAnnounce() {
	// return proxy(request, false);
	// }

	// @ResponseBody
	// @RequestMapping("announcement")
	// public Object announcement() {
	// return proxy(request, false);
	// }
	//
	// @ResponseBody
	// @RequestMapping("newDynamic")
	// public Object newDynamic() {
	// return proxy(request, false);
	// }
	//
	// @ResponseBody
	// @RequestMapping("announceStatus")
	// public Object announceStatus() {
	// return proxy(request, false);
	// }

	public Object proxy(HttpServletRequest request, boolean needIdfa) {
		logger.info("request url:{}, param:{}", request.getRequestURI(), request.getQueryString());
		String mobile = cookieUtils.getMobile(request);
		String idfa = cookieUtils.getIDFA(request);
		logger.debug("local param, mobile:{}, idfa:{}", mobile, idfa);

		JSONObject check_result = null;
		if (needIdfa) {
			check_result = checkMobileAndIdfa(mobile, idfa);
		} else {
			check_result = checkMobile(mobile);
		}
		if (!check_result.getString("code").equals("1000")) {
			logger.info("return result: {}", check_result);
			return check_result;
		}

		String sUrl = PROXY_URL + request.getRequestURI();
		// String uri = request.getRequestURI();
		// sUrl = sUrl.replace(uri, "/api"+uri);
		Map<String, String> postParams = new HashMap<String, String>();
		postParams.put("mobile", mobile);
		postParams.put("idfa", idfa);
		postParams.put("clientType", "weixin");
		String result = httpService.post(sUrl, postParams);
		// sUrl = sUrl+ "?mobile="+mobile+"&idfa="+idfa;
		// String result = HttpRequest.sendHttpRequest(sUrl, "GET", "UTF-8");
		logger.info("request url:{}, content:{}, result: {}", sUrl, postParams, result);
		return result;
	}

	private JSONObject checkMobile(String mobile) {
		if (StringUtils.isEmpty(mobile)) {
			return assembResult("1004", "手机号异常！");
		}
		JSONObject result = new JSONObject();
		result.put("code", "1000");
		return result;
	}

	private JSONObject checkMobileAndIdfa(String mobile, String idfa) {
		if (StringUtils.isEmpty(mobile)) {
			return assembResult("1004", "手机号异常！");
		}
		if (StringUtils.isEmpty(idfa)) {
			return assembResult("1004", "idfa异常！");
		}
		JSONObject result = new JSONObject();
		result.put("code", "1000");
		return result;
	}

	@ResponseBody
	@RequestMapping("uploadInfo")
	public Object uploadInfo(BindInfo info, HttpServletResponse response) {
		logger.info("uploadInfo param:{}", info);
		JSONObject result = new JSONObject();

		String mobile = info.getMobile();
		if (StringUtils.isEmpty(mobile)) {
			result.put("code", "1001");
			result.put("message", "手机号不能为空！");
			return result;
		}
		if (StringUtils.isEmpty(info.getIdfa())) {
			result.put("code", "1004");
			result.put("message", "idfa不能为空！");
			return result;
		}

		WeixinUser weixinUser = weixinUserService.findByMobile(mobile);
		WeixinUser tmpWeixinUser = new WeixinUser();
		tmpWeixinUser.setOpenid(weixinUser.getOpenid());
		tmpWeixinUser.setIdfa(info.getIdfa());
		weixinUserService.updateByOpenid(tmpWeixinUser);
		cookieUtils.setLocalIDFA(request, response, info.getIdfa());

		User user = userService.getUserByMobile(mobile);
		User tmpUser = new User();
		tmpUser.setMobile(user.getMobile());
		String oldIdfa = user.getIdfa();
		String newIdfa = info.getIdfa();

		/**
		 * 第一种IDFA异常情况：如果非第一次上传IDFA，并且数据库的IDFA与上传的IDFA不一致，该用户就置为IDFA异常
		 */
		if(StringUtils.isEmpty(oldIdfa)) { // idfa为空，说明是第一次上传idfa
			// 将user的idfa设为最新的idfa
			tmpUser.setIdfa(newIdfa);
			userService.updateByMobile(tmpUser);
		} else { // 非第一次上传
			if (!oldIdfa.equals(newIdfa)) {
				if (1 == user.getStatus()) {
					tmpUser.setStatus(2);
					tmpUser.setIdfa(newIdfa);
					userService.updateByMobile(tmpUser);
				}
				UserLoginException exception = new UserLoginException();
				exception.setAppleId(user.getAppleAccount());
				exception.setIdfa(info.getIdfa());
				exception.setMoblie(info.getMobile());
				exception.setCity(info.getCity());
				exception.setProvince(info.getProvince());
				exception.setRegion(info.getRegion());
				exception.setCreateTime(new Date());
				exception.setClientType(info.getClientType());
				userService.insertUserLoginException(exception);
			}
		}
		
		/**
		 * 第二种IDFA异常情况：多个用户的IDFA相同，除了第一个注册的用户不作处理，其他用户全部置为IDFA异常
		 */
		List<User> sameIdfaUsers = userService.getListByIdfa(newIdfa);
		if (sameIdfaUsers.size() > 1) {
			// 从list中去除第一个注册的用户
			User firstRegisterUser = sameIdfaUsers.get(0);
			for (int i = 1; i < sameIdfaUsers.size(); i++) {
				User u = sameIdfaUsers.get(i);
				if (u.getCreateTime().before(firstRegisterUser.getCreateTime())) {
					firstRegisterUser = u;
				}
			}
			sameIdfaUsers.remove(firstRegisterUser);
			for (User u : sameIdfaUsers) {
				User tempU = new User();
				tempU.setMobile(u.getMobile());
				// 只有当用户的状态正常记录为异常，如果已经为异常状态不作处理
				if (u.getStatus() == 1) {
					tempU.setStatus(2);
					userService.updateByMobile(tempU);
					UserLoginException exception = new UserLoginException();
					exception.setAppleId(u.getAppleAccount());
					exception.setIdfa(u.getIdfa());
					exception.setMoblie(u.getMobile());
					exception.setCity(u.getCity());
					exception.setProvince(u.getProvince());
					exception.setRegion("");
					exception.setCreateTime(new Date());
					exception.setClientType(u.getClientType());
					userService.insertUserLoginException(exception);
				}
			}
		}
		result.put("code", "1000");
		result.put("message", "success");
		return result;
	}

	/**
	 * 绑定idfa&openid
	 * 
	 * @param idfa
	 * @param cookie_openid
	 * @return
	 */
	@ResponseBody
	@RequestMapping("bind")
	public Object bindIDFA(String idfa,
			@CookieValue(CookieUtils.COOKIE_NAME_OPENID) String cookie_openid) {
		JSONObject result = new JSONObject();
		if (StringUtils.isEmpty(cookie_openid) || StringUtils.isEmpty(idfa)) {
			result.put("message", "bind param error");
			return result;
		}
		WeixinSession session = sessionService.findByPrimaryKey(cookie_openid);
		if (session == null || session.getValue() == null) {
			result.put("message", "bind param error");
			return result;
		}
		String openid = session.getValue();
		WeixinUser user = new WeixinUser();
		user.setOpenid(openid);
		user.setIdfa(idfa);
		weixinUserService.updateByOpenid(user);
		result.put("message", "bind ok");
		return result;
	}

	@ResponseBody
	@RequestMapping("/User/Bind")
	public Object bind(String mobile, String verify) {
		JSONObject result = new JSONObject();
		if (StringUtils.isEmpty(mobile) || StringUtils.isEmpty(verify)) {
			result.put("message", "bind param error");
			return result;
		}
		// WeixinSession session =
		// sessionService.findByPrimaryKey(cookie_openid);
		// if (session == null || session.getValue() == null){
		// result.put("message", "bind param error");
		// return result;
		// }
		// String openid = session.getValue();
		// WeixinUser user = new WeixinUser();
		// user.setOpenid(openid);
		// user.setIdfa(idfa);
		// userService.updateByOpenid(user);
		result.put("message", "bind ok");
		return result;
	}
	
	/**
	 * 删除一个用户的所有信息（仅为了方便测试提供，不对外提供这个接口）
	 * @param mobile
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/deleteUserAllInfo")
	public Object deleteUserAllInfo(String mobile) {
		JSONObject result = new JSONObject();
		if(null == mobile || "" == mobile) {
			result.put("message", "手机号不能为空");
		} else {
			userService.deleteUserAllInfo(mobile);
			result.put("message", "ok");
		}
		return result;
	}
}
