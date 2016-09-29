package com.ipeaksoft.moneyday.weixin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ipeaksoft.moneyday.core.dto.FastClick;
import com.ipeaksoft.moneyday.core.service.TaskFastCallbackService;

/**
 * 针对第三方的回调点击请求，供工作室版调用
 * 2015年5月7日 上午11:29:15
 */
@Controller
public class TaskChannelController extends BaseController {

//	@Autowired
//	TaskFastCallbackService callbackService;
//
//	@ResponseBody
//	@RequestMapping("clickChannel")
//	public void clickChannel(FastClick fastClick, String taskSource) {
//		callbackService.callback(fastClick, taskSource);
//	}

	
}
