package com.ipeaksoft.moneyday.task.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.ipeaksoft.moneyday.core.entity.UserTaskFastClick;
import com.ipeaksoft.moneyday.core.mapper.UserTaskFastClickMapper;
import com.ipeaksoft.moneyday.core.service.BaseService;
import com.ipeaksoft.moneyday.core.util.AnalysisIpUtil;
import com.ipeaksoft.moneyday.task.thread.TaskClickIPThread;


/**
 * 定期更新激活表中ip对应的地域信息
 */

@Lazy(false)
@Component
public class TaskClickIPService extends BaseService{
    @Autowired
    private UserTaskFastClickMapper           mapper;
	private static final String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
    private static final Pattern pattern = Pattern.compile(regex);

    @PostConstruct
    public void init() {
    }
	
    @Scheduled(cron = "*/10 * * * * ?")
    private void schedule() {
		logger.info("schedule start...");
    	List<UserTaskFastClick> list = mapper.selectNoLocation();
    	for (UserTaskFastClick click: list){
    		try {
    			if (StringUtils.isNotBlank(click.getClientip())){
        			Matcher matcher = pattern.matcher(click.getClientip());
        			logger.debug("ip: "+click.getClientip()+", match:"+matcher.matches());
        			if (matcher.matches()){
            			Map<String, String> address = AnalysisIpUtil.getAddress(click.getClientip());
            			String country = address.get("country");
            			String area = address.get("area");
            			String province = address.get("province");
            			String city = address.get("city");
            			String county = address.get("county");
            			String isp = address.get("isp");
            			UserTaskFastClick record = new UserTaskFastClick();
            			record.setId(click.getId());
            			record.setCountry(country);
            			record.setArea(area);
            			record.setProvince(province);
            			record.setCity(city);
            			record.setCounty(county);
            			record.setIsp(isp);
            			mapper.updateLocationByPrimaryKey(record);
            			Thread.sleep(100);
        			}
        		}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	logger.info("schedule end...");
    }
}
