package com.ipeaksoft.moneyday.task.thread;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class AutoTaskBatMobiThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());
	public static final String URL = "http://bulk2.batmobi.net/api/v2/bulk/campaigns?app_key=E732N36XXSJMU8JZLHZOHWFF&platform=ios&countries=CN";

	TaskAutoService taskAutoService;
	TaskFastService taskFastService;
	AppStoreRankUtil util;

	public AutoTaskBatMobiThread(TaskAutoService taskAutoService, 
			TaskFastService taskFastService,
			AppStoreRankUtil util){
		this.taskAutoService = taskAutoService;
		this.taskFastService = taskFastService;
		this.util = util;
	}

	/**
	 * 执行逻辑：
	 * 1. 从batmobi获取任务，更新tb_task_auto表， 并记录adid入list
	 * 2. 过滤（对于同一个appid有多个adid只取价格最高的一条），先排序（根据adid取出有效的，然后根据appid和价格排序），然后遍历过滤，有效adid存入redis
	 * 3. 针对子渠道分别插入任务
	 */
	@Override
	public void run() {
		logger.info("AutoTaskBatMobiThread start...");
		doJob();
		logger.info("AutoTaskBatMobiThread end...");
	}
	
	private void doJob(){
		try{
			long time = System.currentTimeMillis();
			String result = HttpRequest.sendHttpRequest(URL, "GET", "UTF-8");
			JSONObject json = JSONObject.parseObject(result);
			if (json.getIntValue("status")==200){
				//1. 从batmobi获取任务，更新tb_task_auto表， 并记录adid入list
				List<String> adidList = new ArrayList<String>();
				List<String> prizeAdidList = new ArrayList<String>();
				int incent = 0;
				if (json.getIntValue("total")>0){
					JSONArray array = json.getJSONArray("offers");
					for (int i = 0 ; i < array.size() ; i++){
						JSONObject item = array.getJSONObject(i);
						if (item.containsKey("countries")){
							JSONArray countries = item.getJSONArray("countries");
							if (countries.contains("CN")){
								TaskAuto task = dealTaskAuto(item, incent);
								if (task != null){
									adidList.add(task.getAdid());
									if (incent > 0){
										prizeAdidList.add(task.getAdid());
									}
								}
							}
						}
					}
				}
				
				logger.debug("adidList: "+ adidList);
				Set<String> validAdidSet = new HashSet<String>();
				adidList.forEach(t->validAdidSet.add(t));
				logger.debug("validAdidSet: "+ validAdidSet);
				
				String key = PersistRedisKey.BatMobiOnlineTaskStudio.name();
				//先清空redis里有效adid
				taskAutoService.clearOnlineTask(key);

				//有效adid存入redis
				if (validAdidSet.size()>0){
					taskAutoService.addOnlineTask(key, validAdidSet);
				}
				
				String prize_key = PersistRedisKey.BatMobiOnlineTaskXiguamei.name();
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
				List<TaskAuto> validTaskList = taskAutoService.selectByAdidsAndSource(validAdidSet, AutoTaskSource.BATMOBI);
				for (TaskAuto task: validTaskList){
					dealTaskFast(task, Channel.STUDIO);
					dealTaskFast(task, Channel.XIGUAMEI);
				}
			}
			logger.info("consume time:{}", System.currentTimeMillis()-time);			
		}
		catch (Exception e){
			logger.error("", e);
		}
	}
	

	private TaskAuto dealTaskAuto(JSONObject item, int intent){
		String appid = item.getString("mobile_app_id");
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
			String icon = appinfo.getIcons();
			String[] icons = icon.split("\\|");
//			JSONArray icons = json.getJSONArray("icons");
			if (icons.length>0){
				task.setIcon(icons[0]);
			}
			task.setAppid(appid);
			task.setName(item.getString("name"));
			//task.setCategory(item.getString("category"));
			task.setAdid(item.getString("camp_id"));
			task.setPrice(item.getBigDecimal("payout_amount"));
			task.setUnit(item.getString("payout_currency"));
			task.setClickUrl(item.getString("click_url"));
			task.setSource(AutoTaskSource.BATMOBI);
			task.setTaskType(item.getString("acquisition_flow"));
			task.setModifyTime(new Date());
			task.setDailyCap(item.getInteger("daily_cap"));
			taskAutoService.save(task);
			return task;
		}
		return null;
	}
	
	private void dealTaskFast(TaskAuto record, Channel channel){
		String adid = record.getAdid();
		PersistRedisKey key = null;
		if (channel == Channel.STUDIO){
			key = PersistRedisKey.BatMobiTaskMapStudio;
		}
		else if (channel == Channel.XIGUAMEI){
			key = PersistRedisKey.BatMobiTaskMapXiguamei;
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
			Integer dailyCap = record.getDailyCap();
			if (dailyCap == null){
				task.setTotal(100);
			} else {
				task.setTotal(dailyCap);
			}
			taskFastService.updateByPrimaryKey(task);
		} else {
			task = new TaskFast();
			task.setAdId(record.getAdid());
			task.setImg(record.getIcon());
			task.setTaskname(record.getAppname());
			task.setDescription("(BatMobi自动任务)");
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
			task.setTaskSource(TaskSourceType.BATMOBI.name());			
			task.setChannelName(channel.name());
			taskFastService.addTaskFastGetId(task);
		}
		if (task.getId() != null && task.getId() > 0) {
			taskAutoService.setTaskId(key, record.getAdid(), task.getId());
		}

		if (channel == Channel.STUDIO){
			taskAutoService.setTaskPrice(PersistRedisKey.BatMobiPriceMapStudio, record.getAdid(), record.getPrice()+record.getUnit());
		}
		else if (channel == Channel.XIGUAMEI){
			taskAutoService.setTaskPrice(PersistRedisKey.BatMobiPriceMapXiguamei, record.getAdid(), record.getPrice()+record.getUnit());
		}
	}
	

}
