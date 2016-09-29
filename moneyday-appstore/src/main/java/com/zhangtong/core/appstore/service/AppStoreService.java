package com.zhangtong.core.appstore.service;

import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhangtong.core.appstore.entity.AppInfo;
import com.zhangtong.core.appstore.vo.AppRank;

@Service
public class AppStoreService extends BaseService {
	private int RANK_MIN = 1000;
	private int RANK_MAX = 1500;
	private int APP_MAX_SIZE = 20 * 1024 * 1024;
	// {医疗6020, 天气6001, 商品指南6022, 美食佳饮6023, 摄影与录像6008, 健康健美6013}
	private String[] CATS = { "6020", "6001", "6022", "6023" }; 

//	private String KEY_APPLE_RANK = "moneyday_apple_rank";
//	private String KEY_APPLE_CATE = "moneyday_apple_cate";
	private String KEY_APPLE_RANK = "appstore:rank";
	private String KEY_APPLE_CATE = "appstore:cate";

	private ReentrantLock lock = new ReentrantLock();

	@Autowired
	private RedisService redisClient;
	@Autowired
	RankingService rankingService;

	/**
	 * 指定分类和排名，顺序分配app，最多获取5次，还没有取到就返回为空
	 * @param cat
	 * @param rank
	 * @return
	 */
	public AppRank assign() {
		Integer size = APP_MAX_SIZE + 1;
		AppRank appRank = null;
		try {
			RankParam param = null;
			boolean switchCat = false;
			int times = 0;
			while (times <5) {
				times++;
				param = getRankParam(switchCat);
				AppRank tmpAppRank = queryAppInfo(param.getCat(), param.getRank());
				logger.debug("switchCat:{}, get cat:{}, rank:{}, result:{}", switchCat, param.getCat(), param.getRank(), appRank!= null?(appRank.getAppid()+"-"+appRank.getName()):" NULL");
				if (tmpAppRank != null) {
					switchCat = false;
					
					size = tmpAppRank.getSize().intValue();
					if (0 == size || size >= APP_MAX_SIZE){
						continue;
					}
					else{
						String usedAppIdKey = generateKey(tmpAppRank.getAppid().intValue());
						if(redisClient.exists(usedAppIdKey)) {
							continue;
						} else {
							appRank = tmpAppRank;
							break;
						}
					}
				} else {
					switchCat = true;
				}
			}
			if (appRank != null){
				String key = generateKey(appRank.getAppid().intValue());
				redisClient.setInteger(key, appRank.getAppid().intValue());
				redisClient.expire(key, 60 * 60 * 6 + 60 * 40);
			}
		} catch (Exception e) {
			logger.error("ERROR:", e);
			return null;
		}

		return appRank;
	}
	
	/**
	 * 删除分配
	 * @param appid
	 */
	public void assignDel(String appid) {
		String key = generateKey(Integer.valueOf(appid));
		redisClient.del(key);
	}
	
	private RankParam getRankParam(boolean switchCat) {
		lock.lock();
		RankParam param = new RankParam();
		// 初始化redis的值
		Integer rank = redisClient.getInteger(KEY_APPLE_RANK);
		if (rank == null) {
			redisClient.setInteger(KEY_APPLE_RANK, RANK_MIN);// 没有则新增
			rank = RANK_MIN;
		} else {
			redisClient.incr(KEY_APPLE_RANK);
			rank++;
		}

		Integer cat_index = redisClient.getInteger(KEY_APPLE_CATE);
		if (cat_index == null) {
			redisClient.setInteger(KEY_APPLE_CATE, 0);
			cat_index = 0;
		}

		if (rank > RANK_MAX || switchCat) {
			redisClient.setInteger(KEY_APPLE_RANK, RANK_MIN);// 重置
			rank = RANK_MIN;

			if (cat_index >= CATS.length - 1) {
				redisClient.setInteger(KEY_APPLE_CATE, 0);// 重置
				cat_index = 0;
			} else {
				redisClient.incr(KEY_APPLE_CATE);
				cat_index++;
			}
		}
		param.setRank(rank);
		param.setCat(CATS[cat_index]);

		lock.unlock();
		return param;
	}

	private AppRank queryAppInfo(String cat, int rank) {
		AppInfo info =  rankingService.selectByCatAndRank(cat, rank);
		return convert(info, cat, rank);
	}
	

	private class RankParam {
		private String cat;
		private int rank;

		public String getCat() {
			return cat;
		}

		public void setCat(String cat) {
			this.cat = cat;
		}

		public int getRank() {
			return rank;
		}

		public void setRank(int rank) {
			this.rank = rank;
		}
	}
	
	
	private AppRank convert(AppInfo appInfo, String cat, int pos){
		if (appInfo == null){
			return null;
		}
		AppRank rank = new AppRank();
		rank.setCat(cat);
		rank.setPos(pos);
		rank.setTab(1);
		rank.setAppid(appInfo.getAppid().intValue());
		rank.setName(appInfo.getName());
		rank.setPrice(appInfo.getPrice().intValue());
		rank.setSize(appInfo.getSize().intValue());
		rank.setUrl(appInfo.getUrl());
		rank.setIcons(appInfo.getIcons().split("\\|"));
		Date date = new Date();
		rank.setCreateTime(date);
		rank.setModifyTime(date);
		return rank;
	}
	
	private String generateKey(Integer appid){
		if (null != appid) {
			return "appstore:rank:used:"+appid;
		} else {
			return null;
		}
	}
}
