package com.ipeaksoft.moneyday.weixin.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ipeaksoft.moneyday.core.entity.WeixinUser;
import com.ipeaksoft.moneyday.core.service.WeixinUserService;
import com.ipeaksoft.moneyday.weixin.model.EventType;
import com.ipeaksoft.moneyday.weixin.model.Message;

@Service
public class MessageHanlerService {
    @Autowired
    WeixinUserService weixinUserService;

	public String handler(Message message){
		if (message == null){
			return null;
		}
		String msgType = message.getMsgType();
		String event = message.getEvent();
		
		if ("event".equals(msgType)){
			if (EventType.SUB.equals(event)){
				String openid = message.getFromUserName();
				WeixinUser user = weixinUserService.findByOpenid(openid);
				
				if (user == null){
					user = new WeixinUser();
					user.setOpenid(openid);
					user.setCreateTime(new Date());
					user.setEnable(true);
					weixinUserService.create(user);
				}
				else{
					user.setEnable(true);
					user.setUpdateTime(new Date());
					weixinUserService.updateByOpenid(user);
				}
			}
			else if (EventType.UNSUB.equals(event)){
				String openid = message.getFromUserName();
				WeixinUser user = weixinUserService.findByOpenid(openid);
				user.setEnable(false);
				user.setUpdateTime(new Date());
				weixinUserService.updateByOpenid(user);
			}
		}
		return "";			
	}

}
