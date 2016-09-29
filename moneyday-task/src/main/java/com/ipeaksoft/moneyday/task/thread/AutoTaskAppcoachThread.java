package com.ipeaksoft.moneyday.task.thread;

import java.math.BigDecimal;
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

public class AutoTaskAppcoachThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());

	TaskAutoService taskAutoService;
	TaskFastService taskFastService;
	AppStoreRankUtil util;

	public AutoTaskAppcoachThread(TaskAutoService taskAutoService,
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
			JSONObject json = JSONObject.parseObject(result);
			int limit = json.getInteger("limit");
			String status = json.getString("status");
			//logger.debug(status + "---" + limit);
			if (null != status  && "OK".equalsIgnoreCase(status)) {
				JSONArray array = json.getJSONArray("ads");
				logger.debug("offer size: " + array.size());
				List<String> adidList = new ArrayList<String>();
				for (int i = 0; i < array.size(); i++) {
					JSONObject item = array.getJSONObject(i);
					TaskAuto task = dealTaskAuto(item, 0);
					if (task != null) {
						adidList.add(task.getAdid());
					}
				}

				logger.debug("adidList: " + adidList);
				Set<String> validAdidSet = new HashSet<String>();
				adidList.forEach(t -> validAdidSet.add(t));
				logger.debug("validAdidSet: " + validAdidSet);

				String key = PersistRedisKey.AppcoachOnlineTaskStudio.name();
				// 先清空redis里有效adid
				taskAutoService.clearOnlineTask(key);

				// 有效adid存入redis
				taskAutoService.addOnlineTask(key, validAdidSet);

				// 针对子渠道分别插入任务
				List<TaskAuto> validTaskList = taskAutoService
						.selectByAdidsAndSource(validAdidSet,
								AutoTaskSource.Appcoach);
				for (TaskAuto task : validTaskList) {
					dealTaskFast(task, Channel.STUDIO);
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
		JSONObject detail = item.getJSONObject("detail");
		JSONObject developer = detail.getJSONObject("developer");
		String appstore_url = (String) developer.get("url");
		//String appid = appstore_url
		//		.substring(appstore_url.lastIndexOf("/") + 3);
		String appid = item.getString("pkgname");
		String icon_url = (String) detail.get("icon_url");
		String appname = (String) item.get("title");
		String category = detail.getJSONObject("category").getString("name");
		String adid = item.getString("id");
		JSONArray payouts = item.getJSONArray("payouts");
		String price = "";
		for (int i = 0; i < payouts.size(); i++) {
			JSONObject payout = payouts.getJSONObject(i);
			JSONArray country = payout.getJSONArray("countries");
			if (country.contains("CN")) {
				price = payout.getString("payout");
			}
		}
		String clkurl = item.getString("clkurl");
		String caps = item.getString("caps");
		BigDecimal _cap_n = new BigDecimal(caps);
		BigDecimal _price_n = new BigDecimal(price);
		// 每日预算除以单价取整
		int dailyCap = _cap_n.divideToIntegralValue(_price_n).intValue();
         
		//测试 -不走appsotre  appname正式用的时候再调
		AppInfo appinfo = util.queryAppInfo(appid);
		if (appinfo != null) {
		//if(true){
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
			task.setUnit("$");
			task.setClickUrl(clkurl);
			task.setDailyCap(dailyCap);
			task.setSource(AutoTaskSource.Appcoach);
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
			key = PersistRedisKey.AppcoachTaskMapStudio;
		} else if (channel == Channel.XIGUAMEI) {
			key = PersistRedisKey.AppcoachTaskMapXiguamei;
		}

		long taskId = taskAutoService.getTaskIdByAdid(key, adid);
		TaskFast task = null;
		if (taskId > 0) {
			task = taskFastService.findById(taskId);
			Calendar calendar = Calendar.getInstance();
			task.setStartTime(new Date(calendar.getTimeInMillis()));
			calendar.add(Calendar.DAY_OF_YEAR, +1);
			task.setDescription("(Appcoach)");
			task.setEndTime(new Date(calendar.getTimeInMillis()));
			task.setChannelName(channel.name());
			task.setAward((int) (record.getPrice().doubleValue() * 100));
			Integer dailyCap = record.getDailyCap();
			if (dailyCap != null && dailyCap == 0) {
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
			task.setDescription("(Appcoach)");
			task.setDownloadUrl(record.getClickUrl());
			task.setAward((int) (record.getPrice().doubleValue() * 100));
			task.setOperator(1);
			
			Integer dailyCap = record.getDailyCap();
			if (dailyCap != null && dailyCap == 0) {
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
			task.setTaskSource(TaskSourceType.Appcoach.name());
			task.setChannelName(channel.name());
			taskFastService.addTaskFastGetId(task);
		}
		if (task.getId() != null && task.getId() > 0) {
			taskAutoService.setTaskId(key, record.getAdid(), task.getId());
		}

		if (channel == Channel.STUDIO) {
			taskAutoService.setTaskPrice(
					PersistRedisKey.AppcoachPriceMapStudio, record.getAdid(),
					record.getPrice() + record.getUnit());
		} else if (channel == Channel.XIGUAMEI) {
			taskAutoService.setTaskPrice(
					PersistRedisKey.AppcoachPriceMapXiguamei, record.getAdid(),
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
		String timestamp = Long.toString(System.currentTimeMillis());
		String country = "CN";
		String siteid = "vncNNJba";
		String token = "WTaZkJUdcHHPeJSE";
		String offset = "0";
		String limit = "100";
		String sign = md5("GET/hydra/v3/api/ads"
				+ token + "offset=" + offset + "&limit=" + limit + "&siteid="
				+ siteid + "&ts=" + timestamp);
		String sUrl = "http://api.hydra.appcoachs.net/hydra/v3/api/ads?"
				+ "country=" + country + "&restype=json&pf=ios&offset="
				+ offset + "&limit=" + limit + "&siteid=" + siteid + "&ts="
				+ timestamp + "&sign=" + sign;
		return sUrl;
	}

}
