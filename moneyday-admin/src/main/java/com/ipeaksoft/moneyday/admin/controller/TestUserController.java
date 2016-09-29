package com.ipeaksoft.moneyday.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.service.AdminUserService;

@Controller
@RequestMapping(value = "/admin")
public class TestUserController extends BaseController{
    @Autowired
    private AdminUserService adminUserService;

	@ResponseBody
	@RequestMapping("/test")
	public Object test(String username, String password){
		Map<String, String> map = new HashMap<String, String>();
		map.put("result", "成功");
		return JSONObject.toJSON(map);
	}

}
