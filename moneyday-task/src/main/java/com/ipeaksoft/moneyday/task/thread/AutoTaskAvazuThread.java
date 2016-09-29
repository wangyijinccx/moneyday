package com.ipeaksoft.moneyday.task.thread;

import java.math.BigDecimal;
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

public class AutoTaskAvazuThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private String url = "http://api.c.avazunativeads.com/s2s?sourceid=18639&incent=2&os=ios&country=CN&market=apple&pagenum=100";

	TaskAutoService taskAutoService;
	TaskFastService taskFastService;
	AppStoreRankUtil util;

	public AutoTaskAvazuThread(TaskAutoService taskAutoService, 
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
		logger.info("start...");
		doJob();
		logger.info("end...");
	}
	

	private void doJob(){
		List<String> adidList = new ArrayList<String>();
		List<String> prizeAdidList = new ArrayList<String>();

		//第一次先假设只有1页 
		int total_page = 2;
		for (int j = 1 ; j < total_page ; j++){
			String offer_url = url+"&page="+j;
			String result = HttpRequest.sendHttpRequest(offer_url, "GET", "utf-8");
			logger.debug("get to url: {}, result: {}", offer_url, result);
			JSONObject json = JSONObject.parseObject(result);
			if ("OK".equals(json.getString("status"))){
				JSONObject ads = json.getJSONObject("ads");
				int total_records = ads.getIntValue("total_records");
				total_page = total_records%100==0?total_records/100:total_records/100+1;
				JSONArray array = ads.getJSONArray("ad");
				for (int i = 0 ; i < array.size() ; i++){
					JSONObject item = array.getJSONObject(i);
					TaskAuto task = dealTaskAuto(item);
					if (task != null){
						adidList.add(task.getAdid());
						if ("1".equals(task.getPrize())){
							prizeAdidList.add(task.getAdid());
						}
					}
					
				}
			}
		}
		
		logger.debug("adidList: "+ adidList);
		Set<String> validAdidSet = new HashSet<String>();
		adidList.forEach(t->validAdidSet.add(t));
//		//过滤（对于同一个appid有多个adid只取价格最高的一条）
//		logger.debug("adidList:{}, prizeAdidList:{}", adidList, prizeAdidList);
//		List<TaskAuto> list = taskAutoService.selectByAdidsAndSource(adidList, AutoTaskSource.AVAZU);
//		Set<String> validAdidSet = new HashSet<String>();
//		String tmpAppid = null;
//		for (TaskAuto task: list){
//			if (!task.getAppid().equals(tmpAppid)){
//				validAdidSet.add(task.getAdid());
//				tmpAppid = task.getAppid();
//			}
//		}
		logger.debug("validAdidSet: "+ validAdidSet);

		String key = PersistRedisKey.AvazuOnlineTaskStudio.name();
		//先清空redis里有效adid
		taskAutoService.clearOnlineTask(key);
		//有效adid存入redis
		taskAutoService.addOnlineTask(key, validAdidSet);
		
		String prize_key = PersistRedisKey.AvazuOnlineTaskXiguamei.name();
		taskAutoService.clearOnlineTask(prize_key);
		if (prizeAdidList.size()>0){
			prizeAdidList.retainAll(validAdidSet);
			if (prizeAdidList.size()>0){
				Set<String> validPrizeAdidSet = new HashSet<String>();
				for (String adid: prizeAdidList){
					validPrizeAdidSet.add(adid);
				}
				//有效adid存入redis
				logger.debug("validPrizeAdidSet: "+ validPrizeAdidSet);
				taskAutoService.addOnlineTask(prize_key, validPrizeAdidSet);
			}
		}

		
		//针对子渠道分别插入任务
		List<TaskAuto> validTaskList = taskAutoService.selectByAdidsAndSource(validAdidSet, AutoTaskSource.AVAZU);
		for (TaskAuto task: validTaskList){
			dealTaskFast(task, Channel.STUDIO);
			if ("1".equals(task.getPrize())){
				dealTaskFast(task, Channel.XIGUAMEI);
			}
		}

	}
	
	private TaskAuto dealTaskAuto(JSONObject item){
		//"preview_url": "https://itunes.apple.com/app/id1044039377?mt=8",
		String appid = item.getString("pkgname");
		AppInfo appinfo = util.queryAppInfo(appid);
		if (appinfo != null){
//		if (json != null && json.containsKey("_id") && json.getString("_id").equals(appid)){
			TaskAuto task = new TaskAuto();
			task.setNoprize("1");
			String incent = item.getString("incent");
			if ("yes".equalsIgnoreCase(incent)){
				task.setPrize("1");
			}
			task.setAppname(appinfo.getName());
			String icon = appinfo.getIcons();
			String[] icons = icon.split("\\|");
//			JSONArray icons = json.getJSONArray("icons");
			if (icons.length>0){
				task.setIcon(icons[0]);
			}
			task.setAppid(appid);
			task.setName(item.getString("title"));
			task.setCategory(item.getString("appcategory"));
			task.setAdid(item.getString("campaignid"));
			String payout = item.getString("payout");
			if (payout.endsWith("$")){
				payout = payout.substring(0, payout.length()-1);
			}
			task.setPrice(new BigDecimal(payout));
			task.setUnit("$");
			task.setClickUrl(item.getString("clkurl"));
//			if (item.containsKey("end_date")){
//				task.setEndTime(item.getString("end_date"));
//			}
//			task.setDailyCap(item.getInteger("remaining_daily_cap"));
			task.setSource(AutoTaskSource.AVAZU);
//			task.setTaskType(item.getString("acquisition_flow"));
			task.setModifyTime(new Date());
			taskAutoService.save(task);
			return task;
		}
		return null;
	}
	
	private void dealTaskFast(TaskAuto record, Channel channel){
		String adid = record.getAdid();
		PersistRedisKey key = null;
		if (channel == Channel.STUDIO){
			key = PersistRedisKey.AvazuTaskMapStudio;
		}
		else if (channel == Channel.XIGUAMEI){
			key = PersistRedisKey.AvazuTaskMapXiguamei;
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
			}
			else{
				task.setTotal(dailyCap);
			}
			taskFastService.updateByPrimaryKey(task);
		} else {
			task = new TaskFast();
			task.setAdId(record.getAdid());
			task.setImg(record.getIcon());
			task.setTaskname(record.getAppname());
			task.setDescription("(Avazu自动任务)");
			task.setDownloadUrl(record.getClickUrl());
			task.setAward((int)(record.getPrice().doubleValue()*100));
			task.setOperator(1);
			Integer dailyCap = record.getDailyCap();
			if (dailyCap == null){
				task.setTotal(100);
			}
			else{
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
			task.setTaskSource(TaskSourceType.Avazu.name());			
			task.setChannelName(channel.name());
			taskFastService.addTaskFastGetId(task);
		}
		if (task.getId() != null && task.getId() > 0) {
			taskAutoService.setTaskId(key, record.getAdid(), task.getId());
		}

		if (channel == Channel.STUDIO){
			taskAutoService.setTaskPrice(PersistRedisKey.AvazuPriceMapStudio, record.getAdid(), record.getPrice()+record.getUnit());
		}
		else if (channel == Channel.XIGUAMEI){
			taskAutoService.setTaskPrice(PersistRedisKey.AvazuPriceMapXiguamei, record.getAdid(), record.getPrice()+record.getUnit());
		}
	}
	

}
