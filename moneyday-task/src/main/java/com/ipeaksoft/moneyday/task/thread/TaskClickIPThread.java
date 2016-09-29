package com.ipeaksoft.moneyday.task.thread;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ipeaksoft.moneyday.core.entity.UserTaskFastClick;
import com.ipeaksoft.moneyday.core.mapper.UserTaskFastClickMapper;
import com.ipeaksoft.moneyday.core.util.AnalysisIpUtil;

public class TaskClickIPThread implements Runnable {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
    private static final Pattern pattern = Pattern.compile(regex);

	UserTaskFastClickMapper mapper;

	public TaskClickIPThread(UserTaskFastClickMapper mapper){
		this.mapper = mapper;
	}

	@Override
	public void run() {
		logger.info("TaskClickIPThread start...");
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
		logger.info("TaskClickIPThread end...");
    }

}
