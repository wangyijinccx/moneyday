package com.zhangtong.core.appstore.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.zhangtong.core.appstore.service.RedisService;

@RestController
public class TestController extends BaseController{

	@Autowired
	RedisService redis;


    @RequestMapping("test")
    public Object test() {
    	JSONObject result = new JSONObject();
    	result.put("result", 1);
    	return result;
    }

}
