package com.zhangtong.core.appstore.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhangtong.core.appstore.entity.AppInfo;
import com.zhangtong.core.appstore.entity.Ranking;
import com.zhangtong.core.appstore.mapper.AppInfoMapper;
import com.zhangtong.core.appstore.mapper.RankingMapper;

@Service
public class RankingService extends BaseService {
	@Autowired
	private RankingMapper rankingMapper;
	@Autowired
	private AppInfoMapper appInfoMapper;
	
	/**
	 * 根据分类和排名获取appinfo
	 * @param cat
	 * @param pos
	 * @return
	 */
	public AppInfo selectByCatAndRank(String cat, int pos){
		return appInfoMapper.selectByCatAndRank(cat, pos);
	}
	
	public AppInfo selectByAppid(String appid){
		return appInfoMapper.selectByAppid(appid);
	}

	/**
	 * 根据appid获取排名信息(滤除总榜之外数据)
	 * @param appid
	 * @return
	 */
	public List<Ranking> selectRank(String appid){
		return rankingMapper.selectRankByAppid(appid);
	}
	
}