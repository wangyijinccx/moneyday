package com.zhangtong.core.appstore.mapper;

import java.util.List;

import com.zhangtong.core.appstore.entity.Ranking;

public interface RankingMapper {
	
	List<Ranking> selectRankByAppid(String appid);
	
}