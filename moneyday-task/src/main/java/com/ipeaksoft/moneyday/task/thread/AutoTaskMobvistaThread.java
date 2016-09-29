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

public class AutoTaskMobvistaThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());
	public static final String URL = "http://3s.mobvista.com/v3.php?key=f178fa87a88c37135de11fc0707e01dc&platform=ios";

	TaskAutoService taskAutoService;
	TaskFastService taskFastService;
	AppStoreRankUtil util;

	public AutoTaskMobvistaThread(TaskAutoService taskAutoService, 
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
		doJob();
	}
	
	/**
	 * 非激励任务
	 */
	private void doJob(){
		try{
			long time = System.currentTimeMillis();
			String result = HttpRequest.sendHttpRequest(URL, "GET", "UTF-8");
			logger.debug("result: "+result);
			JSONObject json = JSONObject.parseObject(result);
			logger.debug(json.getBooleanValue("sucess")+"---"+json.getIntValue("total_offers_num"));
			if (json.getBooleanValue("sucess") && json.getIntValue("total_offers_num")>0){
				//1. 从batmobi获取任务，更新tb_task_auto表， 并记录adid入list
				JSONArray array = json.getJSONArray("offers");
				logger.debug("offer size: "+array.size());
				List<String> adidList = new ArrayList<String>();
				
				List<String> prizeAdidList = new ArrayList<String>();
				for (int i = 0 ; i < array.size() ; i++){
					JSONObject item = array.getJSONObject(i);
					if (item.containsKey("geo")){
						String geo = item.getString("geo");
						if (geo.indexOf("cn")>=0){
							String traffic_source = item.getString("traffic_source");
							int incent = 0;
							if ("non-incent".equals(traffic_source)){
								incent = 0;
							}
							else{
								if (item.containsKey("exclude_traffic")){
									String exclude_traffic = item.getString("exclude_traffic");
									if (exclude_traffic.indexOf("incent")<0){
										incent = 2;
									}
									else 
										incent = 1;
								}
							}

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
//				List<TaskAuto> test = taskAutoService.selectALL();
//				test.stream().forEach(t->adidList.add(t.getAdid()));
				
				logger.debug("adidList: "+ adidList);
				Set<String> validAdidSet = new HashSet<String>();
				adidList.forEach(t->validAdidSet.add(t));
//				//过滤（对于同一个appid有多个adid只取价格最高的一条）
//				List<TaskAuto> list = taskAutoService.selectByAdidsAndSource(adidList, AutoTaskSource.Mobvista);
//				String tmpAppid = null;
//				for (TaskAuto task: list){
//					if (!task.getAppid().equals(tmpAppid)){
//						validAdidSet.add(task.getAdid());
//						tmpAppid = task.getAppid();
//					}
//				}
				logger.debug("validAdidSet: "+ validAdidSet);
				
				String key = PersistRedisKey.MobvistaOnlineTaskStudio.name();
				//先清空redis里有效adid
				taskAutoService.clearOnlineTask(key);

				//有效adid存入redis
				taskAutoService.addOnlineTask(key, validAdidSet);
				
				String prize_key = PersistRedisKey.MobvistaOnlineTaskXiguamei.name();
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
				List<TaskAuto> validTaskList = taskAutoService.selectByAdidsAndSource(validAdidSet, AutoTaskSource.Mobvista);
				for (TaskAuto task: validTaskList){
					dealTaskFast(task, Channel.STUDIO);
				}
			}
			logger.info("consume time:{}", System.currentTimeMillis()-time);			
		}
		catch (Exception e){
			logger.error("", e);
		}
	}
	

	/**
	 * @param item
	 * @param intent 0：非激励任务 1：激励任务 2：两者皆可
	 * @return
	 */
	private TaskAuto dealTaskAuto(JSONObject item, int intent){
		String appid = null;
		String package_name = item.getString("package_name");
		if (package_name!= null && package_name.indexOf("id")==0){
			appid = package_name.substring(2);
		}
		AppInfo appinfo = util.queryAppInfo(appid);
		if (appinfo != null){
//		JSONObject json = util.queryAppInfo(appid);
//		if (json != null && json.containsKey("_id") && json.getString("_id").equals(appid)){
			TaskAuto task = new TaskAuto();
			if (intent == 0){
				task.setNoprize("1");
			}
			else if(intent == 1){
				task.setPrize("1");
			}
			else if(intent == 2){
				task.setPrize("1");
				task.setNoprize("1");
			}
			task.setAppname(appinfo.getName());
			task.setIcon(item.getString("icon_link"));
			task.setAppid(appid);
			task.setName(item.getString("app_name"));
			task.setCategory(item.getString("app_category"));
			task.setAdid(item.getString("campid"));
			task.setPrice(item.getBigDecimal("price"));
			task.setUnit("$");
			task.setClickUrl(item.getString("tracking_link"));
//			if (item.containsKey("end_date")){
//				task.setEndTime(item.getString("end_date"));
//			}
			task.setDailyCap(item.getInteger("daily_cap"));
			task.setSource(AutoTaskSource.Mobvista);
			task.setTaskType(item.getString("price_model"));
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
			key = PersistRedisKey.MobvistaTaskMapStudio;
		}
		else if (channel == Channel.XIGUAMEI){
			key = PersistRedisKey.MobvistaTaskMapXiguamei;
		}
		
		long taskId = taskAutoService.getTaskIdByAdid(key, adid);
		TaskFast task = null;
		if (taskId > 0) {
			task = taskFastService.findById(taskId);
			Calendar calendar = Calendar.getInstance();
			task.setStartTime(new Date(calendar.getTimeInMillis()));
			calendar.add(Calendar.DAY_OF_YEAR, +1);
			task.setDescription("(Mobvista)");
			task.setEndTime(new Date(calendar.getTimeInMillis()));
			task.setChannelName(channel.name());
			task.setAward((int)(record.getPrice().doubleValue()*100));
			Integer dailyCap = record.getDailyCap();
			if (dailyCap != null && dailyCap == 0){
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
			task.setDescription("(Mobvista)");
			task.setDownloadUrl(record.getClickUrl());
			task.setAward((int)(record.getPrice().doubleValue()*100));
			task.setOperator(1);
			Integer dailyCap = record.getDailyCap();
			if (dailyCap != null && dailyCap == 0){
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
			task.setTaskSource(TaskSourceType.Mobvista.name());			
			task.setChannelName(channel.name());
			taskFastService.addTaskFastGetId(task);
		}
		if (task.getId() != null && task.getId() > 0) {
			taskAutoService.setTaskId(key, record.getAdid(), task.getId());
		}

		if (channel == Channel.STUDIO){
			taskAutoService.setTaskPrice(PersistRedisKey.MobvistaPriceMapStudio, record.getAdid(), record.getPrice()+record.getUnit());
		}
		else if (channel == Channel.XIGUAMEI){
			taskAutoService.setTaskPrice(PersistRedisKey.BatMobiPriceMapXiguamei, record.getAdid(), record.getPrice()+record.getUnit());
		}
	}
	

}
