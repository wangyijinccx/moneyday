package com.ipeaksoft.moneyday.api.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.api.service.CallbackService;
import com.ipeaksoft.moneyday.api.vo.AdLangyi;
import com.ipeaksoft.moneyday.core.dto.FastClick;
import com.ipeaksoft.moneyday.core.entity.IdfaApp;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastActive;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastClick;
import com.ipeaksoft.moneyday.core.service.AdIdfaService;
import com.ipeaksoft.moneyday.core.service.FastActiveService;
import com.ipeaksoft.moneyday.core.service.FastClickService;
import com.ipeaksoft.moneyday.core.service.IdfaAppService;
import com.ipeaksoft.moneyday.core.service.RedisClient;
import com.ipeaksoft.moneyday.core.service.TaskFastService;
import com.ipeaksoft.moneyday.core.service.UserFastService;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.core.util.Constant;
import com.ipeaksoft.moneyday.core.util.FastActive;
import com.ipeaksoft.moneyday.core.util.IPUtils;
import com.ipeaksoft.moneyday.core.util.RedisKeyUtil;

@Controller
public class TaskBridgeController extends BaseController {

    @Autowired
    RedisClient             redisClient;
    @Autowired
    UserService             userService;
    @Autowired
    FastActiveService       fastActiveService;
    @Autowired
    FastClickService        fastClickService;
    @Autowired
    UserFastService         userFastService;
    @Autowired
    TaskFastService         taskFastService;
    @Autowired
    AdIdfaService  adIdfaService;
    @Autowired
	IdfaAppService idfaAppService;

    @Autowired
    CallbackService callbackService;

    /**
     * 获取快速任务列表
     * @return
     */
    @ResponseBody
    @RequestMapping("getAdsByChannel")
    public Object getAdsByChannel(String channelName) {
        JSONObject result = new JSONObject();
        try {
            List<TaskFast> channelList = taskFastService.listChannelTask(channelName);
            result.put("code", 0);
            result.put("message", "success");
            result.put("size", String.valueOf(channelList.size()));
            result.put("list", toChannelListNoAppid(channelList));
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", 1);
            result.put("message", "程序异常");
        }
        return result;
    }


    @ResponseBody
    @RequestMapping("click")
    public Object click(FastClick fastClick, HttpServletResponse response) {
        JSONObject result = new JSONObject();
        try {
            Long taskId = Long.valueOf(fastClick.getTaskId());
            TaskFast fast = taskFastService.findById(taskId);
            String idfa = fastClick.getIdfa();
            boolean duplicate = callbackService.query(fast, idfa, fastClick);
            if (duplicate){
        		result.put("code", 203);
        		result.put("message", "idfa重复");
        		return result;
            }

            // 对不同渠道进行异步分发点击请求
            boolean clickResult = false;
            switch (fast.getTaskSource()){
        	case "DIANRU":
        		clickResult = callbackService.clickSync(fast, idfa, fastClick);
        		break;
        	default:
        		callbackService.clickAsync(fast, idfa, fastClick);
        		clickResult = true;
            }
            if (!clickResult){
        		result.put("code", 204);
        		result.put("message", "任务已下线");
        		return result;
            }

            fastClick.setAppID(fast.getAppid());
            // 查询数据库点击记录
            UserTaskFastClick utFastClick = fastClickService.findChannelByTaskId(fastClick.getIdfa(), taskId);
            if (null != utFastClick) {
                UserTaskFastClick utfClick = new UserTaskFastClick();
                utfClick.setId(utFastClick.getId());
                utfClick.setPoint(fastClick.getPoints());   
                utfClick.setCreateTime(new Date());
                fastClickService.updateBySelective(utfClick);
            } else {
                fastClick.setClientIP((null==fastClick.getClientIP()) ? getIP() : fastClick.getClientIP());
                fastClick.setAppName(fast.getTaskname());
                fastClick.setPoints(fast.getAward().toString());    
                // 记录快速任务点击记录
                utFastClick = fastClick.convertModel();
                fastClickService.create(utFastClick);   
            }
            // 存入redis
            String key = RedisKeyUtil.getKey(fastClick);
            redisClient.setObject(key, fastClick);
            redisClient.expire(key, RedisKeyUtil.TIMEOUT_CLICK);// 设置超时时间
            // 返回
            result.put("code", 0);
            result.put("message", "success");
            return result;
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", 1);
            result.put("message", "未知异常");
        }
        return result;
    }
    
    
    /**
     * @description 第三方激活快速任务的回调方法
     * @author: sxy
     * 2015年5月26日 下午5:37:11
     * @param fastActive
     * @return 
     */
    @ResponseBody
    @RequestMapping(value={"callback"})
    public Object callback(FastActive fastActive) {
    	if (IPUtils.isInnerIP(fastActive.getClientIP())){
            fastActive.setClientIP(getIP());
    	}
        JSONObject result = new JSONObject();
        String code = "1002";
        String message = "";
        try {
            String appid = fastActive.getAppID();
            String idfa = fastActive.getIdfa();
            
            // 异步分发消息到渠道商激活接口或者上报到广告主
            callbackService.callActive(fastActive);
            UserTaskFastClick click = fastClickService.getOneByIdfa(idfa, appid);
            if (null != click) {
                // 查询任务激活记录
                UserTaskFastActive taskFastActive = fastActiveService.findByIdfa(idfa, click.getTaskId(), Constant.CLIENTTYPE_OTHER);
                if (null == taskFastActive) {
                    // 记录快速任务激活记录
                    UserTaskFastActive active = toActiveObject(fastActive, click);
                    active = fastActiveService.create(active);

                    // 记录快速任务明细表
                    if ("1".equals(active.getStatus())) {
                        code = "1000";
                        message = "success";
                    }
                } else {
                    code = "1000";
                    message = "已激活";
                }
                syncIdfaApp(idfa, appid);
            } else {
                code = "1005";
                message = "任务未开始，不能提交审核";
            }
        } catch (Exception e) {
            logger.error("ERROR:", e);
            code = "1001";
            message = "未知异常";
        }

        result.put("code", code);
        result.put("message", message);
        return result;
    }

    /**
     * 以渠道的激活为准时渠道上报接口
     * @param fastActive
     * @return
     */
    @ResponseBody
    @RequestMapping("upload")
    public Object upload(String idfa, String taskId,String clientIP,String osVersion) {
        TaskFast fast = null;
        try{
            fast = taskFastService.findById(Long.parseLong(taskId));
        }
        catch(Exception e){
        }
        
        if (fast == null){
    		JSONObject result = new JSONObject();
    		result.put("code", "1002");
            result.put("message", "参数错误");
    		return result;
        }
    	FastActive fastActive = new FastActive();
    	fastActive.setIdfa(idfa);
    	fastActive.setAppID(fast.getAppid());
    	fastActive.setTaskSource(fast.getTaskSource());
    	fastActive.setClientIP(clientIP);
    	fastActive.setOSVersion(osVersion);
    	
    	return callback(fastActive);
    }
    
	@Async
	private void syncIdfaApp(String idfa, String appid){
		try {
			int cnt = idfaAppService.selectCntByAppidAndIdfa(appid, idfa);
			IdfaApp record = new IdfaApp();
			record.setIdfa(idfa);
			record.setAppid(appid);
			record.setCreateTime(new Date());
			record.setNumorder(cnt);
			idfaAppService.insert(record);
		} catch (Exception e) {
		}
	}
	
	
	private List<AdLangyi> toChannelListNoAppid(List<TaskFast> channelList) {
        List<AdLangyi> result = new ArrayList<AdLangyi>();
        for (TaskFast fast : channelList) {
            AdLangyi ad = new AdLangyi();
            ad.setAdid(fast.getId().toString());
            ad.setTask_source(fast.getTaskSource());
            ad.setAd(fast.getTaskname());
            ad.setApp_store_url(fast.getDownloadUrl());
            ad.setPrice(fast.getAward().toString());
            result.add(ad);
        }
        return result;
    }


    private UserTaskFastActive toActiveObject(FastActive active, UserTaskFastClick click) {
        UserTaskFastActive record = new UserTaskFastActive();
        String taskSource = active.getTaskSource();
        record.setIdfa(active.getIdfa());
        record.setTaskId(click.getTaskId());
        record.setIp(active.getClientIP());
        record.setCreateTime(new Date());
        record.setStatus("0");
        if (click != null) {
            record.setClientType(click.getClientType());
            record.setMobile(click.getMobile());
            record.setAppid(click.getAppid());
            record.setStatus("1");
        }
        if (taskSource != null && !"self".equals(taskSource)) {
            record.setClientType(Constant.CLIENTTYPE_OTHER);
        }
        return record;
    }
}
