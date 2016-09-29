package com.ipeaksoft.moneyday.admin.controller;

import java.security.Principal;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ipeaksoft.moneyday.admin.util.MD5Util;
import com.ipeaksoft.moneyday.core.entity.AdminUser;
import com.ipeaksoft.moneyday.core.mapper.UserLoginExceptionMapper;
import com.ipeaksoft.moneyday.core.service.AdminUserService;
import com.ipeaksoft.moneyday.core.service.CompetitorService;
import com.ipeaksoft.moneyday.core.service.UserService;

@Controller
@RequestMapping(value = "/account")
public class AccountController extends BaseController {
	@Autowired
	private UserService UserService;
	@Autowired
	private AdminUserService AdminUserService;
	@Autowired
	private UserLoginExceptionMapper userLoginExceptionService;
	@Autowired
	private CompetitorService competitorService;


	@RequestMapping(value = "/create", method = { RequestMethod.GET })
	public String Account_Create(ModelMap map, Principal principal, HttpServletRequest request) {
		return "/account/create_account";
	}


	@ResponseBody
	@RequestMapping(value = "/create", method = { RequestMethod.POST })
	public String Account_Add(AdminUser record, HttpServletRequest request) {
		String result = "{\"status\":true,\"msg\":\"添加成功\"}";

		record.setCreateTime(new Date());// 指定用户创建时间
		record.setModifyTime(new Date());
		record.setPassword(MD5Util.md5(record.getPassword())); // 加密密码
		record.setIsValid(1);

		try {
			AdminUser existInfo = AdminUserService.getUserByName(record.getUsername());
			if (existInfo != null) {
				return "{\"status\":false,\"msg\":\"用户名已经存在\"}";
			}
			if (AdminUserService.addUser(record) < 1) {
				result = "{\"status\":false,\"msg\":\"添加失败\"}";
			}
		} catch (Exception ex) {
			result = "{\"status\":false,\"msg\":\"添加失败\"}";
		}
		return result;
	}
	
}
