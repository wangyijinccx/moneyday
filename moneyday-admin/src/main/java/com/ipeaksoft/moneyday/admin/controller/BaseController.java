package com.ipeaksoft.moneyday.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ipeaksoft.moneyday.admin.util.security.SpringSecurityUtils;
import com.ipeaksoft.moneyday.core.entity.AdminUser;
import com.ipeaksoft.moneyday.core.service.AdminUserService;

public  class BaseController {
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	protected AdminUserService adminUserService;
  //取得登录用户
  	public  AdminUser getUser(){
  		String name=SpringSecurityUtils.getCurrentUserName();
  		AdminUser user=adminUserService.getUserByName(name);
  		return user;
  	}
}
