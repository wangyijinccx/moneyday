package com.ipeaksoft.moneyday.weixin.controller;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ipeaksoft.moneyday.weixin.util.CookieUtils;

@Controller
@RequestMapping({"/cookie"})
public class CookieContrller extends BaseController {

	@Autowired
	CookieUtils cookieUtils;
	
	@ResponseBody
    @RequestMapping({"write"})
	public String write(@RequestParam(value="openid", required=false) String openid, 
						@RequestParam(value="mobile", required=false) String mobile,
						@RequestParam(value="idfa", required=false) String idfa,
						HttpServletResponse response){
    	if (StringUtils.isNotEmpty(openid)){
        	cookieUtils.setLocalOpenid(request, response, openid);
    	}
    	if (StringUtils.isNotEmpty(mobile)){
        	cookieUtils.setLocalMobile(request, response, mobile);
    	}
    	if (StringUtils.isNotEmpty(idfa)){
        	cookieUtils.setLocalIDFA(request, response, idfa);
    	}
		return "OK";
	}
	
	@ResponseBody
    @RequestMapping("/clearAll")
	public String clearAll(HttpServletResponse response){
		cookieUtils.clearCookie(request, response, CookieUtils.COOKIE_NAME_OPENID);
		cookieUtils.clearCookie(request, response, CookieUtils.COOKIE_NAME_MOBILE);
		cookieUtils.clearCookie(request, response, CookieUtils.COOKIE_NAME_IDFA);
		return "OK";
	}
    
	@ResponseBody
    @RequestMapping("/clearOpenid")
	public String clearOpenid(HttpServletResponse response){
		cookieUtils.clearCookie(request, response, CookieUtils.COOKIE_NAME_OPENID);
		return "OK";
	}

	@ResponseBody
    @RequestMapping("/clearMobile")
	public String clearMobile(HttpServletResponse response){
		cookieUtils.clearCookie(request, response, CookieUtils.COOKIE_NAME_MOBILE);
		return "OK";
	}

	@ResponseBody
    @RequestMapping("/clearIdfa")
	public String clearIdfa(HttpServletResponse response){
		cookieUtils.clearCookie(request, response, CookieUtils.COOKIE_NAME_IDFA);
		return "OK";
	}

}
