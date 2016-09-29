package com.ipeaksoft.moneyday.core.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.util.Channel;
import com.ipeaksoft.moneyday.core.util.PersistRedisKey;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * 给朗亿专门用的service...
 * @author liyazi
 * @date 2015年10月14日
 */
@Service
public class LangYiService {

	@Resource(name = "writeRedisPool")
	protected JedisPool jedisPool;

	public int getTaskIdByLangYiAdid(String adid, Channel channel) {
		if (StringUtils.isBlank(adid))
			return 0;
		String key = null;
		switch (channel) {
		case XIGUAMEI:
			key = PersistRedisKey.LangYiTaskMap.name();
			break;
		case TAOJINZHE:
			key = PersistRedisKey.LangYiTaskMapTJZ.name();
			break;
		}
		if (key == null)
			return 0;
		Jedis j = jedisPool.getResource();
		try {
			int res = NumberUtils.toInt(j.hget(key, adid));
			return res;
		} finally {
			j.close();
		}
	}

	public void setTaskId(String adid, Long taskId, Channel channel) {
		if (StringUtils.isBlank(adid))
			return;
		String key = null;
		switch (channel) {
		case XIGUAMEI:
			key = PersistRedisKey.LangYiTaskMap.name();
			break;
		case TAOJINZHE:
			key = PersistRedisKey.LangYiTaskMapTJZ.name();
			break;
		}
		if (key == null)
			return;
		Jedis j = jedisPool.getResource();
		try {
			j.hset(key, adid, taskId + "");
		} finally {
			j.close();
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Map<String, Object>> getTaskJson(Collection<String> adids, Channel channel) {
		if (adids == null || adids.size() == 0)
			return Collections.emptyMap();
		String key = null;
		switch( channel ){
		case XIGUAMEI:
			key = PersistRedisKey.LangYiTaskJsonMap.name();
			break;
		case TAOJINZHE:
			key = PersistRedisKey.LangYiTaskJsonMapTJZ.name();
			break;
		}
		if( key == null )
			return Collections.emptyMap();
		
		Jedis j = jedisPool.getResource();
		try {
			Pipeline p = j.pipelined();
			Map<String, Map<String, Object>> res = new HashMap<>();
			Map<String, Response<String>> resResponse = new HashMap<>();
			for (String adid : adids) {
				resResponse.put(adid, p.hget(key, adid));
			}
			p.sync();
			Iterator<String> iter = resResponse.keySet().iterator();
			while (iter.hasNext()) {
				String adid = iter.next();
				res.put(adid, (Map<String, Object>) JSONObject.parseObject(resResponse.get(adid).get(), Map.class));
			}
			return res;
		} finally {
			j.close();
		}
	}

	public void setTaskJson(String adid, String json, Channel channel) {
		if (StringUtils.isBlank(adid))
			return;
		String key = null;
		switch (channel) {
		case XIGUAMEI:
			key = PersistRedisKey.LangYiTaskJsonMap.name();
			break;
		case TAOJINZHE:
			key = PersistRedisKey.LangYiTaskJsonMapTJZ.name();
			break;
		}
		if (key == null)
			return;
		Jedis j = jedisPool.getResource();
		try {
			j.hset(key, adid, json);
		} finally {
			j.close();
		}
	}

	public void clearOnlineTask() {
		Jedis j = jedisPool.getResource();
		try {
			String key = PersistRedisKey.LangYiOnlineTask.name();
			j.del(key);
		} finally {
			j.close();
		}
	}

	public void addOnlineTask(String adid) {
		if (StringUtils.isBlank(adid))
			return;
		Jedis j = jedisPool.getResource();
		try {
			String key = PersistRedisKey.LangYiOnlineTask.name();
			j.sadd(key, adid);
		} finally {
			j.close();
		}
	}

	public Set<String> getOnlineTask() {
		Jedis j = jedisPool.getResource();
		try {
			String key = PersistRedisKey.LangYiOnlineTask.name();
			return j.smembers(key);
		} finally {
			j.close();
		}
	}

}
