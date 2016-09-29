package com.ipeaksoft.moneyday.core.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.dto.FastClick;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.enums.TaskSourceType;
import com.ipeaksoft.moneyday.core.mapper.TaskFastMapper;
import com.ipeaksoft.moneyday.core.util.ChannelUrlPropertiesUtil;
import com.ipeaksoft.moneyday.core.util.Constant;
import com.ipeaksoft.moneyday.core.util.FastActive;
import com.ipeaksoft.moneyday.core.util.HttpRequest;
import com.ipeaksoft.moneyday.core.util.MD5Util;
import com.ipeaksoft.moneyday.core.util.RedisKeyUtil;

@Async
@Service
public class TaskFastCallbackService extends BaseService {

	@Autowired
	TaskFastMapper taskFastMapper;
	@Autowired
	RedisClient redisClient;
	@Autowired
	AdsSourceService adsSourceService;

	public void callback(FastClick fastClick, TaskFast fast) {
		try {
			callback(fastClick, fast, 0);
		} catch (Exception e) {
			logger.error("ERROR", e);
		}
	}

	public void callback(FastClick fastClick, String taskSource) {
		try {
			callback(fastClick, null, 1);
		} catch (Exception e) {
			logger.error("ERROR", e);
		}
	}

	/**
	 * 回调分两类， 一类是广告主有固定的回调url，另外一类是嵌入SDK的游戏，每个游戏一个回调url
	 * @param fastClick
	 * @param fast
	 * @param type
	 * @throws Exception
	 */
	public void callback(FastClick fastClick, TaskFast fast, int type) throws Exception{
		String taskSource = fast.getTaskSource();
		if (TaskSourceType.REYUN.name().equals(taskSource)){
			//http://uri6.com/[后台生成短链]?mac=[MAC]&idfa=[IDFA]&bundleid=应用的appid&noredirect=true&ip= 客户端ip&callback=[callbackurl]
			String sdkLink = fast.getSdkLink();
			String url = sdkLink+"?mac=%s&idfa=%s&bundleid=%s&noredirect=true&ip=%s&callback=%s";
			String mac = "";
			String idfa = fastClick.getIdfa();
			String appid = fast.getAppid();
			String ip = fastClick.getClientIP();
			String callbackUrl = "http://ads.i43.com/api/speedTaskActive?taskSource=%s&idfa=%s&appID=%s";
			callbackUrl = String.format(callbackUrl, taskSource, idfa, appid);
			callbackUrl = URLEncoder.encode(callbackUrl, "UTF-8");
			url = String.format(url, mac, idfa, appid, ip, callbackUrl);
			String result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			logger.info("广告主: {}, 点击URL: {}, 点击结果: {}", taskSource, url, result);
		}
		else if (TaskSourceType.TalkingData.name().equals(taskSource)){
			
		}
		else{
			String adId = null;
			if (type == 0) { // 微信端需要转换
				adId = fast.getAdId();
			} else { // 工作室端已转换，直接取
				adId = fastClick.getAppID();
			}
			callback(fastClick, taskSource, adId);
		}
	}

	private void callback(FastClick fastClick, String taskSource, String adId) {
		String url = ChannelUrlPropertiesUtil.getProperty(taskSource);
		if (StringUtils.isNotBlank(url)) {
//			Long taskId = Long.valueOf(fastClick.getTaskId());
//			TaskFast fast = taskFastMapper.selectByPrimaryKey(taskId);
			String idfa = fastClick.getIdfa();
			taskSource = taskSource.toUpperCase();
			String result = null;
			String appid = fastClick.getAppID();
//			String adId = null;
//			if (type == 0) { // 微信端需要转换
//				adId = fast.getAdId();
//			} else { // 工作室端已转换，直接取
//				adId = appid;
//			}
			String mac = fastClick.getMacAddress();
			mac = (StringUtils.isEmpty(mac)) ? "020000000000" : mac;
			final String macAddress = "02:00:00:00:00:00";
			String ip = fastClick.getClientIP();
			ip = (null == ip) ? "" : ip;
			String callbackUrl = "http://ads.i43.com/api/speedTaskActive?taskSource=%s&idfa=%s&appID=%s";
			callbackUrl = String.format(callbackUrl, taskSource, idfa, appid);

			/*****************************公司接口特殊，两个参数共同决定一个产品**********************************/

			//北京无限点乐科技有限公司
			if (TaskSourceType.DIANLE.toString().equals(taskSource)) {
				String adId_appid[] = adId.split(",");
				String adId_one = adId_appid[0];
				String adId_two = adId_appid[1];
				url = String.format(url, adId_one, idfa, idfa, mac, adId_two, ip);
			}

			/*************************** 不需要URLEncode类型的callbackUrl ***********************************/
			
			//天天挂奇迹
			if (TaskSourceType.TTGQJ.toString().equals(taskSource)) {
				url = String.format(url, idfa);
			}
			
			//有米2
			if (TaskSourceType.YOUMIT.toString().equals(taskSource)) {
				url = String.format(url, adId, mac, idfa, ip);
			}
			
			//汇智纵横
			if (TaskSourceType.HUIZHI.toString().equals(taskSource)) {
				String osVersion = fastClick.getOSVersion();
				if(null == osVersion || "".equals(osVersion) ){
					osVersion="9.1";
				}
				url = String.format(url, idfa, osVersion, adId, appid,ip);
			}
			
			//北京星联时空科技有限公司
			if (TaskSourceType.XINGLIAN.toString().equals(taskSource)) {
				url = String.format(url, adId, mac, idfa);
			}
			
			//成都吉乾科技有限公司
			if (TaskSourceType.Gamesky.toString().equals(taskSource)) {
				url = String.format(url, idfa, mac);
			}
			// 深圳天游网络科技有限公司
			if (TaskSourceType.TIANYOU.toString().equals(taskSource)) {
				url = String.format(url, idfa, mac);
			}
			// 北京百度网讯科技有限公司
			if (TaskSourceType.NUOMI.toString().equals(taskSource)) {
				url = String.format(url, appid, idfa);
			}
			// 团博百众渠道的分发
			if (TaskSourceType.TUAN800.toString().equals(taskSource)) { // 判断是否是团博百众的应用
				appid = "tao800";
				String _sign = appid.concat(macAddress).concat(Constant.KEY_TAO800);
				String sign = MD5Util.md5(_sign);
				url = String.format(url, Constant.SOURCE_TAO800, appid, macAddress, idfa, sign);
			}
			// 上海摩邑诚渠道的分发
			if (TaskSourceType.MEX.toString().equals(taskSource)) { // 判断是否是上海摩邑诚的应用
				url = String.format(url, adId, idfa, appid);
			}
			// 优米渠道的分发
			if (TaskSourceType.YOUMI.toString().equals(taskSource)) { // 判断是否是优米的应用
				url = String.format(url, adId, mac, idfa, ip);
			}
			// 深圳培科渠道的分发
			if (TaskSourceType.PEIKE.toString().equals(taskSource)) { // 判断是否是深圳培科的应用
				url = String.format(url, idfa, mac, taskSource.toLowerCase());
			}
			// 软猎渠道的分发
			if (TaskSourceType.RUANLIE.toString().equals(taskSource)) { // 判断是否是软猎的应用
				url = String.format(url, mac, appid, idfa);
			}
			// 行者天下渠道的分发
			if (TaskSourceType.XINGZHETIANXIA.toString().equals(taskSource)) { // 判断是否是行者天下的应用
				url = String.format(url, adId, idfa, ip, Constant.SOURCE_ZHANGTONG);
			}
			
			// 钱妈妈理财
			if (TaskSourceType.QIANMAMA.toString().equals(taskSource)) { // 判断是否是行者天下的应用
				url = String.format(url, idfa, mac);
			}
			
			// 未来研究所
			if (TaskSourceType.WLYJS.toString().equals(taskSource)) { // 判断是否是未来研究所的应用
				url = String.format(url, idfa);
			}
			

			/*************************** 需要URLEncode类型的callbackUrl ***********************************/

			try {
				callbackUrl = URLEncoder.encode(callbackUrl, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			//哇棒移动传媒
			if (TaskSourceType.WABANG.toString().equals(taskSource)) {
				url = String.format(url,adId,idfa,mac,"",callbackUrl);
			}
			
			
			//ZAKER
			if (TaskSourceType.ZAKER.toString().equals(taskSource)) {
				url = String.format(url,idfa,callbackUrl,ip);
			}
			
			
			//点智科技
			if (TaskSourceType.DIANZHIKJ.toString().equals(taskSource)) {
				if("1093345848".equals(appid)){
					 url = String.format(url,"1",idfa,callbackUrl);
            	}
            	if("1075817245".equals(appid)){
            		 url = String.format(url,"2",idfa,callbackUrl);
            	}
            	if("1079321984".equals(appid)){
            		 url = String.format(url,"3",idfa,callbackUrl);
            	}
                if("1093353598".equals(appid)){
                	 url = String.format(url,"4",idfa,callbackUrl);
            	}
			}
			
			
			//聚点
			if (TaskSourceType.JUDIAN.toString().equals(taskSource)) {
				String cuid ="39";
				String appkey="666EFA84A3F94829A9A8714F1A76";
				String udid="";
				String sMd5=appkey+"adid"+adId+"callback"+callbackUrl+"cuid"+cuid+"idfa"+idfa+"ip"+ip+"mac"+macAddress+"udid"+udid;
				String sign=MD5Util.md5(sMd5);
				url = String.format(url,cuid,adId,idfa,udid,macAddress,ip,callbackUrl,sign);
			}
			
			
			//闯齐
			if (TaskSourceType.CHUANGQIG.toString().equals(taskSource)) {
				url = String.format(url,appid,idfa,ip,callbackUrl);
			}
			
			//七彩
			if (TaskSourceType.QICAI.toString().equals(taskSource)) {
				url = String.format(url,idfa,callbackUrl,ip);
			}
			
			//7赚—友商 mac地址要求为正式格式  ：,必填，为空时，默认就传，02:00:00:00:00:00
			if (TaskSourceType.QIZHUAN.toString().equals(taskSource)) {
				url = String.format(url, appid, macAddress, idfa,ip,callbackUrl);
			}
			
			//米迪
			if (TaskSourceType.MIDIG.toString().equals(taskSource)) {
				url = String.format(url, appid,mac, idfa,ip,callbackUrl);
			}
			
			//加沃
			if (TaskSourceType.JIAWO.toString().equals(taskSource)) {
				url = String.format(url, adId, ip,mac, idfa,callbackUrl);
			}
			
			//江苏旭升网络科技有限公司
			if (TaskSourceType.XUSHENG.toString().equals(taskSource)) {
//				url = String.format(url, adId, idfa, ip, /*mac*/macAddress);
				/*String osVersion = fastClick.getOSVersion();
				if( osVersion==null || osVersion.equals("null") ){
					osVersion = "";
				}
				url = String.format(url, adId, idfa, macAddress, ip, osVersion,xushengMd5(idfa, "adwan", "wise_baidu_video_partner"), appid );
				*/
				String osVersion = fastClick.getOSVersion();
				if(null == osVersion || "".equals(osVersion) ){
					osVersion="9.1";
				}
				url = String.format(url, idfa, mac,adId,ip,callbackUrl,osVersion);
			}
			
			//百合
			if (TaskSourceType.BAIHE.toString().equals(taskSource)) {
				String osVersion = fastClick.getOSVersion();
				if(null == osVersion || "".equals(osVersion) ){
					osVersion="9.1";
				}
				url = String.format(url,mac,adId,idfa,ip,osVersion,callbackUrl);
			}
			
			//钱庄理财
			if (TaskSourceType.QIANZHUANGLICAI.toString().equals(taskSource)) {
				url = String.format(url,idfa, callbackUrl);
			}
			//三维度
			if (TaskSourceType.SANWEIDU.toString().equals(taskSource)) {
				url = String.format(url, appid, adId, mac, idfa, idfa, callbackUrl);
			}
			//勤诚
			if (TaskSourceType.QINCHENGHUDONG.toString().equals(taskSource)) {
				url = String.format(url, idfa, adId, appid, ip, callbackUrl);
			}
			//朗逸广告投放
			if (TaskSourceType.LANGYI.toString().equals(taskSource)) {
				url = String.format(url, adId, idfa, macToColonSeperate(mac), ip, callbackUrl, fastClick.getOSVersion(), "iphone", "", "");
			}
			//杭州点告网络技术有限公司
			if (TaskSourceType.DIANGAO.toString().equals(taskSource)) {
				url = String.format(url, adId, mac, idfa, ip, callbackUrl);
			}
			// 北京光芒星空信息技术有限公司
			if (TaskSourceType.GUANGMANG.toString().equals(taskSource)) { // 判断是否是快友世纪的应用
				url = String.format(url, idfa, callbackUrl);
			}
			// 北京快友世纪科技有限公司
			if (TaskSourceType.KUAIYOU.toString().equals(taskSource)) { // 判断是否是快友世纪的应用
				url = String.format(url, adId, mac, idfa, ip, callbackUrl);
			}
			// 广州瞬锐渠道的分发
			if (TaskSourceType.SHUNRUI.toString().equals(taskSource)) { // 判断是否是广州瞬锐的应用
				url = String.format(url, idfa, callbackUrl);
			}
			// 点开科技渠道的分发
			if (TaskSourceType.ALLDK.toString().equals(taskSource)) { // 判断是否是点开科技的应用
				url = String.format(url, appid, idfa, macAddress, Constant.SOURCE_ZHANGTONG, callbackUrl);
			}
			// 广州巨途渠道的分发
			if (TaskSourceType.GAIT.toString().equals(taskSource)) { // 判断是否是广州巨途的应用
				url = String.format(url, adId, idfa, callbackUrl);
			}
			// 掌上互动渠道的分发
			if (TaskSourceType.HUDONG.toString().equals(taskSource)) { // 判断是否是掌上互动的应用
				url = String.format(url, adId, idfa, macAddress, ip, callbackUrl);
			}
			// 派瑞威行渠道的分发
			if (TaskSourceType.PAIRUIWEIXING.toString().equals(taskSource)) { // 判断是否是派瑞威行的应用
				Long timeStamp = new Date().getTime();
				url = String.format(url, mac, adId, Constant.SOURCE_ZHANGTONG, timeStamp, idfa, callbackUrl);
			}
			// 金汇盛世渠道的分发
			if (TaskSourceType.YOUGUU.toString().equals(taskSource)) { // 判断是否是金汇盛世的应用
				url = String.format(url, mac, idfa, ip, appid, callbackUrl);
			}
			// 点客网络渠道的分发
			if (TaskSourceType.DKE.toString().equals(taskSource)) { // 判断是否是点客网络的应用
				url = url.replaceAll("ADID", adId).replaceAll("QUDAO", Constant.SOURCE_ZHANGTONG);
				url = String.format(url, idfa, appid, callbackUrl);
			}
			// 天天乐讯渠道的分发
			if (TaskSourceType.TIANTIANLEXUN.toString().equals(taskSource)) { // 判断是否是天天乐讯的应用
				url = String.format(url, Constant.SOURCE_ZHANGTONG, appid, idfa, callbackUrl);
			}
			//   北京磨盘渠道的分发
			if (TaskSourceType.MOPAN.toString().equals(taskSource)) { // 判断是否是北京磨盘的应用
				url = String.format(url, "i43", adId, mac, idfa, callbackUrl, ip);
			}
			// 广州指奥渠道的分发
			if (TaskSourceType.GUANGZHOUZHIAO.toString().equals(taskSource)) { // 判断是否是广州指奥的应用
				url = url.replaceAll("ADID", adId).replaceAll("QUDAO", Constant.SOURCE_ZHANGTONG);
				url = String.format(url, idfa, appid);
			}
			// 北京友乐活的分发
			if (TaskSourceType.YOULEHUO.toString().equals(taskSource)) { // 判断是否是北京友乐活的应用
				url = String.format(url, mac, idfa, adId, callbackUrl);
			}
			// 北京互爱的分发
			if (TaskSourceType.HUAI.toString().equals(taskSource)) { // 判断是否是北京互爱的应用
				url = url.replaceAll("URL", adId);
				url = String.format(url, mac, idfa, appid, ip, callbackUrl);
			}
			// 广州银汉渠道的分发
			if (TaskSourceType.YHSHENMO.toString().equals(taskSource)) { // 判断是否是广州银汉的应用
				url = String.format(url, appid, idfa, Constant.SOURCE_ZHANGTONG, callbackUrl);
			}
			// 氪金信息渠道的分发
			if (TaskSourceType.KEJINXINXI.toString().equals(taskSource)) { // 判断是否是氪金信息的应用
				if (null == adId) {
					return;
				}
				Long timeStamp = new Date().getTime();
				url = String.format(url, adId, idfa, ip, timeStamp, callbackUrl);
			}
			// 指盟渠道的分发
			if (TaskSourceType.ZHIMENG.toString().equals(taskSource)) { 
				url = String.format(url, adId, mac, idfa, ip, callbackUrl);
			}
			// 腾讯Wifi管家
			if (TaskSourceType.WIFIGUANJIA.toString().equals(taskSource)) { 
				url = String.format(url, appid, idfa);
			}
			// 请求第三方点击接
			if (TaskSourceType.LANGYI.toString().equals(taskSource)) {
				HashMap<String, String> header = new HashMap<>();
				header.put("WkStudioId", "159");
				result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8", header);
			} else {
				result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			}
			logger.info("广告主: {}, 点击URL: {}, 点击结果: {}", taskSource, url, result);
		}
	}

	public void callStudio(String idfa, String appId) {
		String url = "http://123.57.68.79:8081/api/common?idfa=%s&action=23&appId=%s";
		url = String.format(url, idfa, appId);
		String ppp = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
		logger.info("分发到工作室URL: {}, 结果: {}", url, ppp);
	}

	/**
	 * 激活信息上报广告主或者下送渠道
	 * @param fastActive
	 * @throws UnsupportedEncodingException
	 */
	public void callActive(FastActive fastActive) throws UnsupportedEncodingException {
		String key = RedisKeyUtil.getKey(fastActive);
		Object obj = redisClient.getObject(key);
		String url = null;
		String result = null;
		if (obj != null) {
			FastClick click = (FastClick) obj;
			redisClient.delByKey(key);

			Long taskId = Long.valueOf(click.getTaskId());
			TaskFast fast = taskFastMapper.selectByPrimaryKey(taskId);
			
			//上报广告主
			if (fast.isActiveUpload()){
			}
			else{
				//下送子渠道
				Date now = new Date();
				boolean isCallBack = now.after(fast.getStartTime()) && now.before(fast.getEndTime());
				logger.debug("Task ID:{}, StartTime:{}, EndTime:{}, Now:{}, isCallBack:{}", taskId, fast.getStartTime(), fast.getEndTime(), now, isCallBack);
				if (isCallBack) {
					String callback = click.getCallback();
					if (null != callback) {
						url = URLDecoder.decode(callback, "UTF-8");
						result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
						logger.info("分发到渠道商URL: {}, 结果: {}" , url, result);
					}
				}
			}
		}

	}

	/**
	 * 朗亿的渠道，需要单独访问一个api，然后访问一个任务的连接
	 */
	//    @Async
	//    private void forLangYi(String idfa, String mac, String taskId){
	//    	TaskFast taskFast = taskFastService.findById(Long.parseLong(taskId));
	//    	
	//    	String url = "http://prweike.apptree.com.cn/weike/channelapplist.jsp?idfa=%s&mac=%s&type=1&channelid=159";
	//    	url = String.format(url, idfa, macToColonSeperate(mac));
	//    	String apiRes = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
	//    	logger.info("朗亿任务列表:{}", apiRes);
	//    	@SuppressWarnings("unchecked")
	//		Map<String, Object> json = JSONObject.parseObject(apiRes, Map.class);
	//    	if( json!=null && json.containsKey("items") ){
	//    		String callUrl = null;
	//    		
	//    		@SuppressWarnings("unchecked")
	//			List<Map<String, Object>> items = (List<Map<String, Object>>) json.get("items");
	//    		for( Map<String, Object> item: items ){
	//    			if( item.containsKey("adudid") && StringUtils.equals( (String)item.get("adudid"), taskFast.getAdId() ) ){
	//    				callUrl = (String) item.get("url");
	//    			}
	//    		}
	//    		
	//    		logger.info("朗亿的下载地址:{}", callUrl);
	//    		if( callUrl !=null ){
	//    			HttpRequest.sendHttpRequest(callUrl, "GET", "UTF-8");
	//    		}
	//    	}
	//    }

	public static boolean isKuaiYouIDFADuplicate(String appid, String idfa) {
		if (StringUtils.isBlank(appid) || StringUtils.isBlank(idfa)) {
			return false; //乐观
		}
		String url = "http://ent.coolad.cn/queryidfa/difidfa/1461";
		HashMap<String, Object> form = new HashMap<>();
		form.put("appid", appid);
		form.put("idfa", idfa);
		try {
			String resStr = HttpRequest.postForm(url, form);
			@SuppressWarnings("unchecked")
			Map<String, String> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && res.containsKey(idfa)) {
				return res.get(idfa).equals("0") ? false : true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return false;//乐观
	}

	public static boolean isYouMiIDFADuplicate(String appid, String idfa) {
		if (StringUtils.isBlank(appid) || StringUtils.isBlank(idfa)) {
			return false;//乐观
		}
		String url = "http://cp.api.youmi.net/midiapi/querya/";
		HashMap<String, Object> form = new HashMap<>();
		form.put("appid", appid);
		form.put("idfa", idfa);
		try {
			String resStr = HttpRequest.postForm(url, form);
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && res.containsKey("c")) {
				return false;
			}
			if (res != null && res.containsKey(idfa)) {
				Integer result = (Integer) res.get(idfa);
				if (result != null && result == 0)
					return false;
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return false;//乐观
	}

	public static boolean isMoPanIDFADuplicate(String appid, String idfa) {
		if (StringUtils.isBlank(appid) || StringUtils.isBlank(idfa)) {
			return false;//乐观
		}
		String url = "http://wall.imopan.com/app/cpHasExistBatch.bin";
		HashMap<String, Object> form = new HashMap<>();
		form.put("appid", appid);
		form.put("idfa", idfa);
		form.put("source", "i43");
		try {
			String resStr = HttpRequest.postForm(url, form);
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && res.containsKey(idfa) && (Integer) res.get(idfa) == 1) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static String xushengMd5(String idfa, String source, String key){
		String md5 = new String(DigestUtils.md5Hex("588287777," + idfa + "," + source + "," + key));
		return md5;
	}

	private static String macToColonSeperate(String mac) {
		if (mac == null || mac.length() != 12)
			return mac;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mac.length(); i++) {
			sb.append(mac.charAt(i));
			if (i % 2 == 1 && i != mac.length() - 1) {
				sb.append(":");
			}
		}

		return sb.toString().toUpperCase();
	}
	public static boolean isDFHIDFADuplicate(String appid, String idfa) {
		if (StringUtils.isBlank(appid) || StringUtils.isBlank(idfa)) {
			return false;//乐观
		}
		String url = "http://222.73.26.214/interface/dereplication.asp";
		HashMap<String, Object> form = new HashMap<>();
		form.put("channel", "89");
		form.put("idfa", idfa);
		try {
			String resStr = HttpRequest.postForm(url, form);
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && res.containsKey(idfa) && (Integer) res.get(idfa) == 1) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	public static boolean isDianGaoIDFADuplicate(FastClick fastClick,
			TaskFast fast) {
		try {
			String idfa = fastClick.getIdfa();
			String appid = fast.getAppid();
			String adid = fast.getAdId();
			String mac = fastClick.getMacAddress();
			mac = (StringUtils.isEmpty(mac)) ? "020000000000" : mac;
			String ip = fastClick.getClientIP();
			ip = (null == ip) ? "" : ip;
			
			String callback = "http://ads.i43.com/api/speedTaskActive?taskSource=%s&idfa=%s&appID=%s";
			callback = String.format(callback, fast.getTaskSource().toUpperCase(), idfa, appid);
			callback = URLEncoder.encode(callback, "UTF-8");
			String url = "http://www.qumi.com/api/vendor/ios/transfernew?app=12548ab374df81ca&ad="
					+ adid
					+ "&mac="
					+ mac
					+ "&idfa="
					+ idfa
					+ "&clientip="
					+ ip + "&callback=" + callback;
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isHuiZhiIDFADuplicate(String idfa,String adid,String appid) {
		if ( StringUtils.isBlank(idfa)) {
			return false;//乐观
		}
		String url = "http://api.hongbao8888.com:83/IdfaRepeat?idfa="+idfa+"&appid="+appid+"&adid="+adid;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) == 1) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	
	public static boolean isMiDiGIDFADuplicate(String idfa,String appid) {
		if ( StringUtils.isBlank(idfa)) {
			return false;//乐观
		}
		String url = "http://api.miidi.net/cas/exist.bin?source=zhangtong&appid="+appid+"&idfa="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && "1".equals((String) res.get(idfa))) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	public static boolean isDianRuIDFADuplicate( String idfa,String adid) {
		if ( StringUtils.isBlank(idfa)) {
			return false;//乐观
		}
		//final String adid = "15927";
		String url = "http://api.mobile.dianru.com/channel/proxy.do?dr_adid=" + adid;
		//String url = "http://api.mobile.dianru.com/channel/proxy.do";
		HashMap<String, Object> form = new HashMap<>();
		form.put("dr_adid", adid);
		form.put("idfa", idfa);
		try {
			String resStr = HttpRequest.postForm(url, form);
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && res.containsKey(idfa) && (Integer) res.get(idfa) == 1) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	public static boolean isQCHDIDFADuplicate(String idfa,String appid) {
		if (StringUtils.isBlank(appid) || StringUtils.isBlank(idfa)) {
			return false; //乐观
		}
		String url = "http://aff.ihmedia.com.cn/channelinterface/filterIdfa";
		HashMap<String, Object> form = new HashMap<>();
		form.put("appId", appid);
		form.put("idfa", idfa);
		form.put("channel", "79");
		try {
			String resStr = HttpRequest.postForm(url, form);
			@SuppressWarnings("unchecked")
			Map<String, String> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && "1".equals((String) res.get(idfa))) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return false;//乐观
	}
	
	public static boolean isPLAYIDFADuplicate(String idfa, String appid) {
		try {
			String callback = "http://ads.i43.com/api/speedTaskActive?taskSource=%s&idfa=%s&appID=%s";
			callback = String.format(callback, "PLAY", idfa, appid);
			callback = URLEncoder.encode(callback, "UTF-8");
			String url = "http://www.play800.cn/other.php?url=click/ztwxclick&appid="
					+ appid + "&idfa=" + idfa + "&callback_url=" + callback;
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && 0 == (Integer) res.get(idfa)) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;// 乐观
	}
	
	public static boolean reportForDianGao(String idfa,String adid,String ip) {
		String url = "http://new.wall.qumi.com/api/opendata/idfasubmit?adid="+adid+"&app=12548ab374df81ca&idfa="+idfa+"&ip="+ip;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get("code") != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isChuangQiIDFADuplicate(String appid, String idfa,String ip) {
		if (StringUtils.isBlank(appid) || StringUtils.isBlank(idfa)) {
			return false;//乐观
		}
		String url = "http://115.29.165.234/LoveBar/IdfaRepeat?appid="+appid+"&source=xiguamei&connect=1&idfa="+idfa+"&ip="+ip;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && !"0".equals((String) res.get(idfa))) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	public static boolean isLanMaoIDFADuplicate(String adId,String idfa) {
		String url = "http://qc.cattry.com/Home/Union/qc.html?source=xiguamei&appiosid="+adId+"&idfa="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isBaiHeIDFADuplicate(FastClick fastClick,
			TaskFast fast) {
		try {
			String mac = fastClick.getMacAddress();
			mac = (StringUtils.isEmpty(mac)) ? "020000000000" : mac;
			
			//mac地址得带冒号
			String _mac ="";
			String[] s1 = mac.split("");
			for(int i=0;i<mac.length();i++){
				_mac += s1[i];
			    if(i % 2 == 1 && i != mac.length()-1) 
			    _mac += ':';
			}
			mac=_mac;
			
			String idfa = fastClick.getIdfa();
			String appid = fast.getAppid();
			String adid = fast.getAdId();
			String ip = fastClick.getClientIP();
			ip = (null == ip) ? "" : ip;
			String osVersion = fastClick.getOSVersion();
			if (null == osVersion || "".equals(osVersion)) {
				osVersion = "9.1";
			}
			String callback = "http://ads.i43.com/api/speedTaskActive?taskSource=%s&idfa=%s&appID=%s";
			callback = String.format(callback, fast.getTaskSource()
					.toUpperCase(), idfa, appid);
			callback = URLEncoder.encode(callback, "UTF-8");
        
			String url = "http://120.25.57.37:8083/task/channelReceiveTask.html?mac="
					+ mac
					+ "&adid="
					+ adid
					+ "&idfa="
					+ idfa
					+ "&clientIp="
					+ ip + "&osVersion=" + osVersion + "&callback=" + callback;
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			/*
			 if(!"".equals(resStr)){
				return true;
			}*/
		
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get("code") != 1) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isDianZhiIDFADuplicate(String app,String idfa) {
		String url = "http://a.jfq.ttdb.com/index.php?tp=jfq/checkkuaiyou&app="+app+"&idfa="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) == 1) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isChanDaShiIDFADuplicate(String appid,String idfa) {
		String url = "http://ddashi.com/shike/repeatnew.php?appid="+appid+"&source=zhuangtong&idfa="+idfa+"&key=c2tlY2NzYQ==";
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	public static boolean isZAKERIDFADuplicate(String idfa) {
		String url = "http://iphone.myzaker.com/zaker/ad/check_idfa.php?idfa="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && "1".equals((String) res.get(idfa))) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isZhiMengIDFADuplicate(String idfa) {
		String url ="http://uc.qiujian.cc:9898/uc/channel/promotion/checkIdfa?idfa="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isZMCGIDFADuplicate(String idfa) {
		String url ="https://channel.cgtz.com/channel/QueryIfas?ifa="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			if (resStr != null && !"[false]".equalsIgnoreCase(resStr)) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	public static boolean isZMHJIDFADuplicate(String idfa) {
		String url ="https://adtrack.yeshj.com/query/fanzuo/idfa?hj_app=ios_lan_cc&idfa="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isKJXXIDFADuplicate(String idfa,String code) {
		String url = "http://s2s.codrim.net/checkInstall?code="+code+"&did="+idfa;
		try {
			String resStr = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer) res.get(idfa) != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	

	
	public static boolean isWBIDFADuplicate(String idfa,String adid) {
		String url = "http://queryapi.wbddz.com/servlet/query";
		HashMap<String, Object> form = new HashMap<>();
		form.put("appid", adid);
		form.put("idfa", idfa);
		try {
			String resStr = HttpRequest.postForm(url, form);
			@SuppressWarnings("unchecked")
			Map<String, Object> res = JSONObject.parseObject(resStr, Map.class);
			if (res != null && (Integer)res.get(idfa) != 0) {
				return true;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return false;//乐观
	}
	


	public static void main(String args[]) throws IOException {
		
		/*String url = "http://localhost:8080/moneyday-api/qkClick";
		HashMap<String, Object> form = new HashMap<>();
		form.put("appid", "11");
		form.put("idfa", "22");
		form.put("ip", "1003");
		
		String resStr = HttpRequest.postForm(url, form);
	    System.out.println("resStr="+resStr);*/
		/*String str = "020000000000", result1 = "";
		String[] s1 = str.split("");
		for(int i=0;i<str.length();i++){
		    result1 += s1[i];
		    if(i % 2 == 1) result1 += ':';
		}
		System.out.println("result1="+result1);*/
		
		String idfa ="000003AA-0863-4716-A709-5859E14130AT";
		String appid1 ="586157918";
		String adId ="qARVJ3";
		String ip="114.240.86.196";
	    final String macAddress = "020000000000";
	    FastClick f = new FastClick();
	    TaskFast t = new TaskFast();
	    t.setTaskSource("QIZHUAN");
	    f.setMacAddress(macAddress);
	    f.setClientIP(ip);
	    f.setIdfa(idfa);
	    t.setAdId(adId);
	    t.setAppid(appid1);
	    JSONObject result = new JSONObject();
	  if( TaskSourceType.KEJINXINXI.toString().equals("KEJINXINXI") ){
      	//idfa去重
      	boolean isDuplicate = false;
      	if ("Ubayya".equals(t.getAdId())){
          	String code = "3573_com.wemomo.momoappdemo1";
          	isDuplicate = TaskFastCallbackService.isKJXXIDFADuplicate(f.getIdfa(),code);
      	}else if("NjY3Q3".equals(t.getAdId())){
      		String code = "3570_72172921e5d67ca672b10f5ac6aef93c";
          	isDuplicate = TaskFastCallbackService.isKJXXIDFADuplicate(f.getIdfa(),code);
      	}else if("6JjQrm".equals(t.getAdId())){
      		String code = "3575_512915857";
          	isDuplicate = TaskFastCallbackService.isKJXXIDFADuplicate(f.getIdfa(),code);
      
      	}else if("UBBbum".equals(t.getAdId())){
    		String code = "3575_588287777";
        	isDuplicate = TaskFastCallbackService.isKJXXIDFADuplicate(f.getIdfa(),code);
        	
    	}else if("qARVJ3".equals(t.getAdId())){
    		String code = "3575_923209807_cp";
        	isDuplicate = TaskFastCallbackService.isKJXXIDFADuplicate(f.getIdfa(),code);
        	System.out.println("isDuplicate="+isDuplicate);    	}
      	if( isDuplicate == true ){
      		result.put("code", 203);
      		result.put("message", "idfa重复");
      	}
      }
	}
}


