package com.zhangtong.core.appstore.mapper;

import org.apache.ibatis.annotations.Param;

import com.zhangtong.core.appstore.entity.AppInfo;

public interface AppInfoMapper {
	
	AppInfo selectByCatAndRank(@Param("cat")String cat, @Param("pos")int pos);
	AppInfo selectByAppid(String appid);
}	