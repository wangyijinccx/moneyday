import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ipeaksoft.moneyday.core.service.RedisClient;
import com.ipeaksoft.moneyday.core.util.AppRank;
import com.ipeaksoft.moneyday.core.util.AppStoreRankUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/*.xml", "classpath:mybatis/mybatis-config.xml" })
public class TestFunction {
	
	@Autowired
	AppStoreRankUtil appStoreRankUtil;
	
	@Autowired
	private RedisClient redisClient;
	
    @Test
	public void testAppRankUtil() throws Exception{
    	long startTime = System.currentTimeMillis();
		AppRank ar = appStoreRankUtil.assign();
		long endTime = System.currentTimeMillis();
		System.out.println("Spend "+ (endTime - startTime) + " ms!!!");
		System.out.println(ar);
	} 
    
//    @Test
//	public void testRedis() {
//    	long startTime = System.currentTimeMillis();
//    	redisClient.setInteger("test_key", 123);
//    	long endTime1 = System.currentTimeMillis();
//		System.out.println("Redis pool set operation spend " + (endTime1 - startTime) + " ms.");
//		redisClient.getString("test_key");
//		long endTime2 = System.currentTimeMillis();
//		System.out.println("Redis pool  get operation spend " + (endTime2 - endTime1) + " ms.");
//		redisClient.delByKey("test_key");
//		
//		Jedis jedis = new Jedis("115.28.175.196", 6379);
//		jedis.auth("123456");
//		long endTime3 = System.currentTimeMillis();
//		jedis.set("test_key", "123");
//		long endTime4 = System.currentTimeMillis();
//		System.out.println("Redis client set operation spend " + (endTime4 - endTime3) + " ms.");
//		jedis.get("test_key");
//		long endTime5 = System.currentTimeMillis();
//		System.out.println("Redis client get operation spend " + (endTime5 - endTime4) + " ms.");
//		jedis.del("test_key");
//	} 
//    
//    @Test
//   	public void testRedis1() {
//       	redisClient.setString("test_key", "123");
//   	} 
    
}
