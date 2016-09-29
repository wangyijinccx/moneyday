package com.ipeaksoft.moneyday.task.thread;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

public class AutoTaskWanPuThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());

	TaskAutoService taskAutoService;
	TaskFastService taskFastService;
	AppStoreRankUtil util;

	public AutoTaskWanPuThread(TaskAutoService taskAutoService,
			TaskFastService taskFastService, AppStoreRankUtil util) {
		this.taskAutoService = taskAutoService;
		this.taskFastService = taskFastService;
		this.util = util;
	}

	@Override
	public void run() {
		doJob();
	}

	private void doJob() {
		try {
			long time = System.currentTimeMillis();
			String url = setUrl();
			String result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			logger.debug("result: " + result);
			JSONArray array = JSONArray.parseArray(result);
			String message = array.getJSONObject(0).getString("message");//这个有值代表请求失败
			//广告为空判断？
			if ((null == message  || "".equals(message))) {
				logger.debug("offer size: " + array.size());
				List<String> adidList = new ArrayList<String>();
				List<String> prizeAdidList = new ArrayList<String>();
				int incent = 1;
				for (int i = 0; i < array.size(); i++) {
					JSONObject item = array.getJSONObject(i);
					String deviceType = item.getString("device_type");
					String launchArea = item.getString("launch_area");
					//osVersion 7+ 暂时改为“” 
					if(("ios".equalsIgnoreCase(deviceType) || "iphone".equalsIgnoreCase(deviceType) )
						&& ("".equals(launchArea) || null == launchArea)){
						TaskAuto task = dealTaskAuto(item, incent);
						if (task != null) {
							adidList.add(task.getAdid());
							if (incent > 0){
								prizeAdidList.add(task.getAdid());
							}
						}
					}
				}

				logger.debug("adidList: " + adidList);
				Set<String> validAdidSet = new HashSet<String>();
				adidList.forEach(t -> validAdidSet.add(t));
				logger.debug("validAdidSet: " + validAdidSet);

				String key = PersistRedisKey.WanPuOnlineTaskStudio.name();
				// 先清空redis里有效adid
				taskAutoService.clearOnlineTask(key);

				// 有效adid存入redis
				taskAutoService.addOnlineTask(key, validAdidSet);
				
				//因为都是激励任务  WanPuOnlineTaskStudio = WanPuOnlineTaskXiguamei
				//WanPuOnlineTaskXiguamei可以删除
				String prize_key = PersistRedisKey.WanPuOnlineTaskXiguamei.name();
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

				// 针对子渠道分别插入任务
				List<TaskAuto> validTaskList = taskAutoService
						.selectByAdidsAndSource(validAdidSet,
								AutoTaskSource.WanPu);
				for (TaskAuto task : validTaskList) {
					dealTaskFast(task, Channel.XIGUAMEI);
				}
			}
			logger.info("consume time:{}", System.currentTimeMillis() - time);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	/**
	 * @param item
	 * @param intent
	 *            0：非激励任务 1：激励任务 2：两者皆可
	 * @return
	 */
	private TaskAuto dealTaskAuto(JSONObject item, int intent) {
		String appid = item.getString("storeid");
		String icon_url = item.getString("adicon");
		String appname = item.getString("adtitle");
		String tasksummary  = "";
		String category = "";
		try {
			tasksummary  = URLDecoder.decode(item.getString("taskStep"), "UTF-8");
			tasksummary = tasksummary.replace("\r\n", "");
			category = URLDecoder.decode(item.getString("type"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String adid = item.getString("adv_id");
		String price = item.getString("price");
		String clkurl = item.getString("click_url");
		// 测试 -不走appsotre appname正式用的时候再调
		AppInfo appinfo = util.queryAppInfo(appid);
		if (appinfo != null) {
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
			//task.setAppname(appname);
			task.setIcon(icon_url);
			task.setAppid(appid);
			task.setName(appname);
			task.setCategory(category);
			task.setAdid(adid);
			task.setPrice(new BigDecimal(price));
			task.setUnit("");
			task.setClickUrl(clkurl);
			task.setDescription(tasksummary);
			//task.setDailyCap(100);
			task.setSource(AutoTaskSource.WanPu);
			// task.setTaskType(item.getString("price_model"));
			task.setModifyTime(new Date());
			taskAutoService.save(task);
			return task;
		}
		return null;
	}

	private void dealTaskFast(TaskAuto record, Channel channel) {
		String adid = record.getAdid();
		PersistRedisKey key = null;
		if (channel == Channel.STUDIO) {
			key = PersistRedisKey.WanPuTaskMapStudio;
		} else if (channel == Channel.XIGUAMEI) {
			key = PersistRedisKey.WanPuTaskMapXiguamei;
		}

		long taskId = taskAutoService.getTaskIdByAdid(key, adid);
		TaskFast task = null;
		if (taskId > 0) {
			task = taskFastService.findById(taskId);
			Calendar calendar = Calendar.getInstance();
			task.setStartTime(new Date(calendar.getTimeInMillis()));
			calendar.add(Calendar.DAY_OF_YEAR, +1);
			task.setDescription(record.getDescription());
			task.setEndTime(new Date(calendar.getTimeInMillis()));
			task.setChannelName(channel.name());
			task.setAward((int) (record.getPrice().doubleValue() * 100));
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
			task.setDescription(record.getDescription());
			task.setDownloadUrl(record.getClickUrl());
			task.setAward((int) (record.getPrice().doubleValue() * 100));
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
			task.setTaskSource(TaskSourceType.WanPu.name());
			task.setChannelName(channel.name());
			taskFastService.addTaskFastGetId(task);
		}
		if (task.getId() != null && task.getId() > 0) {
			taskAutoService.setTaskId(key, record.getAdid(), task.getId());
		}

		if (channel == Channel.STUDIO) {
			taskAutoService.setTaskPrice(
					PersistRedisKey.WanPuPriceMapStudio, record.getAdid(),
					record.getPrice() + record.getUnit());
		} else if (channel == Channel.XIGUAMEI) {
			taskAutoService.setTaskPrice(
					PersistRedisKey.WanPuPriceMapXiguamei, record.getAdid(),
					record.getPrice() + record.getUnit());
		}
	}

	public String md5(String str) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			byte[] byteDigest = md.digest();
			int i;
			StringBuffer buf = new StringBuffer("");
			for (int offset = 0; offset < byteDigest.length; offset++) {
				i = byteDigest[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}
			return buf.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;

		}
	}

	public String setUrl() {
		String app_id ="6855053cfc3e0fa0259d480f370a2550";
		String ip ="123.56.109.73";
		//String ip = "106.37.117.201";//测试IP
		String key = md5(app_id+ip);
		String sUrl = "http://api.adsofts.cn/ios/api/ad_api?app_id="+app_id+"&key="+key+"&isSearch=1";
		return sUrl;
	}

}
