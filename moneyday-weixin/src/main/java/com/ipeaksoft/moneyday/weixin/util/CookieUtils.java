package com.ipeaksoft.moneyday.weixin.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ipeaksoft.moneyday.core.entity.WeixinSession;
import com.ipeaksoft.moneyday.core.enums.SessionType;
import com.ipeaksoft.moneyday.core.service.RedisClient;
import com.ipeaksoft.moneyday.core.service.WeixinSessionService;

@Service
public class CookieUtils {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String COOKIE_NAME_OPENID = "openid";
	public static final String COOKIE_NAME_IDFA = "idfa";
	public static final String COOKIE_NAME_MOBILE = "mobile";

	public static final int TIMEOUT_SESSION = 30 * 60;

	@Autowired
	RedisClient redisClient;

	private String DOMAIN = "i43.com";

	private int MAX_AGE = 315360000;

	@Autowired
	WeixinSessionService sessionService;

	public void setLocalOpenid(HttpServletRequest request, HttpServletResponse response,
			String openid) {
		String uuid = UUID.randomUUID().toString();
		// bind value & openid
		WeixinSession record = new WeixinSession();
		record.setId(uuid);
		record.setValue(openid);
		record.setType(SessionType.OPENID);
		record.setCreateTime(new Date());
		sessionService.create(record);

		setCookieValue(request, response, COOKIE_NAME_OPENID, uuid);

		redisClient.setObject(uuid, openid);
		redisClient.expire(uuid, TIMEOUT_SESSION);// 设置超时时间

	}

	public void setLocalIDFA(HttpServletRequest request, HttpServletResponse response, String idfa) {
		String uuid = UUID.randomUUID().toString();
		// bind value & idfa
		WeixinSession record = new WeixinSession();
		record.setId(uuid);
		record.setValue(idfa);
		record.setType(SessionType.IDFA);
		record.setCreateTime(new Date());
		sessionService.create(record);

		setCookieValue(request, response, COOKIE_NAME_IDFA, uuid);

		redisClient.setObject(uuid, idfa);
		redisClient.expire(uuid, TIMEOUT_SESSION);// 设置超时时间

	}

	public void setLocalMobile(HttpServletRequest request, HttpServletResponse response,
			String mobile) {
		String uuid = UUID.randomUUID().toString();
		// bind value & mobile
		WeixinSession record = new WeixinSession();
		record.setId(uuid);
		record.setValue(mobile);
		record.setType(SessionType.MOBILE);
		record.setCreateTime(new Date());
		sessionService.create(record);

		setCookieValue(request, response, COOKIE_NAME_MOBILE, uuid);

		redisClient.setObject(uuid, mobile);
		redisClient.expire(uuid, TIMEOUT_SESSION);// 设置超时时间
	}

	public void setCookieValue(HttpServletRequest request, HttpServletResponse response,
			String name, String value) {

		Cookie newCookie = getCookie(request, name);
		if (newCookie == null) {
			newCookie = new Cookie(name, encodeValue(value));
		} else {
			newCookie.setValue(encodeValue(value));
		}
		// newCookie.setDomain(DOMAIN);
		newCookie.setPath("/");
		newCookie.setMaxAge(MAX_AGE);
		response.addCookie(newCookie);
	}

	public String getCookieOpenid(HttpServletRequest request) {
		return getCookieValue(request, COOKIE_NAME_OPENID);
	}

	public String getCookieIDFA(HttpServletRequest request) {
		return getCookieValue(request, COOKIE_NAME_IDFA);
	}

	public String getCookieMobile(HttpServletRequest request) {
		return getCookieValue(request, COOKIE_NAME_MOBILE);
	}

	public String getSessionValue(String uuid) {
		if (StringUtils.isEmpty(uuid)) {
			return null;
		}

		Object obj = redisClient.getObject(uuid);
		if (obj != null) {
			return (String) obj;
		}
		long startTime = System.currentTimeMillis();
		WeixinSession session = sessionService.findByPrimaryKey(uuid);
		long endTime1 = System.currentTimeMillis();
		logger.debug("1 sessionService.findByPrimaryKey spend :" + (endTime1 - startTime) + " ms.");
		if (session == null) {
			return null;
		}
		redisClient.setObject(uuid, session.getValue());
		redisClient.expire(uuid, TIMEOUT_SESSION);// 设置超时时间
		long endTime2 = System.currentTimeMillis();
		logger.debug("2 redis spend :" + (endTime2 - endTime1) + " ms.");
		return session.getValue();
	}

	public String getOpenid(HttpServletRequest request) {
		String cookie_openid = getCookieValue(request, COOKIE_NAME_OPENID);
		if (StringUtils.isEmpty(cookie_openid)) {
			return null;
		}
		return getSessionValue(cookie_openid);
	}

	public String getIDFA(HttpServletRequest request) {
		long startTime = System.currentTimeMillis();
		String cookie_idfa = getCookieValue(request, COOKIE_NAME_IDFA);
		long endTime1 = System.currentTimeMillis();
		logger.debug("get cookie value spend " + (endTime1 - startTime) + " ms.");
		if (StringUtils.isEmpty(cookie_idfa)) {
			return null;
		}
		String sessionValue = getSessionValue(cookie_idfa);
		long endTime2 = System.currentTimeMillis();
		logger.debug("get session value spend " + (endTime2 - endTime1) + " ms.");
		return sessionValue;
	}

	public String getMobile(HttpServletRequest request) {
		String cookie_mobile = getCookieValue(request, COOKIE_NAME_MOBILE);
		if (StringUtils.isEmpty(cookie_mobile)) {
			return null;
		}
		return getSessionValue(cookie_mobile);
	}

	public void clearCookie(HttpServletRequest request, HttpServletResponse response, String name) {
		Cookie cookie = getCookie(request, name);
		if (cookie != null) {
			String uuid = decodeValue(cookie.getValue());
			// cookie.setDomain(DOMAIN);
			cookie.setValue(null);
			cookie.setPath("/");
			cookie.setMaxAge(0);
			response.addCookie(cookie);

			clearSession(uuid);
		}

	}

	public int clearSession(String uuid) {
		if (StringUtils.isEmpty(uuid)) {
			return 0;
		}

		if (redisClient.getObject(uuid) != null) {
			redisClient.delByKey(uuid);
		}

		return sessionService.deleteByPrimaryKey(uuid);
	}

	private Cookie getCookie(HttpServletRequest request, String name) {
		
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookie.getName().equalsIgnoreCase(name)) {
					return cookie;
				}
			}
		}
		return null;
	}

	public String getCookieValue(HttpServletRequest request, String name) {
		Cookie cookie = getCookie(request, name);
		if (cookie != null) {
			return decodeValue(cookie.getValue());
		}
		return null;
	}

	private String encodeValue(String value) {
		try {
			String str = Base64.encodeBase64String(value.getBytes("UTF-8"));
			return "yI" + str;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String decodeValue(String value) {
		if (value == null || value.length() == 0) {
			return null;
		}
		try {
			value = value.substring(2);
			return new String(Base64.decodeBase64(value), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void main(String args[]) throws Exception {
		String b = "otIbzjp9qerhgfIEnxfzpcgXtVOc";
		// String c = encodeValue(b);
		// System.out.println(c);
		// System.out.println(decodeValue(c));

		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] bytes = md5.digest(b.toString().getBytes("UTF-8"));
		StringBuilder sign = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(bytes[i] & 0xFF);
			if (hex.length() == 1) {
				sign.append("0");
			}
			sign.append(hex);
		}
		System.out.println(sign);
	}

}
