package com.ipeaksoft.moneyday.weixin.controller;

import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.entity.WeixinUser;
import com.ipeaksoft.moneyday.core.service.UserLoginHistoryService;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.core.service.WeixinUserService;
import com.ipeaksoft.moneyday.weixin.service.Oauth2Service;
import com.ipeaksoft.moneyday.weixin.thread.AddUserLoginHistoryThread;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;

@Controller
public class HomeContrller extends BaseController {

	@Autowired
	CookieUtils cookieUtils;
	@Autowired
	WeixinUserService weixinUserService;
	@Autowired
	Oauth2Service oauth2Service;
	@Autowired
	UserService userService;
	@Resource(name = "threadPoolTaskExecutor")
	private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
	private UserLoginHistoryService userLoginHistoryService;
	
//    @RequestMapping({"index"})
//	public String index(){
//		String openid = cookieUtils.getCookieOpenid(request);		
//		if (StringUtils.isEmpty(openid)){
//			String url = request.getRequestURL().toString();
//			String backurl = url.substring(0, url.indexOf("index"))+"openid";
//			try {
//				String oauth2Url = new Oauth2Service().getOauth2BaseUrl("TT", backurl);
//				logger.info("Oauth2 url: " + oauth2Url);
//				return "redirect:" + oauth2Url;
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			}
//			return "index";
//		}
//		return "index";
//	}
	
    @RequestMapping("openid")
	public String openid(String code,HttpServletResponse response){
		logger.debug("request param: {}", request.getQueryString());
		if (StringUtils.isEmpty(code)) {
			logger.error("code is null");
//			return new ModelAndView("error");
			return "error";
		}
		JSONObject obj = oauth2Service.getOauth2BaseResult(code);
		String openid = obj.getString("openid");
		String access_token = obj.getString("access_token");
//		String openid = "oznebuPeostK-cwxlu2ra3wt8bdc";
		
		//比对openid是否一致
		String openid_uuid = cookieUtils.getCookieOpenid(request);
		logger.debug("openid_uuid: {}", openid_uuid);
		String openid_local = cookieUtils.getSessionValue(openid_uuid);
		logger.debug("openid: {}, openid_local: {}", openid, openid_local);
		if (!openid.equals(openid_local)){
			cookieUtils.setLocalOpenid(request, response, openid);
			if (StringUtils.isNotEmpty(openid_local)){
				cookieUtils.clearSession(openid_uuid);
			}
		}
		
		WeixinUser user = weixinUserService.findByOpenid(openid);
		if (user == null){
			user = new WeixinUser();
			user.setOpenid(openid);
			user.setCreateTime(new Date());
			user.setEnable(true);
			weixinUserService.create(user);
		}
		JSONObject info = oauth2Service.getOauth2UserinfoResult(openid, access_token);
//		JSONObject info = WechatUtil.getInfo(openid);
		if (info.containsKey("errcode")){
			return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx496a0a7c766067c2&redirect_uri=http://mp.i43.com/openid&response_type=code&scope=snsapi_userinfo#wechat_redirect";
		}
		else{
			WeixinUser userinfo = new WeixinUser();
			userinfo.setOpenid(openid);
			String nickname = info.getString("nickname");
			userinfo.setNickname(null==nickname ? "":nickname);
			Short sex = info.getShort("sex");
			userinfo.setSex(null==sex ? 0:sex);
			String language = info.getString("language");
			userinfo.setLanguage(null==language ? "":language);
			String city = info.getString("city");
			userinfo.setCity(null==city ? "":city);
			String province = info.getString("province");
			userinfo.setProvince(null==province ? "":province);
			String country = info.getString("country");
			userinfo.setCountry(null==country ? "":country);
			String headimgurl = info.getString("headimgurl");
 			userinfo.setHeadimgurl(null==headimgurl ? "":headimgurl);
 			String privilege = info.getString("privilege");
			userinfo.setPrivilege(null==privilege ? "":privilege);
			weixinUserService.updateByOpenid(userinfo);
		}
		
		//比对本地mobile
		String mobile = user.getMobile();
		if (StringUtils.isNotEmpty(mobile)){
			String mobile_uuid = cookieUtils.getCookieMobile(request);
			String mobile_local = cookieUtils.getSessionValue(mobile_uuid);
			logger.debug("mobile: {}, mobile_local: {}", mobile, mobile_local);
			if (!mobile.equals(mobile_local)){
				cookieUtils.setLocalMobile(request, response, mobile);
				if (StringUtils.isNotEmpty(mobile_local)){
					cookieUtils.clearSession(mobile_uuid);
				}
			}
		}
		
		//比对本地idfa
		String idfa = user.getIdfa();
		if (StringUtils.isNotEmpty(idfa)){
			String idfa_uuid = cookieUtils.getCookieIDFA(request);
			String idfa_local = cookieUtils.getSessionValue(idfa_uuid);
			logger.debug("idfa: {}, idfa_local: {}", idfa, idfa_local);
			if (!idfa.equals(idfa_local)){
				cookieUtils.setLocalIDFA(request, response, idfa);
				if (StringUtils.isNotEmpty(idfa_local)){
					cookieUtils.clearSession(idfa_uuid);
				}
			}
		}
		
		String ip = "";
		if (null == request.getHeader("X-Real-IP")) {
			ip = request.getRemoteAddr();
		} else {
			ip = request.getHeader("X-Real-IP");
		}
		User u=userService.getWeixinUserByMobile(mobile);
		if(u!=null){
			threadPoolTaskExecutor.execute(new AddUserLoginHistoryThread(Long.valueOf(u.getId()), ip, userLoginHistoryService));
		}
		
		//判断用户是否绑定手机号， 如果没有，需要跳绑定页面，否则直接进主界面
		String url = null;
		if (StringUtils.isEmpty(mobile)){
			url = "redirect:/index.html#?no_mobile=true";
		}
		else{
			url = "redirect:/index.html";
		}
		return url;
	}
    
    @RequestMapping("userinfo")
    public String userinfo(){
    	
    	return null;
    }
    
    
    @RequestMapping("openidtest")
	public String openidtest(String openid,HttpServletResponse response){		
		//比对openid是否一致
		String openid_uuid = cookieUtils.getCookieOpenid(request);
		logger.debug("openid_uuid: {}", openid_uuid);
		String openid_local = cookieUtils.getSessionValue(openid_uuid);
		logger.debug("openid: {}, openid_local: {}", openid, openid_local);
		if (!openid.equals(openid_local)){
			cookieUtils.setLocalOpenid(request, response, openid);
			if (StringUtils.isNotEmpty(openid_local)){
				cookieUtils.clearSession(openid_uuid);
			}
		}
		
		WeixinUser user = weixinUserService.findByOpenid(openid);
		if (user == null){
			user = new WeixinUser();
			user.setOpenid(openid);
			user.setCreateTime(new Date());
			user.setEnable(true);
			weixinUserService.create(user);
		}
		
		//比对本地mobile
		String mobile = user.getMobile();
		if (StringUtils.isNotEmpty(mobile)){
			String mobile_uuid = cookieUtils.getCookieMobile(request);
			String mobile_local = cookieUtils.getSessionValue(mobile_uuid);
			logger.debug("mobile: {}, mobile_local: {}", mobile, mobile_local);
			if (!mobile.equals(mobile_local)){
				cookieUtils.setLocalMobile(request, response, mobile);
				if (StringUtils.isNotEmpty(mobile_local)){
					cookieUtils.clearSession(mobile_uuid);
				}
			}
		}
		
		//比对本地idfa
		String idfa = user.getIdfa();
		if (StringUtils.isNotEmpty(idfa)){
			String idfa_uuid = cookieUtils.getCookieIDFA(request);
			String idfa_local = cookieUtils.getSessionValue(idfa_uuid);
			logger.debug("idfa: {}, idfa_local: {}", idfa, idfa_local);
			if (!idfa.equals(idfa_local)){
				cookieUtils.setLocalIDFA(request, response, idfa);
				if (StringUtils.isNotEmpty(idfa_local)){
					cookieUtils.clearSession(idfa_uuid);
				}
			}
		}
		
		//判断用户是否绑定手机号， 如果没有，需要跳绑定页面，否则直接进主界面
		String url = null;
		if (StringUtils.isEmpty(mobile)){
			url = "redirect:/index.html#?no_mobile=true";
		}
		else{
			url = "redirect:/index.html";
		}
		return url;
	}

}
