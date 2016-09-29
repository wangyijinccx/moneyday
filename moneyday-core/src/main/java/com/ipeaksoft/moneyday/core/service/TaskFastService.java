package com.ipeaksoft.moneyday.core.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.dto.Dictionary;
import com.ipeaksoft.moneyday.core.dto.FastClick;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.entity.TaskFastCompleteInfo;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.entity.UserTaskFast;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastActive;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastClick;
import com.ipeaksoft.moneyday.core.entity.UserValidate;
import com.ipeaksoft.moneyday.core.mapper.TaskFastMapper;
import com.ipeaksoft.moneyday.core.mapper.UserMapper;
import com.ipeaksoft.moneyday.core.mapper.UserTaskFastActiveMapper;
import com.ipeaksoft.moneyday.core.mapper.UserTaskFastClickMapper;
import com.ipeaksoft.moneyday.core.mapper.UserTaskFastMapper;
import com.ipeaksoft.moneyday.core.mapper.UserValidateMapper;
import com.ipeaksoft.moneyday.core.util.AppRank;
import com.ipeaksoft.moneyday.core.util.AppStoreRankUtil;
import com.ipeaksoft.moneyday.core.util.Constant;
import com.ipeaksoft.moneyday.core.util.FastActive;
import com.ipeaksoft.moneyday.core.util.RedisKeyUtil;

@Service
public class TaskFastService extends BaseService {
    static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws ParseException {
        Date date = formatter.parse("2015-04-18 21:48:00");
        System.out.println(getComparedTime(date, new Date()));
    }

    @Autowired
    private TaskFastMapper           taskFastMapper;

    @Autowired
    private AppStoreRankUtil         appStoreRankUtil;

    @Autowired
    private UserValidateMapper       userValidateMapper;

    @Autowired
    private UserTaskFastClickMapper  userTaskFastClickMapper;

    @Autowired
    private UserTaskFastActiveMapper userTaskFastActiveMapper;

    @Autowired
    RedisClient                      redisClient;

    @Autowired
    private UserMapper               userMapper;

    @Autowired
    private UserTaskFastMapper       userTaskFastMapper;

    @Autowired
    UserRecordService                userRecordService;

    @Autowired
    TaskFastCallbackService          taskFastCallbackService;

    public String getByAdId(String adId) {
        return taskFastMapper.selectByAdId(adId);
    }

    public int deleteByPrimaryKey(long id) {
        return taskFastMapper.deleteByPrimaryKey(id);
    }

    public int addTaskFast(TaskFast record) {
        return taskFastMapper.insert(record);
    }
    public int addTaskFastGetId(TaskFast record) {
        return taskFastMapper.insertGetId(record);
    }

    public TaskFast getUserById(long id) {
        return taskFastMapper.selectByPrimaryKey(id);
    }

    public TaskFast getTaskByappid(String appid) {
        return taskFastMapper.selectByAppid(appid);
    }

    public int addFinished(Long id) {
        return taskFastMapper.addFinished(id);
    }

    public int reduceFinished(Long id) {
        return taskFastMapper.reduceFinished(id);
    }

    public int addFinishedByAppid(String appid) {
        return taskFastMapper.addFinishedByAppid(appid);
    }

    public int reduceFinishedByAppid(String appid) {
        return taskFastMapper.reduceFinishedByAppid(appid);
    }

    public int updateUser(TaskFast record) {
        return taskFastMapper.updateByPrimaryKeySelective(record);
    }

    public int updateByPrimaryKey(TaskFast record) {
        return taskFastMapper.updateByPrimaryKey(record);
    }

    public TaskFast findById(Long taskId) {
        return taskFastMapper.selectByPrimaryKey(taskId);
    }

    public List<TaskFast> listByIds(Collection<Long> ids) {
        return taskFastMapper.selectByIds(ids);
    }

    public List<TaskFast> findByWhere(TaskFast taskFast) {
        return taskFastMapper.findByWhere(taskFast);
    }

    // 取得总条数
    public int findCountByWhere(TaskFast taskFast) {
        return taskFastMapper.findCountByWhere(taskFast);

    }

    // 即将开始的任务
    public List<TaskFast> findNotBeginningTask(TaskFast taskFast) {
        taskFast.setSearchFrom(getNowTimestamp());
        List<TaskFast> list = taskFastMapper.findNotBeginningTask(taskFast);
        for (TaskFast tf : list) {
            // tf.setRemainTime(getSubTimestamp(taskFast.getSearchFrom(),
            // converDateToString(tf.getShowStartTime())));
            tf.setRemainTime(getComparedTime(taskFast.getSearchFrom(), tf.getShowStartTime()));
            tf.setStatus("0");// 完成
        }
        return list;
    }

    public List<TaskFast> findCompleteTask(TaskFast taskFast) {
        taskFast.setSearchFrom(getNowTimestamp());
        List<TaskFast> list = taskFastMapper.findCompleteTask(taskFast);
        for (TaskFast tf : list) {
            // tf.setRemainTime(getSubTimestamp(converDateToString(tf.getEndTime()),
            // getNowTimestamp()));
            tf.setRemainTime(getComparedTime(tf.getEndTime(), new Date()));
            tf.setStatus("1");// 完成
        }
        return list;
    }

    public List<TaskFast> findAll() {
        return taskFastMapper.selectAll();
    }

    public List<TaskFast> findUnCompleteTask(TaskFast taskFast) {
        taskFast.setSearchFrom(getNowTimestamp());
        List<TaskFast> list = taskFastMapper.findAllTask(taskFast);
        List<TaskFast> completlist = taskFastMapper.findCompleteTask(taskFast);

        if (completlist != null && completlist.size() > 0) {
            for (TaskFast tf : completlist) {
                for (TaskFast temp : list) {
                    if ((temp.getAppid()).equals(tf.getAppid())) {
                        list.remove(temp);
                        break;
                    }
                }
            }
        }
        for (TaskFast tf : list) {
            // tf.setRemainTime(getSubTimestamp(converDateToString(tf.getEndTime()),
            // taskFast.getSearchFrom()));
            tf.setRemainTime(getComparedTime(tf.getEndTime(), taskFast.getSearchFrom()));
            tf.setStatus("0");// 未完成
        }
        return list;
    }

    /**
     * 统计某天某个任务的完成情况
     * 
     * @param where
     * @return
     */
    public List<TaskFastCompleteInfo> findPageListTaskCompleteInfo(Map<String, Object> where) {
        return taskFastMapper.findPageListTaskCompleteInfo(where);
    }

    /**
     * 统计某天某个任务的完成情况总数
     * 
     * @param where
     * @return
     */
    public int findPageListTaskCompleteInfoCount(Map<String, Object> where) {
        return taskFastMapper.findPageListTaskCompleteInfoCount(where);
    }

    private static String getNowTimestamp() {
        return formatter.format(new Date());
    }

    @SuppressWarnings("unused")
    private static String converDateToString(Date date) {
        String returnValue = "";
        if (date == null) {
            return null;
        } else {
            returnValue = formatter.format(date);
        }

        return (returnValue);
    }

    @SuppressWarnings("unused")
    private static String getSubTimestamp(String start1, String end) {
        long startT = fromDateStringToLong(start1); // 定义上机时间
        long endT = fromDateStringToLong(end); // 定义下机时间
        long ss = (startT - endT) / (1000); // 共计秒数
        // int MM = (int) ss / 60; // 共计分钟数
        // int dd = (int) hh / 24; // 共计天数
        int hh = (int) ss / 3600; // 共计小时数
        // String str=dd+"天 ,"+
        String str = hh + "小时 ";
        // +MM+",分钟"+ss+",秒 ";
        return str;
    }

    @SuppressWarnings("unused")
    private static String getComparedTime(String end, String start) {
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = formatter.parse(start);
            endDate = formatter.parse(end);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return getComparedTime(endDate, startDate);
    }

    private String getComparedTime(String end, Date startDate) {
        Date endDate = null;
        try {
            startDate = formatter.parse(end);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return getComparedTime(endDate, startDate);
    }

    private static String getComparedTime(Date endDate, String start) {
        Date startDate = null;
        try {
            startDate = formatter.parse(start);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return getComparedTime(endDate, startDate);
    }

    private static String getComparedTime(Date end, Date now) {
        String str = null;
        long endTime = end.getTime(); // 应用的展示结束时间
        long nowTime = now.getTime(); // 当前时间
        long ss = (endTime - nowTime) / (1000); // 共计秒数
        // int MM = (int) ss / 60; // 共计分钟数
        // int dd = (int) hh / 24; // 共计天数
        int hh = (int) ss / 3600; // 共计小时数
        str = hh + "小时 ";
        // +MM+",分钟"+ss+",秒 ";
        return str;
    }

    private static long fromDateStringToLong(String inVal) { // 此方法计算时间毫秒
        Date date = null; // 定义时间类型
        try {
            date = formatter.parse(inVal); // 将字符型转换成日期型
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime(); // 返回毫秒数
    }

    public List<TaskFast> listBeginedTask(String mobile) {
    	
    	User user = userMapper.selectByMobile(mobile);
    	Integer weightFlag = user.getWeightFlag();
    	Integer priority = 0;
    	if(2 == weightFlag) {
    		priority = 1;
    	} else {
    		priority = 0;
    	}
    	// 获取所有展示期的任务
        List<TaskFast> list = taskFastMapper.listALLBeginedTaskNew(priority);
        // 获取当前用户已完成的任务
        List<String> complete = taskFastMapper.listCompleteBeginedTaskNew(mobile, priority);
        // 获取当前用户进行中的任务
        List<TaskFast> doingTasts = taskFastMapper.listDoingBeginedTask(mobile, priority);
        Map<String, TaskFast> doing = new HashMap<String, TaskFast>();
        for (TaskFast t : doingTasts) {
            doing.put(t.getAppid(), t);
        }
        for (String appid : complete) {
            if (doing.containsKey(appid)) {
                doing.remove(appid);
            }
        }

        for (TaskFast task : list) {
            task.setRemainTime(getComparedTime(task.getEndTime(), new Date()));
            if (complete.contains(task.getAppid())) {
                task.setStatus("1");// 完成
            } else if (doing.containsKey(task.getAppid())) {
                task.setStatus("2");// 进行中
                task.setCreateTime(doing.get(task.getAppid()).getCreateTime());
            } else {
                task.setStatus("0");// 未开始
            }
        }
        return list;
    }

    public List<TaskFast> listALLNoBegingingTaskNew(String mobile) {
    	User user = userMapper.selectByMobile(mobile);
    	Integer weightFlag = user.getWeightFlag();
    	Integer priority = 0;
    	if(2 == weightFlag) {
    		priority = 1;
    	} else {
    		priority = 0;
    	}
        return taskFastMapper.listALLNoBegingingTaskNew(priority);
    }

    @Transactional(readOnly = false)
    public JSONObject fastTaskClick(FastClick fastClick) {
        JSONObject result = new JSONObject();
        Long taskId = Long.valueOf(fastClick.getTaskId());
        TaskFast fast = taskFastMapper.selectByPrimaryKey(taskId);
        if (fast.getTotal() <= fast.getFinished()) {
            result.put("code", "1002");
            result.put("message", "任务已被领完");
            return result;
        }
        // vip账号,快速任务奖励金币乘以2（新手任务和奖励任务除外）
        TaskFast taskFast = taskFastMapper.selectByPrimaryKey(taskId);
        User user = userMapper.selectByMobile(fastClick.getMobile());
 		if(user.getWeightFlag().equals(2) && (Constant.TYPE_FAST_TASK == taskFast.getTaskType() || Constant.TYPE_SEARCH_TASK == taskFast.getTaskType())) {
 			fastClick.setPoints((taskFast.getAward() * 2) + "");
 		} else {
 			fastClick.setPoints((taskFast.getAward()) + "");
 		}
        // 查询数据库点击记录
        UserTaskFastClick utFastClick = userTaskFastClickMapper.selectByMobileAndTaskId(fastClick.getMobile(), taskId);
        if (null != utFastClick) {
            UserTaskFastClick utfClick = new UserTaskFastClick();
            utfClick.setId(utFastClick.getId());
            utfClick.setPoint(fastClick.getPoints());
            utfClick.setCreateTime(new Date());
            userTaskFastClickMapper.updateByPrimaryKeySelective(utfClick);
            result.put("code", "1000");
            result.put("message", "success");
            return result;
        } else {
        	// 记录快速任务点击记录
            UserTaskFastClick click = fastClick.convertModel();
            userTaskFastClickMapper.insert(click);
            // 存入redis
            String key = RedisKeyUtil.getKey(fastClick);
            redisClient.setObject(key, click);
            redisClient.expire(key, RedisKeyUtil.TIMEOUT_CLICK);// 设置超时时间
            
            // 对不同渠道进行异步分发点击请求
            taskFastCallbackService.callback(fastClick, fast);

            if (!Constant.NEW_TASK_APPID.equals(click.getAppid())) {
                addFinished(taskId);
            }
            // 返回
            result.put("code", "1000");
            result.put("message", "success");
            return result;
        }
    }

    @Transactional(readOnly = false)
    public JSONObject fastTaskActive(FastActive fastActive, String compareType) {
        JSONObject result = new JSONObject();
        String code = "1002";
        String message = "";
        int compare = Integer.valueOf(compareType);
        UserTaskFastClick click = null;
        UserTaskFastActive taskFastActive = null;
        String mobile = fastActive.getMobile();
        Long taskId = Long.valueOf(fastActive.getTaskId());
        if (0 == compare) { // 0为比对idfa的方式
            String key = RedisKeyUtil.getKey(fastActive);
            Object obj = redisClient.getObject(key);
            if (obj != null) {
                click = (UserTaskFastClick) obj;
                redisClient.delByKey(key);
            }
        }
        if (1 == compare) { // 1为比对URLscheme的方式
            click = userTaskFastClickMapper.selectByMobileAndTaskId(mobile, taskId);
        }
        if (null == click || !click.getIdfa().equals(fastActive.getIdfa())) { // idfa不一致则提示用户放弃任务
            code = "1005";
            message = "请放弃任务并退出i43网赚，重新进入";
        } else {
            // 查询任务激活记录
            taskFastActive = userTaskFastActiveMapper.selectByMobileAndTaskId(mobile, taskId, Constant.CLIENTTYPE_WECHAT);
            if (null == taskFastActive) {
                // 记录快速任务激活记录
                UserTaskFastActive active = toActiveObject(fastActive, click);
                userTaskFastActiveMapper.insert(active);
                // 记录快速任务明细表
                if ("1".equals(active.getStatus())) {
                    UserTaskFast fast = toUserTaskFast(click, active);
                    userTaskFastMapper.insert(fast);
                    // 记录用户积分流水
                    userRecordService.create(fast);
                    code = "1000";
                    message = "success";
                }
            } else {
                code = "1004";
                message = "已激活";
            }
        }
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    @Transactional(readOnly = false)
    public JSONObject awardTaskClick(FastClick fastClick) {

        JSONObject result = new JSONObject();
        Long taskId = Long.valueOf(fastClick.getTaskId());
        String mobile = fastClick.getMobile();
        String idfa = fastClick.getIdfa();
        if (null != mobile && null != idfa) {
        	// 获取任务奖励金币
            TaskFast taskFast = taskFastMapper.selectByPrimaryKey(taskId);
     		fastClick.setPoints((taskFast.getAward()) + "");
            // 查询数据库点击记录
            UserTaskFastClick utFastClick = userTaskFastClickMapper.selectByMobileAndTaskId(mobile, taskId);
            // 如果有点击记录，就更新点击记录和权重检测记录
            if (null != utFastClick) {
                UserTaskFastClick click = fastClick.convertModel();
                click.setId(utFastClick.getId()); // 将数据库点击记录的ID赋值到新的实体中，再根据ID更新点击记录
                userTaskFastClickMapper.updateByPrimaryKeySelective(click);
                UserValidate uv = userValidateMapper.selectByMobileAndTaskId(mobile, fastClick.getTaskId());
                if (null != uv) {
                    uv.setCreateTime(new Date());
                    int rank = appStoreRankUtil.queryRankByApp(uv.getAppId(), uv.getAppcate());
                    uv.setRank(rank);
                    userValidateMapper.updateByPrimaryKeySelective(uv);
                } else {
                    AppRank appRank = appStoreRankUtil.assign();
                    uv = new UserValidate();
                    uv.setMobile(mobile);
                    uv.setClientType(fastClick.getClientType() + "");
                    uv.setAppId(appRank.getAppid() + "");
                    uv.setTaskId(fastClick.getTaskId());
                    uv.setAppname(appRank.getName());
                    uv.setAppcate(appRank.getCat());
                    uv.setDownloadUrl(appRank.getUrl());
                    if (appRank.getIcons() != null && appRank.getIcons().length > 0) {
                        uv.setImgUrl(appRank.getIcons()[0]);
                    }
                    uv.setRank(appRank.getPos());
                    uv.setCreateTime(new Date());
                    userValidateMapper.insert(uv);
                }
                result.put("code", "1000");
                result.put("message", "success");
                result.put("appId", utFastClick.getAppid());
                result.put("appName", uv.getAppname());
                result.put("downloadUrl", uv.getDownloadUrl());
            } else { // 没有点击记录，就新增记录
                // 先判断是否被领完
                if (taskFast.getTotal() <= taskFast.getFinished()) {
                    result.put("code", "1002");
                    result.put("message", "任务已被领完");
                    return result;
                }
                // 获取应用并添加一条日志记录
                UserValidate validate = new UserValidate();
                AppRank appRank = appStoreRankUtil.assign();
                validate.setMobile(mobile);
                validate.setClientType(fastClick.getClientType() + "");
                validate.setAppId(appRank.getAppid() + "");
                validate.setTaskId(fastClick.getTaskId());
                validate.setAppname(appRank.getName());
                validate.setAppcate(appRank.getCat());
                validate.setDownloadUrl(appRank.getUrl());
                if (appRank.getIcons() != null && appRank.getIcons().length > 0) {
                    validate.setImgUrl(appRank.getIcons()[0]);
                }
                validate.setRank(appRank.getPos());
                validate.setCreateTime(new Date());
                userValidateMapper.insert(validate);

                // 记录快速（奖励任务）点击记录
                fastClick.setMobile(mobile);
                fastClick.setIdfa(idfa);
                TaskFast tf = taskFastMapper.selectByPrimaryKey(Long.valueOf(fastClick.getTaskId()));
                fastClick.setAppID(tf.getAppid());
                fastClick.setAppName(validate.getAppname());
                UserTaskFastClick click = fastClick.convertModel();
                userTaskFastClickMapper.insert(click);

                // 存入redis
                String key = RedisKeyUtil.getKey(fastClick);
                redisClient.setObject(key, click);
                redisClient.expire(key, RedisKeyUtil.TIMEOUT_CLICK);// 设置超时时间
                // 任务完成分数加1
                taskFastMapper.addFinished(taskId);
                // 返回
                result.put("code", "1000");
                result.put("message", "success");
                result.put("appId", tf.getAppid());
                result.put("appName", appRank.getName());
                result.put("downloadUrl", appRank.getUrl());
            }
        } else {
            result.put("code", "1001");
            result.put("message", "请退出公众号重新进入。");
        }
        return result;
    }

    @Transactional(readOnly = false)
    public JSONObject awardTaskActive(FastActive fastActive, String compareType) {

        JSONObject result = new JSONObject();
        String code = "1002";
        String message = "";
        String mobile = fastActive.getMobile();
        // 更新奖励任务记录下载时间
        UserValidate validate = userValidateMapper.selectByMobileAndTaskId(mobile, fastActive.getTaskId());
        if (validate != null) {
            UserValidate record = new UserValidate();
            record.setId(validate.getId());
            record.setDownloadTime(new Date());
            userValidateMapper.updateByPrimaryKeySelective(record);
        }

        // 添加快速任务激活记录和用户积分流水
        int compare = Integer.valueOf(compareType);
        UserTaskFastClick click = null;
        UserTaskFastActive taskFastActive = null;
        Long taskId = Long.valueOf(fastActive.getTaskId());
        if (0 == compare) { // 0为比对idfa的方式
            String key = RedisKeyUtil.getKey(fastActive);
            Object obj = redisClient.getObject(key);
            if (obj != null) {
                click = (UserTaskFastClick) obj;
                redisClient.delByKey(key);
            }
        }
        if (1 == compare) { // 1为比对URLscheme的方式
            // 如果在redis查询点击记录，key需要用到idfa，所以只能通过数据库查询点击记录
            click = userTaskFastClickMapper.selectByMobileAndTaskId(mobile, taskId);
        }
        if (null != click) {
            // 查询任务激活记录
            taskFastActive = userTaskFastActiveMapper.selectByMobileAndTaskId(mobile, taskId, Constant.CLIENTTYPE_WECHAT);
            if (null == taskFastActive) {
                // 更新用户的权重标志字段和奖励任务次数
                User tmpUser = userMapper.selectByMobile(mobile);
                tmpUser.setWeightFlag(1);
                int count = tmpUser.getWeightCount();
                tmpUser.setWeightCount(count + 1);
                userMapper.updateByMobileKeySelective(tmpUser);
                // 记录快速任务激活记录
                UserTaskFastActive active = toActiveObject(fastActive, click);
                userTaskFastActiveMapper.insert(active);
                // 记录快速任务明细表
                if ("1".equals(active.getStatus())) {
                    UserTaskFast fast = toUserTaskFast(click, active);
                    userTaskFastMapper.insert(fast);
                    // 记录用户积分流水
                    userRecordService.create(fast);
                    code = "1000";
                    message = "success";
                }
            } else {
                code = "1004";
                message = "已激活";
            }
        } else {
            code = "1006";
            message = "任务未领取，不能提交审核";
        }
        result.put("code", code);
        result.put("message", message);
        return result;
    }

//    private int getCompareMinutes(Date createTime, Date date) {
//        long beginTime = createTime.getTime();
//        long endTime = date.getTime();
//        long compareTime = endTime - beginTime;
//        return (int) (compareTime / 1000 / 60);
//    }

    private UserTaskFastActive toActiveObject(FastActive active, UserTaskFastClick click) {
        UserTaskFastActive record = new UserTaskFastActive();
        String idfa = active.getIdfa();
        record.setIdfa(idfa);
        long taskId = 0;
        try {
            taskId = Long.parseLong(active.getTaskId());
        } catch (NumberFormatException e) {
        }
        record.setTaskId(taskId);
        record.setIp(active.getClientIP());
        record.setCreateTime(new Date());
        record.setStatus("0");
        record.setClientType(Constant.CLIENTTYPE_WECHAT);
        if (click != null) {
            record.setMobile(click.getMobile());
            record.setAppid(click.getAppid());
            record.setStatus("1");
        }
        return record;
    }

    private UserTaskFast toUserTaskFast(UserTaskFastClick click, UserTaskFastActive active) {
        UserTaskFast record = new UserTaskFast();
        record.setMobile(click.getMobile());
        record.setTaskId(click.getTaskId());
        record.setAppid(click.getAppid());
        record.setAward(Integer.parseInt(click.getPoint()));
        record.setCreateTime(new Date());
        record.setDescription("快速任务 " + click.getAppname());
        record.setClickId(click.getId());
        record.setActiveId(active.getId());
        return record;
    }

    public List<TaskFast> listChannelTask(String channelName) {
        return taskFastMapper.listChannelTask(channelName);
    }

    public List<Dictionary> listTaskSources() {
        return taskFastMapper.listDictionary(1);
    }
    
    public List<Dictionary> listChannelNames() {
        return taskFastMapper.listDictionary(2);
    }
}
