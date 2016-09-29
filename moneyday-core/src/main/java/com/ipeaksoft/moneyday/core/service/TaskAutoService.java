package com.ipeaksoft.moneyday.core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ipeaksoft.moneyday.core.entity.TaskAuto;
import com.ipeaksoft.moneyday.core.enums.AutoTaskSource;
import com.ipeaksoft.moneyday.core.mapper.TaskAutoMapper;
import com.ipeaksoft.moneyday.core.util.PersistRedisKey;

@Service
public class TaskAutoService extends BaseService {
    @Autowired
    private TaskAutoMapper mapper;

    @Autowired
    private RedisClient redis;


    public int deleteByPrimaryKey(Long id){
    	return mapper.deleteByPrimaryKey(id);
    }

    public int insert(TaskAuto record){
    	return mapper.insert(record);
    }

    public TaskAuto selectByPrimaryKey(Long id){
    	return mapper.selectByPrimaryKey(id);
    }

    public TaskAuto selectByAdidAndSource(String adid, AutoTaskSource source){
    	return mapper.selectByAdidAndSource(adid, source);
    }

    public List<TaskAuto> selectByAdidsAndSource(Collection<String> adids, AutoTaskSource source){
    	return mapper.selectByAdidsAndSource(adids, source);
    }

    public List<TaskAuto> selectALL(){
    	return mapper.selectALL();
    }

    public int save(TaskAuto record){
    	TaskAuto result = mapper.selectByAdidAndSource(record.getAdid(), record.getSource());
    	if (result == null){
    		return mapper.insert(record);
    	}
    	else{
    		record.setId(result.getId());
    		return mapper.updateByPrimaryKeySelective(record);
    	}
    }

    public int updateByPrimaryKey(TaskAuto record){
    	return mapper.updateByPrimaryKeySelective(record);
    }

	public void clearOnlineTask(String key) {
		redis.delByKey(key);
	}

	public void addOnlineTask(String key, Set<String> adids) {
		redis.addSet(key, adids);
	}
	
	public Set<String> getOnlineTask(String key) {
		return redis.getSet(key);
	}
//
//	public void clearOnlinePrizeTask(String key) {
//		redis.delByKey(key);
//	}
//
//	public void addOnlinePrizeTask(String key, Set<String> adids) {
//		redis.addSet(key, adids);
//	}
//	
//	public Set<String> getOnlinePrizeTask(String key) {
//		return redis.getSet(key);
//	}

	public void setTaskId(PersistRedisKey key, String adid, Long taskId) {
		if (StringUtils.isBlank(adid))
			return;
		redis.addMapValue(key.name(), adid, taskId+"");
	}
	
	public void setTaskPrice(PersistRedisKey key, String adid, String price) {
		if (StringUtils.isBlank(adid))
			return;
		redis.addMapValue(key.name(), adid, price);
	}

	public Map<String, String> getTaskPrice(PersistRedisKey key) {
		return redis.getMap(key.name());
	}

	public long getTaskIdByAdid(PersistRedisKey key, String adid){
		String result = redis.getMapItem(key.name(), adid);
		if (StringUtils.isNotBlank(result)){
			return NumberUtils.toLong(result);
		}
		return 0;
	}
	
	public Map<String, String> getTaskIdMap(PersistRedisKey key){
		return redis.getMap(key.name());
	}

}