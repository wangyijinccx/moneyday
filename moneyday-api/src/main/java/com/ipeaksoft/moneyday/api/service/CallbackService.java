package com.ipeaksoft.moneyday.api.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.dto.FastClick;
import com.ipeaksoft.moneyday.core.entity.AdsSource;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.service.AdsSourceService;
import com.ipeaksoft.moneyday.core.service.BaseService;
import com.ipeaksoft.moneyday.core.service.HttpService;
import com.ipeaksoft.moneyday.core.service.IdfaAppService;
import com.ipeaksoft.moneyday.core.service.RedisClient;
import com.ipeaksoft.moneyday.core.service.TaskFastService;
import com.ipeaksoft.moneyday.core.util.FastActive;
import com.ipeaksoft.moneyday.core.util.MD5Util;
import com.ipeaksoft.moneyday.core.util.RedisKeyUtil;

@Service
public class CallbackService extends BaseService {

	@Autowired
	TaskFastService taskFastService;
	@Autowired
	RedisClient redisClient;
	@Autowired
	HttpService httpService;
	@Autowired
	AdsSourceService adsSourceService;
	@Autowired
	IdfaAppService idfaAppService;

	private static final String CALLBACL_URL = "http://pa.123nat.com/api/active?taskSource=%s&idfa=%s&appID=%s";
	
	private Date lastUpdateTime;
	private Map<String, AdsSource> map= new HashMap<String, AdsSource>();
	
	@PostConstruct
	public void init(){
		lastUpdateTime = new Date();
		List<AdsSource> list = adsSourceService.selectAll();
		list.forEach(t->{
			map.put(t.getKey(), t);
		});
	}
	
	public void schedule(){
		List<AdsSource> list = adsSourceService.selectByModifyTime(lastUpdateTime);
		list.forEach(t->{
			if (t.getEnable()){
				map.put(t.getKey(), t);
			}
			else{
				map.remove(t.getKey());
			}
		});
		lastUpdateTime = new Date();
	}

	/**
	 * 激活信息上报广告主或者下送渠道
	 * @param fastActive
	 * @throws UnsupportedEncodingException
	 */
	@Async
	public void callActive(FastActive fastActive) throws UnsupportedEncodingException {
		String key = RedisKeyUtil.getKey(fastActive);
		Object obj = redisClient.getObject(key);
		if (obj != null) {
			FastClick click = (FastClick) obj;
			redisClient.delByKey(key);

			Long taskId = Long.valueOf(click.getTaskId());
			TaskFast fast = taskFastService.findById(taskId);
			
			//上报广告主
			if (fast.isActiveUpload()){
				active(fast, fastActive);
			}
			else{
				//下送子渠道
				Date now = new Date();
				boolean isCallBack = now.after(fast.getStartTime()) && now.before(fast.getEndTime());
				logger.debug("Task ID:{}, StartTime:{}, EndTime:{}, Now:{}, isCallBack:{}", taskId, fast.getStartTime(), fast.getEndTime(), now, isCallBack);
				if (isCallBack) {
					String callback = click.getCallback();
					if (StringUtils.isNotBlank(callback)){
						logger.info("url to channel:{}", callback);
						httpService.get(callback);
					}
				}
			}
		}
	}

	/**
     * 排重接口
     * @param task
     * @param idfa
     * @return true代表重复, false代表非重复
     */
    public boolean query(TaskFast task, String idfa, FastClick fastClick){
    	if (!task.isDuplicate()){
    		return false;
    	}
    	if (!map.containsKey(task.getTaskSource())){
    		return false;
    	}
    	
    	AdsSource ad = map.get(task.getTaskSource());
    	String duplicate_url = ad.getDuplicateUrl();
    	if (StringUtils.isBlank(duplicate_url)){
    		return false;
    	}
    	
    	String callbackUrl = String.format(CALLBACL_URL, task.getTaskSource(), idfa, task.getAppid());
    	try {
			callbackUrl = URLEncoder.encode(callbackUrl, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
    	String url = null;
    	switch (task.getTaskSource()){
    	case "LANMAOG":
    		url = String.format(duplicate_url, task.getAdId(), idfa);
    		break;
    	case "KEJINXINXI":
    		url = String.format(duplicate_url, task.getAdId2(), idfa, fastClick.getClientIP());
    		break;
    	case "ZHIMENG":
			url = String.format(duplicate_url, idfa);
			break;
    	case "MIDIG":
			url = String.format(duplicate_url, task.getAppid(), idfa);
			break;
    	case "YOUMI":
			url = duplicate_url;
			break;
    	case "CHANDASHI":
			url = String.format(duplicate_url, task.getAppid(), idfa);
			break;
    	case "MOPAN":
			url = duplicate_url;
			break;
    	case "DIANZHIKJ":
            url = String.format(duplicate_url, task.getAdId2(), idfa);
    		break;
    	case "QIZHUAN":
            url = String.format(duplicate_url, task.getAppid(), idfa, fastClick.getClientIP());
    		break;
    	case "ZAKER":
            url = String.format(duplicate_url, idfa);
    		break;
    	case "HOTTOMATO":
            url = String.format(duplicate_url, task.getAppid(), idfa);
    		break;
    	case "DIANRU":
            url = duplicate_url;
    		break;
    	case "FANZHUO":
    		url = String.format(duplicate_url,"22", task.getAppid(), idfa);
    		break;
    	case "DOMOB":
    		url = duplicate_url;
    		break;
    	case "WABANG":
    		url = duplicate_url;
    		break;
    	case "DIANLE":
    		url = String.format(duplicate_url,task.getAdId(), task.getAdId2(), idfa);
    		break;
    	case "KAIWAN":
    		url = String.format(duplicate_url,  idfa, task.getAppid());
    		break;
    	case "KUAIYOU":
    		url = duplicate_url;
    		break;
    	case "FEINIU":
    		url = String.format(duplicate_url, task.getAdId(), idfa);
    		break;
    	case "ZHONGYI":
    		url = String.format(duplicate_url,task.getAdId(), idfa, fastClick.getClientIP());
    		break;
    	case "YOUZI":
    		url = String.format(duplicate_url,task.getAdId(), idfa);
    		break;
    	case "TTQZ":
    		url = String.format(duplicate_url,task.getAdId(), idfa);
    		break;
    	case "WENDAO":
    		url = duplicate_url;
    		break;
    	case "AMI":
    		url = String.format(duplicate_url,task.getAdId(), idfa);
    		break;
    	case "YIJIFEN":
    		url = String.format(duplicate_url,task.getAdId2(), idfa);
    		break;
    	case "SHANGBANG":
			url = String.format(duplicate_url,task.getAppid(), idfa, task.getAdId(), fastClick.getClientIP());
			break;
    	case "XIAOYU":
			url = String.format(duplicate_url,task.getAppid(), idfa);
			break;
    	case "AMIBAK":
    		url = String.format(duplicate_url,task.getAdId(), idfa, fastClick.getClientIP(), callbackUrl);
    		break;
    	case "QIANZHAN":
			url = String.format(duplicate_url, idfa, callbackUrl);
			break;
    	case "BAIHE":
    		String osV = (null == fastClick.getOSVersion()?"9.0.2":fastClick.getOSVersion());
    		url = String.format(duplicate_url, idfa, task.getAdId(),"02:00:00:00:00:00",osV,fastClick.getClientIP(),callbackUrl);
    		break;
    	}
    	if (StringUtils.isNotBlank(url)){
        	if ("YOUMI".equals(task.getTaskSource())){
        		HashMap<String, String> form = new HashMap<>();
        		form.put("appid", task.getAppid());
        		form.put("idfa", idfa);
        		String result = httpService.post(url, form);
    			@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(result, Map.class);
    			if (res != null && res.containsKey("c")) {
    				return false;
    			}
    			if (res != null && res.containsKey(idfa)) {
    				Integer ret = (Integer) res.get(idfa);
    				if (ret != null && ret == 0)
    					return false;
    				return true;
    			}
        	}
        	else if ("MOPAN".equals(task.getTaskSource())){
        		HashMap<String, String> form = new HashMap<>();
        		form.put("appid", task.getAppid());
        		form.put("idfa", idfa);
        		form.put("source", "pa");
    			String resStr = httpService.post(url, form);
    			@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
    			if (res != null && res.containsKey(idfa) && (Integer) res.get(idfa) == 1) {
    				return true;
    			}
        	}
        	else if ("QIZHUAN".equals(task.getTaskSource())){
        		String result = httpService.get(url);
        		@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(result, Map.class);
    			if (res != null && "1".equals((String) res.get("Result"))) {
    				return true;
    			}
        	}else if ("DIANRU".equals(task.getTaskSource())){
        		HashMap<String, String> form = new HashMap<>();
        		form.put("dr_adid", task.getAdId());
        		form.put("idfa_list", idfa);
    			String resStr = httpService.post(url, form);
    			@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
    			if (res != null && res.containsKey(idfa)) {
    				Object ret = res.get(idfa);
    				if (ret instanceof String){
    					return "1".equals((String)ret);
    				}
    				else if (ret instanceof Integer){
    					return ((Integer)ret).intValue() == 1;
    				}
    				return false;
    			}
        	}else if ("FANZHUO".equals(task.getTaskSource())){
        		String result = httpService.get(url);
        		JSONObject _result_json = JSONObject.parseObject(result);
        		String resStr = _result_json.getJSONObject("data").getString("idfas");
    			@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
    			if (res != null && res.containsKey(idfa) && 1 == (Integer)res.get(idfa)) {
    				return true;
    			}
        	}else if ("DOMOB".equals(task.getTaskSource())){
        		HashMap<String, String> form = new HashMap<>();
        		form.put("appid", task.getAppid());
        		form.put("idfa", idfa);
        		String resStr = httpService.post(url, form);
        		JSONObject json = JSONObject.parseObject(resStr);
            	return "1".equals(json.getString(idfa));
        		/*
    			@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
    			if (res != null && res.containsKey(idfa) && "1".equals((String)res.get(idfa))) {
    				return true;
    			}*/
        	}else if ("WABANG".equals(task.getTaskSource())){
        		HashMap<String, String> form = new HashMap<>();
        		form.put("appid",task.getAdId2());
        		form.put("idfa", idfa);
        		String resStr = httpService.post(url, form);
    			@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
    			if (res != null && res.containsKey(idfa) && "1".equals((String)res.get(idfa))) {
    				return true;
    			}
        	}else if ("DIANLE".equals(task.getTaskSource())
        			||"AMI".equals(task.getTaskSource())){
        		String result = httpService.get(url);
        		@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(result, Map.class);
    			if (res != null && 0 != (Integer) res.get("msg")) {
    				return true;
    			}
        	}else if ("KUAIYOU".equals(task.getTaskSource())){
        		HashMap<String, String> form = new HashMap<>();
        		form.put("appid", task.getAppid());
        		form.put("idfa", idfa);
    			String resStr = httpService.post(url, form);
    			@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
    			if (res != null && res.containsKey(idfa) && "1".equals((String)res.get(idfa))) {
    				return true;
    			}
        	}
        	else if ("FEINIU".equals(task.getTaskSource())){
        		String result = httpService.get(url);
        		@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(result, Map.class);
    			if (res != null && 1 == (Integer) res.get("status")) {
    				return true;
    			}
        	}
        	else if ("CHANDASHI".equals(task.getTaskSource())){
        		String result = httpService.get(url);
        		if(StringUtils.isNotBlank(result)){
        			JSONObject json = JSONObject.parseObject(result);
            		return !"0".equals(json.getString(idfa));
        		}else{
        			return true;
        		}
        	}
        	else if ("WENDAO".equals(task.getTaskSource())){
        		int cnt = idfaAppService.selectDuplicateCntByAppidAndIdfa(task.getAppid(), idfa);
        		if(cnt > 0){
        			return true;
        		}
        	}
        	else if ("AMIBAK".equals(task.getTaskSource())){
        		String result = httpService.get(url);//"{"status":1}"有效
        		if(!"{\"status\":\"1\"}".equals(result)){
        			return true;
        		}
        	}
        	else if ("QIANZHAN".equals(task.getTaskSource())){
        		String result = httpService.get(url);
        		@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(result, Map.class);
    			if (res != null && 0 == (Integer) res.get("code")) {
    				return true;
    			}
        	}
        	else if ("BAIHE".equals(task.getTaskSource())){
        		String result = httpService.get(url);
        		@SuppressWarnings("unchecked")
    			Map<String, Object> res = JSONObject.parseObject(result, Map.class);
    			if (res != null && 1 != (Integer) res.get("code")) {
    				return true;
    			}
        	}
        	else {
            	String result = httpService.get(url);
            	JSONObject json = JSONObject.parseObject(result);
            	return "1".equals(json.getString(idfa));
        	}
    	}
    	return false;
    }
    

    /**
     * 异步点击接口
     * @param task
     * @param idfa
     */
    @Async
    public void clickAsync(TaskFast task, String idfa, FastClick fastClick){
    	clickSync(task, idfa, fastClick);
    }
    
    /**
     * 同步点击接口
     * @param task
     * @param idfa
     * @param fastClick
     */
    public boolean clickSync(TaskFast task, String idfa, FastClick fastClick){
        if (!map.containsKey(task.getTaskSource())){
    		return false;
    	}
    	String ip= fastClick.getClientIP();
    	AdsSource ad = map.get(task.getTaskSource());
    	String click_url = ad.getClickUrl();
    	if (StringUtils.isBlank(click_url)){
    		return false;
    	}
    	
    	String url = null;
    	String adid = task.getAdId();
    	String appid = task.getAppid();
    	String mac = "";
		String callbackUrl = String.format(CALLBACL_URL, task.getTaskSource(), idfa, task.getAppid());
		try {
			callbackUrl = URLEncoder.encode(callbackUrl, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		switch (task.getTaskSource()){
    	case "LANMAOG":
    		url = String.format(click_url, adid, idfa,ip,fastClick.getOSVersion());
    		break;
    	case "KEJINXINXI":
    		long time = System.currentTimeMillis();
    		url = String.format(click_url, adid, idfa, ip, time, callbackUrl);
    		break;
    	case "ZHIMENG":
			url = String.format(click_url, idfa, callbackUrl);
			break;
    	case "MIDIG":
			url = String.format(click_url, appid, mac, idfa, ip, callbackUrl);
			break;
    	case "YOUMI":
			url = String.format(click_url, adid, mac, idfa, ip);
			break;
    	case "CHANDASHI":
			url = click_url;
			break;
    	case "MOPAN":
			url = String.format(click_url, "pa", adid, mac, idfa, callbackUrl, ip);
			break;
    	case "HUDONG":
			url = String.format(click_url, adid, idfa, mac, ip, callbackUrl);
			break;
    	case "DIANZHIKJ":
    		 url = String.format(click_url,task.getAdId2(),idfa,callbackUrl);
    		 break;
    	case "QIZHUAN":
            url = String.format(click_url, appid, idfa, ip, callbackUrl);
            break;
    	case "ZAKER":
            url = String.format(click_url, idfa,callbackUrl,ip);
    		break;
    	case "HOTTOMATO":
    		url = String.format(click_url, appid, idfa,"",fastClick.getOSVersion(),ip);
    		break;
    	case "DIANRU":
			String key = "zhangtongxgm";
			String osVer = (null == fastClick.getOSVersion()?"":fastClick.getOSVersion());
			long currentTime = System.currentTimeMillis();
			String checksum = MD5Util.md5("adid=" + adid
					+ "&device=iPhone&osver=" + osVer
					+ "&idfa=" + idfa + "&client_ip=" + ip + "&time="
					+ currentTime + "&source=xiguamei" + key);
			url = String.format(click_url, adid, osVer,idfa, ip, currentTime, checksum);
			break;
    	case "FANZHUO":
    		url = String.format(click_url,"22",adid, idfa,ip,"",callbackUrl);
    		break;
    	case "DOMOB":
    		url = String.format(click_url,task.getAdId(),"1029535", idfa,ip,task.getAppid());
    		break;
    	case "LINGJING":
    		url = String.format(click_url,task.getAdId(),ip,mac,idfa,callbackUrl);
    		break;
    	case "WABANG":
    		url = String.format(click_url,appid,idfa,"","",callbackUrl);
    		break;
    	case "DIANLE":
    		url = String.format(click_url,task.getAdId(),ip,"".equals(mac)?"020000000000":mac,idfa,task.getAdId2(),callbackUrl);
    		break;
    	case "KUAIYOU":
    		String os = (null == fastClick.getOSVersion()?"":fastClick.getOSVersion());
    		url = String.format(click_url,task.getAdId(),idfa,ip,os,callbackUrl);
    		break;
    	case "FEINIU":
    		url = String.format(click_url, task.getAdId(), idfa);
    		break;
    	case "ZHONGYI":
    		url = String.format(click_url, task.getAdId(), "".equals(mac)?"020000000000":mac,ip,idfa,callbackUrl );
    		break;
    	case "TTQZ":
    		url = String.format(click_url,task.getAdId(), idfa,ip,callbackUrl);
    		break;
    	case "KAIWAN":
    		url = String.format(click_url, task.getAppid(), idfa, ip);
    		break;
    	case "AMI":
    		url = String.format(click_url, task.getAdId(), idfa,ip);
    		break;
    	case "YIJIFEN":
    		url = String.format(click_url, task.getAdId2(), idfa,ip, callbackUrl);
    		break;
    	case "SHANGBANG":
			url = String.format(click_url,task.getAppid(), idfa,callbackUrl, fastClick.getClientIP());
			break;
    	case "XIAOYU":
			url = String.format(click_url,task.getAppid(), idfa, callbackUrl);
			break;
    	case "YOUZI":
    		url = String.format(click_url, task.getAdId(), idfa, ip, callbackUrl);
    		break;
    	case "YITUI":
    		url = String.format(click_url, idfa, appid);
    		break;
    	}
    	if (StringUtils.isNotBlank(url)){
    		boolean ret = false;
    		String result = httpService.get(url);
    		switch (task.getTaskSource()){
    		case "DIANRU":
    			JSONObject json = JSONObject.parseObject(result);
    			if (json.containsKey("success")&&"true".equals(json.getString("success"))){
    				ret = true;
    			}
    			break;
        	default:
    			ret = true;
    		}
    		return ret;
    	}
    	else{
    		return false;
    	}
    }
    
    /**
     * 激活上报接口
     * @param task
     * @param idfa
     */
    public void active(TaskFast task, FastActive fastActive){
    	if (!map.containsKey(task.getTaskSource())){
    		return;
    	}
    	
    	AdsSource ad = map.get(task.getTaskSource());
    	String active_url = ad.getActiveUrl();
    	if (StringUtils.isBlank(active_url)){
    		return;
    	}
    	
    	String idfa = fastActive.getIdfa();
    	String url = null;
    	switch (task.getTaskSource()){
    	case "LANMAOG":
    		url = String.format(active_url, task.getAdId(), idfa,"02:00:00:00:00:00",fastActive.getClientIP());
    		break;
    	case "HOTTOMATO":
    		url = String.format(active_url, task.getAppid(), idfa);
    		break;
    	case "KEJINXINXI":
    		url = String.format(active_url, task.getAdId2(), idfa,fastActive.getClientIP());
    		break;
    	case "DIANRU":
			String key = "zhangtongxgm";
			String adid = task.getAdId();
			String osVer = (null == fastActive.getOSVersion()?"":fastActive.getOSVersion());
			String clentIP = (null == fastActive.getClientIP()?"":fastActive.getClientIP());
			long currentTime = System.currentTimeMillis();
			String checksum = MD5Util.md5("adid=" + adid
					+ "&device=iPhone&osver=" + osVer
					+ "&idfa=" + idfa + "&client_ip=" + clentIP + "&time="
					+ currentTime + "&source=xiguamei" + key);
			url = String.format(active_url, adid, osVer,idfa, clentIP, currentTime, checksum);
			break;
    	case "KAIWAN":
    		url = String.format(active_url, task.getAppid(), idfa, fastActive.getClientIP());
    		break;
    	case "FEINIU":
    		url = String.format(active_url, task.getAdId(), idfa);
    		break;
    	case "YOUZI":
    		url = String.format(active_url, task.getAdId(), idfa);
    		break;
    	case "DIANLE":
    		long time = System.currentTimeMillis()/1000;
    		String token = MD5Util.md5(task.getAdId()+idfa+time+"ZT");
    		url = String.format(active_url,task.getAdId2(), task.getAdId(), idfa,time,token);
    		break;
    	case "CHANDASHI":
			url = String.format(active_url, task.getAppid(), idfa);
			break;
	    case "MIDIG":
			url = String.format(active_url, task.getAppid(), idfa,fastActive.getClientIP());
			break;
	    case "XIAOYU":
			url = String.format(active_url,task.getAppid(), idfa);
			break;
	    case "SHANGBANG":
			url = String.format(active_url,task.getAppid(), idfa, fastActive.getClientIP());
			break;
		}
    	if (StringUtils.isNotBlank(url)){
    		httpService.get(url);
    	}
    }
}