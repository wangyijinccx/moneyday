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

public class AutoTaskYeahMobiThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());
	TaskAutoService taskAutoService;
	TaskFastService taskFastService;
	AppStoreRankUtil util;

	public AutoTaskYeahMobiThread(TaskAutoService taskAutoService, 
			TaskFastService taskFastService,
			AppStoreRankUtil util){
		this.taskAutoService = taskAutoService;
		this.taskFastService = taskFastService;
		this.util = util;
	}

	@Override
	public void run() {
		logger.info("start...");
		doJob();
		logger.info("end...");
	}
	
	private void doJob() {
		List<String> adidList = new ArrayList<String>();
		String url = "http://api.yeahmobi.com/v1/Apps/get?api_token=4a73f767530&devapp_id=97"
				+ "&filters[product_category][$eq]=App%20Download&filters[product_category_secondary][$eq]=Itune&filters[countries][$eq]=CN";
		String result = HttpRequest.sendHttpRequest(url, "GET", "utf-8");
		logger.debug("get to:{}, result:{}", url, result);
		JSONObject json = JSONObject.parseObject(result);
		if ("success".equals(json.getString("flag"))) {
			JSONArray tables = json.getJSONArray("data");
			for (int i = 0; i < tables.size(); i++) {
				JSONObject item = tables.getJSONObject(i);
				TaskAuto task = dealTaskAuto(item, 0);
				if (task != null) {
					adidList.add(task.getAdid());
				}
			}
		}

		logger.debug("adidList: " + adidList);
		Set<String> validAdidSet = new HashSet<String>();
		adidList.forEach(t -> validAdidSet.add(t));
		logger.debug("validAdidSet: " + validAdidSet);

		String key = PersistRedisKey.YeahMobiOnlineTaskStudio.name();
		// 先清空redis里有效adid
		taskAutoService.clearOnlineTask(key);

		// 有效adid存入redis
		taskAutoService.addOnlineTask(key, validAdidSet);

		// 针对子渠道分别插入任务
		List<TaskAuto> validTaskList = taskAutoService.selectByAdidsAndSource(
				validAdidSet, AutoTaskSource.YEAHMOBI);
		List<String> fianlAppid = new ArrayList<String>();
		for (TaskAuto task : validTaskList) {
			String appid = task.getAppid();
			if(!fianlAppid.contains(appid)){
				dealTaskFast(task, Channel.STUDIO);
				fianlAppid.add(appid);
			}
		}
	}
	
	private TaskAuto dealTaskAuto(JSONObject item, int intent){
		JSONArray offers = item.getJSONArray("offers");
		JSONObject offerPriceMax = null;
		for(int i = 0; i < offers.size(); i++){
			JSONObject offer= offers.getJSONObject(i);
			JSONObject targeting = offer.getJSONObject("targeting");
			JSONArray devices = targeting.getJSONArray("devices");
			Boolean isIPhone = false;
			if(null == devices){
				isIPhone = true;
			}else{
				//支持iphone
				for(int m = 0; m < devices.size(); m++){
					String device = devices.getString(m);
					int indexOf = device.indexOf("iPhone");
					if(indexOf != -1){
						isIPhone = true;
						break;
					}
				}
			}
			//单价最高
			if(isIPhone){
				JSONObject financials = offer.getJSONObject("financials");
				BigDecimal payout = financials.getBigDecimal("payout");
				if( i == 0){
					offerPriceMax = offer;
				}else{
					JSONObject financialsMax = offerPriceMax.getJSONObject("financials");
					BigDecimal payoutMax = financialsMax.getBigDecimal("payout");
					int ch = payoutMax.compareTo(payout);
					if(-1 == ch){
						offerPriceMax = offer;
					}
				}
			}
		}
		if(null != offerPriceMax){
			JSONObject financials = offerPriceMax.getJSONObject("financials");
			BigDecimal payout = financials.getBigDecimal("payout");
			Integer capDaily = financials.getInteger("cap_daily");
			String trackingLink =  offerPriceMax.getString("tracking_link");
			String offerId =  offerPriceMax.getString("offer_id");
			String offerName =  offerPriceMax.getString("offer_name");
			String app_url = item.getString("app_url");
			int fromIndex = app_url.indexOf("/id");
			int endIndex = app_url.indexOf("?");
			if (endIndex <= 0){
				endIndex = app_url.length();
			}
			String appid = app_url.substring(fromIndex+3, endIndex);
			AppInfo appinfo = util.queryAppInfo(appid);
			if (appinfo != null){
				TaskAuto task = new TaskAuto();
				if (intent == 1){
					task.setPrize("1");
				}
				else{
					task.setNoprize("1");
				}
				task.setAppname(appinfo.getName());
				String icon = appinfo.getIcons();
				String[] icons = icon.split("\\|");
				if (icons.length>0){
					task.setIcon(icons[0]);
				}
				task.setAppid(appid);
				task.setName(offerName);
				task.setAdid(offerId);
				task.setPrice(payout);
				task.setUnit("$");
				task.setClickUrl(trackingLink);
				task.setDailyCap(capDaily);
				task.setSource(AutoTaskSource.YEAHMOBI);
				task.setModifyTime(new Date());
				taskAutoService.save(task);
				return task;
			}
		}
		return null;
	}
	
	private void dealTaskFast(TaskAuto record, Channel channel){
		String adid = record.getAdid();
		PersistRedisKey key = null;
		if (channel == Channel.STUDIO){
			key = PersistRedisKey.YeahMobiTaskMapStudio;
		}
		else if (channel == Channel.XIGUAMEI){
			key = PersistRedisKey.YeahMobiTaskMapXiguamei;
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
			task.setDescription("(YeahMobi自动任务)");
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
			task.setTaskSource(TaskSourceType.YeahMOBI.name());			
			task.setChannelName(channel.name());
			taskFastService.addTaskFastGetId(task);
		}
		if (task.getId() != null && task.getId() > 0) {
			taskAutoService.setTaskId(key, record.getAdid(), task.getId());
		}

		if (channel == Channel.STUDIO){
			taskAutoService.setTaskPrice(PersistRedisKey.YeahMobiPriceMapStudio, record.getAdid(), record.getPrice()+record.getUnit());
		}
		else if (channel == Channel.XIGUAMEI){
			taskAutoService.setTaskPrice(PersistRedisKey.YeahMobiPriceMapXiguamei, record.getAdid(), record.getPrice()+record.getUnit());
		}
	}
	

}
