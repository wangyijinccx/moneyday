package com.zhangtong.core.appstore.web;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.zhangtong.core.appstore.entity.AppInfo;
import com.zhangtong.core.appstore.entity.Ranking;
import com.zhangtong.core.appstore.service.AppStoreService;
import com.zhangtong.core.appstore.service.RankingService;
import com.zhangtong.core.appstore.vo.AppRank;

@RestController
public class RankingController extends BaseController{
	@Autowired
	RankingService rankingService;
	@Autowired
	AppStoreService appStoreService;

    @RequestMapping("rank")
    public Object rank(String appid) {
    	List<Ranking> list = rankingService.selectRank(appid);
    	return list;
    }

    @RequestMapping("rankbycat")
    public Object rankByCat(String appid, String cat) {
		JSONObject result = new JSONObject();
    	List<Ranking> list = rankingService.selectRank(appid);
    	int rank = 0;
		if (list.size() > 0){
			if (list.size() == 1 || StringUtils.isEmpty(cat)){
				rank = list.get(0).getPos();
			}
			else{
				for (Ranking r: list){
					if (cat.equals(r.getCat())){
						rank = r.getPos();
						break;
					}
				}
			}
		}
		result.put("result", 1);
		result.put("appid", appid);
		result.put("rank", rank);
		return result;
	}

    @RequestMapping("appinfo")
    public Object queryApp(String appid){
    	AppInfo info = rankingService.selectByAppid(appid);
    	return info;
    }

    @RequestMapping("appinfobyrank")
    public Object appinfobyrank(String cat, Integer rank){
    	AppInfo info =  rankingService.selectByCatAndRank(cat, rank);
		return convert(info, cat, rank);
    }
    
    @RequestMapping("assign")
    public Object assign(){
    	return appStoreService.assign();
    }
    
    @RequestMapping("delassign")
    public Object delAssign(String appid){
    	appStoreService.assignDel(appid);
		JSONObject result = new JSONObject();
		result.put("result", 1);
		return result;
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

}
