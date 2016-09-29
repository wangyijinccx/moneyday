package com.ipeaksoft.moneyday.weixin.interceptor;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;

public class AccountAbnormalCheckInterceptor extends HandlerInterceptorAdapter {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final String PARAMETER_MOBILE = "mobile";

	public static final int RESPONSE_STATUS_403 = 403;
	
	@Autowired
	private CookieUtils cookieUtils;
	
	@Autowired
	private UserService userService;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		
		JSONObject result = new JSONObject();
		response.setContentType("application/json; charset=utf-8");
		String mobile = request.getParameter(PARAMETER_MOBILE);
		if(StringUtils.isEmpty(mobile)) {
			mobile = cookieUtils.getMobile(request);
		}

		try {
			// mobile不为空才进行处理
			if(!StringUtils.isEmpty(mobile)) {
				User user = userService.getUserByMobile(mobile);
				if(null == user) {
					logger.info("账号不存在:" + mobile);
					result.put("code", "1013");
					result.put("message", "账号不存在");
					PrintWriter out = response.getWriter();
					out.print(result.toString());
					out.flush();
					out.close();
					return false;
				}
				Integer status = user.getStatus();
				if(!status.equals(1)) {
					logger.info("账号异常:" + mobile);
					result.put("code", "1004");
					result.put("message", "账号异常");
					PrintWriter out = response.getWriter();
					out.print(result.toString());
					out.flush();
					out.close();
					return false;
				} else {
					//response.reset();
					return true;
				}
			} else {
				response.reset();
				return true;
			}
		} catch (Exception e) {
			logger.error("ERROR:", e);
			response.setStatus(RESPONSE_STATUS_403);
			result.put("code", "1001");
			result.put("message", "未知异常");
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
