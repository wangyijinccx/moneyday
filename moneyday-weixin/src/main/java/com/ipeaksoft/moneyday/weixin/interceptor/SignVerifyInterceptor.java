package com.ipeaksoft.moneyday.weixin.interceptor;

import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.weixin.service.MD5Util;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;

/**
 * 签名校验拦截器
 */
public class SignVerifyInterceptor extends HandlerInterceptorAdapter {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CONSTANT_ZHANGTONG = "ztwireless";
	private static final String HEADER_SIGN = "sign";

	public static final int RESPONSE_STATUS_403 = 403;

	@Autowired
	CookieUtils cookieUtils;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		JSONObject result = new JSONObject();
		response.setContentType("application/json; charset=utf-8");
		
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
	        String key = (String) headerNames.nextElement();
	        String value = request.getHeader(key);
	        logger.info("header="+key+":"+value);
	    }

		String sign = request.getHeader(HEADER_SIGN);
		String idfa = cookieUtils.getIDFA(request);
		String mobile = cookieUtils.getMobile(request);
		// 如果手机或者idfa为空直接返回异常
		if (StringUtils.isEmpty(mobile) || StringUtils.isEmpty(idfa)) {
			logger.info("账号异常:" + mobile);
			result.put("code", "1001");
			result.put("message", "请退出公众号重新进入");
			PrintWriter out = response.getWriter();
			out.print(result.toString());
			out.flush();
			out.close();
			return false;
		}
		if(StringUtils.isEmpty(sign)) {
			logger.info("签名为空:" + mobile);
			result.put("code", "1012");
			result.put("message", "签名不能为空");
			PrintWriter out = response.getWriter();
			out.print(result.toString());
			out.flush();
			out.close();
			return false;
		}

		try {
			String mySign = MD5Util.md5(mobile + "_" + idfa + "_" + CONSTANT_ZHANGTONG);
			// 如果签名校验通过继续往下走
			if (mySign.toLowerCase().equals(sign.toLowerCase())) {
				return true;
			} else {
				logger.info("账号异常:" + mobile);
				result.put("code", "1002");
				result.put("message", "账号异常");
				PrintWriter out = response.getWriter();
				out.print(result.toString());
				out.flush();
				out.close();
				return false;
			}
		} catch (Exception e) {
			logger.error("ERROR:", e);
			response.setStatus(RESPONSE_STATUS_403);
			result.put("code", "1012");
			result.put("message", "签名错误");
			PrintWriter out = response.getWriter();
			out.print(result.toString());
			out.flush();
			out.close();
			return false;
		}
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler, ModelAndView modelAndView) throws Exception {

		super.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		super.afterCompletion(request, response, handler, ex);
	}
}
