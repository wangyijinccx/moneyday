package com.ipeaksoft.moneyday.task.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.enums.TaskSourceType;
import com.ipeaksoft.moneyday.core.service.LangYiService;
import com.ipeaksoft.moneyday.core.service.TaskFastService;
import com.ipeaksoft.moneyday.core.util.Channel;
import com.ipeaksoft.moneyday.core.util.HttpRequest;

@Lazy(false)
@Component
public class LangYiTask {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final String PLACEHOLDER_MAC = "02:00:00:00:00:00"; //"MAC_TO_BE_REPLACE";
	public static final String PLACEHOLDER_IDFA = "IDFA_TO_BE_REPLACED";

	@Autowired
	TaskFastService taskFastService;
	
	@Autowired
	LangYiService langYiService;

	@PostConstruct
	public void init() {
		execute();
	}

	/**
	 * 每三分钟执行一次(朗亿每5分钟会更新)
	 **/
	@Scheduled(cron = "0 */3 * * * ?")
	public void execute() {
		logger.info("LANGYI start...");
		forLangYi();
		logger.info("LANGYI end...");
	}

	private void forLangYi() {
		String taskUrl = "http://prweike.apptree.com.cn/weike/channelapplist.jsp?idfa=%s&mac=%s&type=1&channelid=159";
		taskUrl = String.format(taskUrl, PLACEHOLDER_IDFA, PLACEHOLDER_MAC);
		HashMap<String, String> header = new HashMap<>();
    	header.put("WkStudioId", "159");
		String apiRes = HttpRequest.sendHttpRequest(taskUrl, "GET", "UTF-8", header);
		logger.info("朗亿任务列表:{}", apiRes);
		@SuppressWarnings("unchecked")
		Map<String, Object> json = JSONObject.parseObject(apiRes, Map.class);
		if (json != null && json.containsKey("items")) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
			
			langYiService.clearOnlineTask();
			
			for (Map<String, Object> item : items) {
				if (item.containsKey("adudid") && item.get("adudid") != null) {
					dealTask( item, Channel.XIGUAMEI );
					dealTask( item, Channel.TAOJINZHE );
				}
			}
			
			logger.info("当前朗亿在线任务:{}", langYiService.getOnlineTask());
		}
	}
	
	private void dealTask(Map<String, Object> item, Channel channel){
		String url = (String) item.get("activeurl");
		String adid = (String) item.get("adudid");
		String icon = (String) item.get("icon");
		String name = (String) item.get("name");
		Integer appid = (Integer) item.get("appid");
		
		int taskId = langYiService.getTaskIdByLangYiAdid(adid, channel);
		if (taskId > 0) {
			logger.info("已创建朗亿任务:{},{}->{}", name, adid, taskId);
			TaskFast task = taskFastService.findById((long)taskId);
			task.setDownloadUrl(url);
			Calendar calendar = Calendar.getInstance();
			task.setStartTime(new Date(calendar.getTimeInMillis()));
			calendar.add(Calendar.DAY_OF_YEAR, +1);
			task.setEndTime(new Date(calendar.getTimeInMillis()));
			switch( channel ){
			case XIGUAMEI:
				task.setChannelName("XIGUAMEI");
				break;
			case TAOJINZHE:
				task.setChannelName("TAOJINZHE");
				break;
			}
			taskFastService.updateByPrimaryKey(task);
		} else {
			logger.info("准备创建朗亿任务:{}, {}", name, adid);
			TaskFast task = new TaskFast();
			task.setAdId(adid);
			task.setImg(icon);
			task.setTaskname(name + "(朗亿自动任务_" + channel.name() + ")");
			task.setDownloadUrl(url);

			task.setDescription("朗逸自动任务");
			task.setAward(100);
			task.setOperator(1);
			task.setTotal(100);
			task.setFinished(0);
			task.setAppid(appid + "");
			task.setCreateTime(new Date(System.currentTimeMillis()));
			Calendar calendar = Calendar.getInstance();
			task.setStartTime(new Date(calendar.getTimeInMillis()));
			calendar.add(Calendar.DAY_OF_YEAR, +1);
			task.setEndTime(new Date(calendar.getTimeInMillis()));
			
			task.setTaskType(101);
			task.setTaskSource(TaskSourceType.LANGYI.name());
			
			switch( channel ){
			case XIGUAMEI:
				task.setChannelName("XIGUAMEI");
				break;
			case TAOJINZHE:
				task.setChannelName("TAOJINZHE");
				break;
			}

			int count = taskFastService.addTaskFastGetId(task);
			logger.info("朗亿新增任务数量:{}", count);
			if (task.getId() != null && task.getId() > 0) {
				langYiService.setTaskId(adid, task.getId(), channel);
				logger.info("朗亿新增任务id:{}", task.getId());
			}
		}
		
		item.put("taskId", taskId);//把i43的任务id加进去
		switch( channel ){
		case XIGUAMEI:
			langYiService.setTaskJson(adid, JSONObject.toJSONString(item), Channel.XIGUAMEI);
			break;
		case TAOJINZHE:
			langYiService.setTaskJson(adid, JSONObject.toJSONString(item), Channel.TAOJINZHE);
			break;
		}
		
		langYiService.addOnlineTask(adid);
	}
}
