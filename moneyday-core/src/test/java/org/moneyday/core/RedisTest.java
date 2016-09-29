package org.moneyday.core;

import org.junit.Test;
import org.moneyday.AbstractSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

import com.ipeaksoft.moneyday.core.service.RedisClient;


public class RedisTest extends AbstractSpringTest{
	@Autowired
	RedisClient redis;
	
	@Test
	public void test(){
		String key = "1052978978";
		String aa = null;
		for (int i = 0 ; i < 10 ; i++){
			String idfa = IDFAGenerator.gen();
			redis.addSetItems(key, idfa);
			if (i == 5){
				aa = idfa;
			}
			if (i%10 == 0){
				logger.info("---"+i);
			}
		}
		
		long time = System.currentTimeMillis();
		boolean result = redis.sismember(key, aa);
		long time1 = System.currentTimeMillis();
		logger.info("time:{}, result:{}",time1-time, result);
		
		String bb = IDFAGenerator.gen();
		result = redis.sismember(key, bb);
		long time2 = System.currentTimeMillis();
		logger.info("time:{}, result:{}",time2-time1, result);
		
		redis.delByKey(key);
	}
}
