package com.zhangtong.core.appstore.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

@Service
public class RedisService extends BaseService {
	@Autowired
	StringRedisTemplate template;

	public void expire(String key, final long timeout) {
		template.expire(key, timeout, TimeUnit.SECONDS);
	}
	
	public boolean exists(String key){
		return template.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection)
					throws DataAccessException {
				RedisSerializer<String> serializer = template
						.getStringSerializer();
				return connection.exists(serializer.serialize(key));
			}
		}, false, false);
	}

	public void incr(String key) {
		template.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection)
					throws DataAccessException {
				RedisSerializer<String> serializer = template
						.getStringSerializer();
				connection.incr(serializer.serialize(key));
				return true;
			}
		}, false, false);
	}

	public void setInteger(String key, Integer value) {
		template.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection)
					throws DataAccessException {
				RedisSerializer<String> serializer = template
						.getStringSerializer();
				connection.set(serializer.serialize(key), serializer.serialize(value+""));
				return true;
			}
		}, false, false);
	}

	public Integer getInteger(String key) {
		return template.execute(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection)
					throws DataAccessException {
				Integer result = null;
				RedisSerializer<String> serializer = template
						.getStringSerializer();
				byte[] bkey = serializer.serialize(key);
				if (connection.exists(bkey)){
					String r = serializer.deserialize(connection.get(bkey));
					result = Integer.valueOf(r);
				}
				return result;
			}
		}, false, false);
	}

	public boolean setKey(String key, final String value) {
		boolean result = template.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection)
					throws DataAccessException {
				RedisSerializer<String> serializer = template
						.getStringSerializer();
				connection.set(serializer.serialize(key), serializer.serialize(value));
				return true;
			}
		}, false, false);
		return result;
	}
	
	public void del(String key) {
		template.delete(key);
	}

}
