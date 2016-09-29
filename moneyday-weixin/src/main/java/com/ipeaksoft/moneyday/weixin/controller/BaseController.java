package com.ipeaksoft.moneyday.weixin.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseController {
    @Autowired
    protected HttpServletRequest request;
//    @Autowired
//    protected HttpServletResponse response;

    protected Logger logger = null;
    protected String className = "";

    public BaseController(){
        className = getClass().getName();
        logger = LoggerFactory.getLogger(className);
    }
}
