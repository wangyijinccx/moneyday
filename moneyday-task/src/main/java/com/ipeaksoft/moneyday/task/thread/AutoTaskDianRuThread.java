package com.ipeaksoft.moneyday.task.thread;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.appstore.entity.AppInfo;
import com.ipeaksoft.moneyday.core.entity.TaskAuto;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.enums.AutoTaskSource;
import com.ipeaksoft.moneyday.core.enums.TaskSourceType;
import com.ipeaksoft.moneyday.core.service.TaskAutoService;
import com.ipeaksoft.moneyday.core.service.TaskFastService;
import com.ipeaksoft.moneyday.core.util.AppStoreRankUtil;
import com.ipeaksoft.moneyday.core.util.Channel;
import com.ipeaksoft.moneyday.core.util.HttpRequest;
import com.ipeaksoft.moneyday.core.util.PersistRedisKey;

public class AutoTaskDianRuThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());
	public static final String URL = "http://api.mobile.dianru.com/ads_fast_api/show.do?source=xiguamei&device=iPhone&osver=9.3&idfa=2641E9EF-A387-4664-A2B5-59B0XIGUAMEI";

	TaskAutoService taskAutoService;
	TaskFastService taskFastService;
	AppStoreRankUtil util;

	public AutoTaskDianRuThread(TaskAutoService taskAutoService, 
			TaskFastService taskFastService,
			AppStoreRankUtil util){
		this.taskAutoService = taskAutoService;
		this.taskFastService = taskFastService;
		this.util = util;
	}

	@Override
	public void run() {
		logger.info("AutoTaskDianRuThread start...");
		doJob();
		logger.info("AutoTaskDianRuThread end...");
	}
	
	private void doJob(){
		try{
			long time = System.currentTimeMillis();
			String result = HttpRequest.sendHttpRequest(URL, "GET", "UTF-8");
			//System.out.println("-------------------------result="+result);
			JSONObject json = JSONObject.parseObject(result);
			JSONArray tables = json.getJSONArray("table");
			String key = PersistRedisKey.DianRuOnlineTaskStudio.name();
			String prize_key = PersistRedisKey.DianRuOnlineTaskXiguamei.name();
			if (tables.size()>0){
				List<String> adidList = new ArrayList<String>();
				List<String> prizeAdidList = new ArrayList<String>();
				int incent = 2;
				for (int i = 0; i < tables.size(); i++) {
					JSONObject item = tables.getJSONObject(i);
					int runtime = item.getInteger("runtime");
					if (runtime <= 300) {
						TaskAuto task = dealTaskAuto(item, incent);
						if (task != null) {
							adidList.add(task.getAdid());
							if (incent > 0) {
								prizeAdidList.add(task.getAdid());
							}
						}
					}
				}
				
				logger.debug("adidList: "+ adidList);
				Set<String> validAdidSet = new HashSet<String>();
				adidList.forEach(t->validAdidSet.add(t));
				logger.debug("validAdidSet: "+ validAdidSet);
				
				//先清空redis里有效adid
				taskAutoService.clearOnlineTask(key);

				//有效adid存入redis
				if (validAdidSet.size()>0){
					taskAutoService.addOnlineTask(key, validAdidSet);
				}
				
				if (prizeAdidList.size()>0){
					prizeAdidList.retainAll(validAdidSet);
					if (prizeAdidList.size()>0){
						Set<String> validPrizeAdidSet = new HashSet<String>();
						for (String adid: prizeAdidList){
							validPrizeAdidSet.add(adid);
						}
						//先清空redis里有效adid
						taskAutoService.clearOnlineTask(prize_key);
						//有效adid存入redis
						taskAutoService.addOnlineTask(prize_key, validPrizeAdidSet);
					}
					else{
						taskAutoService.clearOnlineTask(prize_key);
					}
				}
				else{
					taskAutoService.clearOnlineTask(prize_key);
				}
				
				//针对子渠道分别插入任务
				List<TaskAuto> validTaskList = taskAutoService.selectByAdidsAndSource(validAdidSet, AutoTaskSource.DianRu);
				for (TaskAuto task: validTaskList){
					dealTaskFast(task, Channel.STUDIO);
					dealTaskFast(task, Channel.XIGUAMEI);
				}
			}else{
				//清空redis里有效adid
				taskAutoService.clearOnlineTask(key);
				taskAutoService.clearOnlineTask(prize_key);
			}
			logger.info("consume time:{}", System.currentTimeMillis()-time);			
		}
		catch (Exception e){
			logger.error("", e);
		}
		//System.out.println("------------------------");
	}
	

	private TaskAuto dealTaskAuto(JSONObject item, int intent){
		String appid = item.getString("appstoreid");
		String text2 = item.getString("text2");
		//keyword = keyword + 排名。西瓜妹只取关键词，工作室取关键词+排名
		String keyWord =null;
		if(StringUtils.isNotBlank(item.getString("keywords"))){
			 keyWord = item.getString("keywords")+"-"+item.getString("aso_pos");
		}
		String description  = text2;
		AppInfo appinfo = util.queryAppInfo(appid);
		if (appinfo != null){
			TaskAuto task = new TaskAuto();
			if (intent == 0) {
				task.setNoprize("1");
			} else if (intent == 1) {
				task.setPrize("1");
			} else if (intent == 2) {
				task.setPrize("1");
				task.setNoprize("1");
			}
			task.setAppname(appinfo.getName());
			task.setAppid(appid);
			task.setName(item.getString("title"));
			task.setAdid(item.getString("adid"));
			task.setPrice(item.getBigDecimal("price"));
			task.setUnit("");
			task.setClickUrl(item.getString("url"));
			task.setSource(AutoTaskSource.DianRu);
			task.setModifyTime(new Date());
			task.setDailyCap(item.getInteger("remain"));
			task.setIcon(item.getString("icon"));
			task.setDescription(description);
			task.setKeyWord(keyWord);
			taskAutoService.save(task);
			return task;
		}
		return null;
	}
	
	private void dealTaskFast(TaskAuto record, Channel channel){
		String adid = record.getAdid();
		PersistRedisKey key = null;
		if (channel == Channel.STUDIO){
			key = PersistRedisKey.DianRuTaskMapStudio;
		}
		else if (channel == Channel.XIGUAMEI){
			key = PersistRedisKey.DianRuTaskMapXiguamei;
		}
		
		long taskId = taskAutoService.getTaskIdByAdid(key, adid);
		TaskFast task = null;
		if (taskId > 0) {
			task = taskFastService.findById(taskId);
			Calendar calendar = Calendar.getInstance();
			task.setStartTime(new Date(calendar.getTimeInMillis()));
			calendar.add(Calendar.DAY_OF_YEAR, +1);
			task.setEndTime(new Date(calendar.getTimeInMillis()));
			task.setChannelName(channel.name());
			task.setAward((int)(record.getPrice().doubleValue()*100));
			task.setDescription(record.getDescription());
			Integer dailyCap = record.getDailyCap();
			if (dailyCap == null){
				task.setTotal(100);
			} else {
				task.setTotal(dailyCap);
			}
			task.setKeyWord(record.getKeyWord());
			taskFastService.updateByPrimaryKey(task);
		} else {
			task = new TaskFast();
			task.setAdId(record.getAdid());
			task.setImg(record.getIcon());
			task.setTaskname(record.getAppname());
			task.setDescription(record.getDescription());
			task.setDownloadUrl(record.getClickUrl());
			task.setAward((int)(record.getPrice().doubleValue()*100));
			task.setOperator(1);
			Integer dailyCap = record.getDailyCap();
			if (dailyCap == null){
				task.setTotal(100);
			} else {
				task.setTotal(dailyCap);
			}
			task.setFinished(0);
			task.setAppid(record.getAppid());
			task.setCreateTime(new Date(System.currentTimeMillis()));
			Calendar calendar = Calendar.getInstance();
			task.setStartTime(new Date(calendar.getTimeInMillis()));
			calendar.add(Calendar.DAY_OF_YEAR, +1);
			task.setEndTime(new Date(calendar.getTimeInMillis()));
			task.setTaskType(102);
			task.setTaskSource(TaskSourceType.DIANRU.name());			
			task.setChannelName(channel.name());
			task.setKeyWord(record.getKeyWord());
			task.setDuplicate(true);
			task.setActiveUpload(true);
			taskFastService.addTaskFastGetId(task);
		}
		if (task.getId() != null && task.getId() > 0) {
			taskAutoService.setTaskId(key, record.getAdid(), task.getId());
		}

		if (channel == Channel.STUDIO){
			taskAutoService.setTaskPrice(PersistRedisKey.DianRuPriceMapStudio, record.getAdid(), record.getPrice()+record.getUnit());
		}
		else if (channel == Channel.XIGUAMEI){
			taskAutoService.setTaskPrice(PersistRedisKey.DianRuPriceMapXiguamei, record.getAdid(), record.getPrice()+record.getUnit());
		}
	}
	

}
