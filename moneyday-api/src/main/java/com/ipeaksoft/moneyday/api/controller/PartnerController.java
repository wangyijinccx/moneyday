package com.ipeaksoft.moneyday.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.IdfaApp;
import com.ipeaksoft.moneyday.core.service.IdfaAppService;
import com.ipeaksoft.moneyday.core.service.RedisClient;

@Controller
public class PartnerController extends BaseController {

	@Autowired
	RedisClient redis;
	@Autowired
	IdfaAppService idfaAppService;
	
	Set<String> partners = new HashSet<String>();

	Set<String> connPartners = Collections.synchronizedSet(new HashSet<String>());
	
	@PostConstruct
	public void init(){
		partners.add("test");
		partners.add("dianle");
		partners.add("qincheng");
		partners.add("shike");
		partners.add("rehulu");
		partners.add("kuaiyou");
		partners.add("qianlu");
		partners.add("yingyongshike");
		partners.add("qianka");
		partners.add("xianbing");
		partners.add("hudong");
		partners.add("lanmao");
		partners.add("xiguamei");
		partners.add("midi");
		partners.add("feiniu");
		partners.add("ami");
		partners.add("kaiwan");
	}

	/**
	 * 排重接口
	 * @param appid
	 * @param idfa
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/queryidfabatch", method = RequestMethod.POST)
	public Object queryidfabatch(String appid, String idfa, String partner) {
		JSONObject result = new JSONObject();
		if (StringUtils.isNotBlank(appid) && StringUtils.isNotBlank(idfa)) {
			if (!partners.contains(partner)){
				result.put("msg", "error");
				return result;
			}
			
			String key = "rateLimiter:"+partner;
			//钱咖控制速率：4次/秒， 其它：1次/2秒
			if ("qianka".equals(partner)){
				Integer cnt = redis.getInteger(key);
				if (cnt != null && cnt>4){
					result.put("msg", "please query again later.");
					return result;
				}
				if (cnt == null){
					redis.setInteger(key, 1);
					redis.expire(key, 1);
				}
				else{
					redis.incrby(key, 1);
				}
			}
			else{
				if (redis.exists(key)){
					result.put("msg", "please query again later.");
					return result;
				}
				redis.setInteger(key, 1);
				redis.expire(key, 2);
			}
			
			String [] idfas = idfa.replaceAll(" ", "").split(",");
			if (idfas.length>1000){
				result.put("msg", "too many data");
				return result;
			}
			
			List<String> idfaList = Arrays.asList(idfas);
			List<String> list = new ArrayList<String>();
			int len = idfaList.size()%200 ==0?idfaList.size()/200:idfaList.size()/200+1;
			for (int i = 0 ; i < len ; i++){
				int startIndex = i*200;
				int endIndex = (i+1)*200;
				if (endIndex > idfaList.size()){
					endIndex = idfaList.size();
				}
				list.addAll(idfaAppService.selectByAppidAndIdfas(appid, idfaList.subList(startIndex, endIndex)));
			}
			Set<String> set = new HashSet<String>();
			set.addAll(list);

			for (String item: idfas){
				if (set.contains(item)){
					result.put(item, "1");
				}
				else{
					result.put(item, "0");
				}
			}
		} else {
			result.put("msg", "error");
		}
		return result;
	}
	
	/**
	 * 激活去重
	 * @param appid
	 * @param idfa
	 * @param partner
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/queryidfaactive")
	public Object queryidfaactive(String appid, String idfa, String partner) {
		JSONObject result = new JSONObject();
		if (StringUtils.isNotBlank(appid) && StringUtils.isNotBlank(idfa)) {
			if (!partners.contains(partner)){
				result.put("msg", "error");
				return result;
			}
			int cnt = idfaAppService.selectDuplicateCntByAppidAndIdfa(appid, idfa);
			if (cnt ==0){
				result.put(idfa, "0");
			}
			else{
				result.put(idfa, "1");
			}

		} else {
			result.put("msg", "error");
		}
		return result;
	}
	
	/**
	 * 点击去重
	 * @param appid
	 * @param idfa
	 * @param partner
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/idfa/click")
	public Object click(String appid, String idfa, String partner) {
		JSONObject result = new JSONObject();
		if (StringUtils.isNotBlank(appid) && StringUtils.isNotBlank(idfa)) {
			if (!partners.contains(partner)){
				result.put("msg", "error");
				return result;
			}
			boolean exist = redis.sismember(appid, idfa);
			if (exist){
				result.put(idfa, "1");
			}
			else{
				redis.addSetItems(appid, idfa);
				int cnt = idfaAppService.selectDuplicateCntByAppidAndIdfa(appid, idfa);
				result.put(idfa, cnt == 0?"0":"1");
			}

		} else {
			result.put("msg", "error");
		}
		return result;
	}
	
	/**
	 * 查询去重
	 * @param appid
	 * @param idfa
	 * @param partner
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/queryidfa")
	public Object queryidfa(String appid, String idfa, String partner) {
		JSONObject result = new JSONObject();
		if (StringUtils.isNotBlank(appid) && StringUtils.isNotBlank(idfa)) {
			if (!partners.contains(partner)){
				result.put("msg", "error");
				return result;
			}
			int cnt = idfaAppService.selectDuplicateCntByAppidAndIdfa(appid, idfa);
			result.put(idfa, cnt == 0?"0":"1");
		} else {
			result.put("msg", "error");
		}
		return result;
	}
	
	/**
	 * 同步idfa信息，同步来源：traffic
	 * @param appid
	 * @param idfa
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/syncIdfa")
	public Object syncIdfa(String appid, String idfa) {
		JSONObject result = new JSONObject();
		if (StringUtils.isNotBlank(appid) && StringUtils.isNotBlank(idfa)) {
			int cnt = idfaAppService.selectCntByAppidAndIdfa(appid, idfa);
			IdfaApp record = new IdfaApp();
			record.setIdfa(idfa);
			record.setAppid(appid);
			record.setCreateTime(new Date());
			record.setNumorder(cnt);
			idfaAppService.insert(record);
			result.put("result", 1);
			result.put("msg", "ok");
		} else {
			result.put("result", 0);
			result.put("msg", "param error");
		}
		return result;
	}
}
