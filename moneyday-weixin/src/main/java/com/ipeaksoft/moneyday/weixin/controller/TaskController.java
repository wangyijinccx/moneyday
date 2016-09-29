package com.ipeaksoft.moneyday.weixin.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.dto.FastClick;
import com.ipeaksoft.moneyday.core.entity.TaskAds;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastActive;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastClick;
import com.ipeaksoft.moneyday.core.entity.UserValidate;
import com.ipeaksoft.moneyday.core.entity.WeixinUser;
import com.ipeaksoft.moneyday.core.service.FastActiveService;
import com.ipeaksoft.moneyday.core.service.FastClickService;
import com.ipeaksoft.moneyday.core.service.RedisClient;
import com.ipeaksoft.moneyday.core.service.TaskAdsService;
import com.ipeaksoft.moneyday.core.service.TaskFastService;
import com.ipeaksoft.moneyday.core.service.UserFastService;
import com.ipeaksoft.moneyday.core.service.UserRecordService;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.core.service.UserValidateService;
import com.ipeaksoft.moneyday.core.service.WeixinUserService;
import com.ipeaksoft.moneyday.core.util.AppStoreRankUtil;
import com.ipeaksoft.moneyday.core.util.Constant;
import com.ipeaksoft.moneyday.core.util.FastActive;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;
import com.ipeaksoft.moneyday.weixin.util.RedisKeyUtil;
import com.ipeaksoft.moneyday.weixin.vo.FastTask;

@Controller
public class TaskController extends BaseController {

    @Autowired
    RedisClient                 redisClient;
    @Autowired
    TaskFastService             taskFastService;
    @Autowired
    FastClickService            fastClickService;
    @Autowired
    FastActiveService           fastActiveService;
    @Autowired
    UserFastService             userFastService;
    @Autowired
    UserRecordService           userRecordService;
    @Autowired
    CookieUtils                 cookieUtils;
    @Autowired
    TaskAdsService              adsService;
    @Autowired
    private AppStoreRankUtil    appStoreRankUtil;
    @Autowired
    private UserValidateService userValidateService;
    @Autowired
    private UserService         userService;
    @Autowired
    WeixinUserService           weixinUserService;

    @ResponseBody
    @RequestMapping("unionTaskPlatform")
    public Object unionTaskPlatform() {
        List<TaskAds> list = adsService.listAll();
        List<com.ipeaksoft.moneyday.weixin.vo.TaskAds> l = convert(list);
        JSONObject result = new JSONObject();
        result.put("code", "1000");
        result.put("message", "success");
        result.put("data", l);
        return result;
    }

    private List<com.ipeaksoft.moneyday.weixin.vo.TaskAds> convert(List<TaskAds> list) {
        List<com.ipeaksoft.moneyday.weixin.vo.TaskAds> l = new ArrayList<com.ipeaksoft.moneyday.weixin.vo.TaskAds>();
        for (TaskAds item : list) {
            com.ipeaksoft.moneyday.weixin.vo.TaskAds ads = new com.ipeaksoft.moneyday.weixin.vo.TaskAds();
            ads.setUnionName(item.getName());
            ads.setIcon(item.getImage());
            ads.setDesc(item.getDescription());
            l.add(ads);
        }
        return l;
    }

    /***
     * 新手任务的状态
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("newUserTask")
    public Object newTask(HttpServletRequest request) {
        JSONObject result = new JSONObject();
        String code = "1000";
        String message = "";
        int complete = 0;
        String mobile = cookieUtils.getMobile(request);
        try {
            // 查询新手任务激活记录
            UserTaskFastActive active = fastActiveService.findByAppid(mobile, Constant.NEW_TASK_APPID, Constant.CLIENTTYPE_WECHAT);
            if (null == active) {
                message = "无激活记录";
            } else {
                message = "success";
                if ("1".equals(active.getStatus())) {
                    complete = 1;
                } else {
                    complete = 0;
                }
            }
        } catch (Exception e) {
            logger.error("ERROR:", e);
            code = "1001";
            message = "未知异常";
        }
        result.put("code", code);
        result.put("message", message);
        result.put("complete", complete);
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }

    /**
     * 获取快速任务列表
     * @return
     */
    @ResponseBody
    @RequestMapping("speedTask")
    public Object getFastTask() {
        String mobile = cookieUtils.getMobile(request);
        JSONObject result = new JSONObject();
        try {
            User user = userService.getUserByMobile(mobile);
            Boolean weight = false;
            if (null != user) {
                if (user.getWeightFlag().equals(2)) {
                    weight = true;
                } else {
                    weight = false;
                }
            } else {
                weight = false;
            }
            List<TaskFast> beginninglist = taskFastService.listBeginedTask(mobile);
            List<TaskFast> noBeginninglist = taskFastService.listALLNoBegingingTaskNew(mobile);
            result.put("code", "1000");
            result.put("message", "success");
            result.put("beginAccount", String.valueOf(beginninglist.size()));
            result.put("notBeginTaskAccount", String.valueOf(noBeginninglist.size()));
            result.put("beginningTask", toList(beginninglist, weight));
            result.put("notBeginTask", toList(noBeginninglist, weight));
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", "1002");
            result.put("message", "failue");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }

    @ResponseBody
    @RequestMapping("speedTaskMessage")
    public Object click(FastClick fastClick, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        logger.info("FastTaskClick: {}", fastClick);
        JSONObject result = new JSONObject();
        String mobile = cookieUtils.getMobile(request);
        if (StringUtils.isNotEmpty(mobile)) {
            fastClick.setMobile(mobile);
        }
        String idfa = cookieUtils.getIDFA(request);
        if (RedisKeyUtil.NEW_ASSIST_APPID.equals(fastClick.getAppID())) {
            fastClick.setIdfa("NewUser");
        } else {
            // 判断idfa是否为空，如果为空，从数据库查询得到idfa再次写入
            if (StringUtils.isEmpty(idfa)) {
                WeixinUser user = weixinUserService.findByMobile(mobile);
                idfa = user.getIdfa();
                if (StringUtils.isEmpty(idfa)) {
                    cookieUtils.setLocalIDFA(request, response, idfa);
                }
            }
            fastClick.setIdfa(idfa);
        }
        try {
            fastClick.setClientIP(getIP());
            result = taskFastService.fastTaskClick(fastClick);
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", "1001");
            result.put("message", "未知异常");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        long endTime10 = System.currentTimeMillis();
        logger.debug("speed task total spend " + (endTime10 - startTime) + " ms.");
        return result;
    }

    /**
     * 奖励任务点击 
     * @param fastClick
     * @return
     */
    @ResponseBody
    @RequestMapping("awardTaskMessage")
    public Object awardTaskClick(FastClick fastClick) {
        long startTime = System.currentTimeMillis();
        logger.info("AwardTaskClick: {}", fastClick);
        JSONObject result = new JSONObject();
        String mobile = cookieUtils.getMobile(request);
        String idfa = cookieUtils.getIDFA(request);
        fastClick.setMobile(mobile);
        fastClick.setIdfa(idfa);
        try {
            result = taskFastService.awardTaskClick(fastClick);
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", "1001");
            result.put("message", "未知异常");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        long endTime = System.currentTimeMillis();
        logger.debug("award task click spend " + (endTime - startTime) + " ms.");
        return result;
    }

    /**
     * 快速任务提交
     * @param fastActive
     * @return
     */
    @ResponseBody
    @RequestMapping("speedTaskActive")
    public Object active(FastActive fastActive) {
        long startTime = System.currentTimeMillis();
        logger.info("FastTaskActive: {}", fastActive);
        JSONObject result = new JSONObject();
        String compareType = request.getParameter("compareType");
        compareType = (null == compareType) ? "1" : compareType;

        try {
            fastActive.setClientIP(getIP());
            result = taskFastService.fastTaskActive(fastActive, compareType);
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", "1001");
            result.put("message", "未知异常");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        long endTime = System.currentTimeMillis();
        logger.debug("speedTaskActive total spend " + (endTime - startTime) + " ms.");
        return result;
    }

    /**
     * 奖励任务提交审核
     * @param fastActive
     * @return
     */
    @ResponseBody
    @RequestMapping("awardTaskActive")
    public Object awardTaskActive(FastActive fastActive) {
        long startTime = System.currentTimeMillis();
        logger.info("FastTaskActive: {}", fastActive);
        JSONObject result = new JSONObject();
        String compareType = request.getParameter("compareType");
        compareType = (null == compareType) ? "1" : compareType;
        String mobile = cookieUtils.getMobile(request);
        fastActive.setMobile(mobile);
        String idfa = fastActive.getIdfa();
        if (null == idfa || "" == idfa) {
            idfa = cookieUtils.getIDFA(request);
            fastActive.setIdfa(idfa);
        }
        try {
            result = taskFastService.awardTaskActive(fastActive, compareType);
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", "1001");
            result.put("message", "未知异常");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        long endTime = System.currentTimeMillis();
        logger.debug("award task active spend " + (endTime - startTime) + " ms.");
        return result;
    }

    /**
     * 快速任务放弃 
     * @param fastActive
     * @return
     */
    @ResponseBody
    @RequestMapping("giveUpTask")
    public Object giveUp(FastActive fastActive) {
        long startTime = System.currentTimeMillis();
        logger.info("FastTaskActive: {}", fastActive);
        JSONObject result = new JSONObject();
        String message = "";
        String code = "1002";
        try {
            // 查询对应的快速任务点击记录
            String mobile = cookieUtils.getMobile(request);
            long endTime1 = System.currentTimeMillis();
            logger.debug("1 REDIS getMobile spend " + (endTime1 - startTime) + " ms.");
            Long taskId = Long.valueOf(fastActive.getTaskId());
            // 查询任务激活记录
            UserTaskFastActive taskFastActive = fastActiveService.findByTaskId(mobile, taskId, Constant.CLIENTTYPE_WECHAT);
            long endTime2 = System.currentTimeMillis();
            logger.debug("2 SELECT ACTIVE spend " + (endTime2 - endTime1) + " ms.");
            if (null == taskFastActive) {
                UserTaskFastClick click = fastClickService.findByTaskId(mobile, taskId);
                long endTime3 = System.currentTimeMillis();
                logger.debug("3 SELECT CLICK spend " + (endTime3 - endTime2) + " ms.");
                if (null != click) {
                    // 删除对应的快速任务点击记录
                    fastClickService.deleteById(click.getId());
                    long endTime4 = System.currentTimeMillis();
                    logger.debug("4 DELETE CLICK spend " + (endTime4 - endTime3) + " ms.");
                    // 将任务的份数返还一个
                    if (!Constant.NEW_TASK_APPID.equals(click.getAppid())) {
                        taskFastService.reduceFinished(click.getTaskId());
                        long endTime5 = System.currentTimeMillis();
                        logger.debug("5 ADD FAST spend " + (endTime5 - endTime4) + " ms.");
                    }
                }
                code = "1000";
                message = "success";
            } else {
                code = "1000";
                message = "任务已激活，无法放弃";
            }
        } catch (Exception e) {
            logger.error("ERROR:", e);
            code = "1001";
            message = "未知异常";
        }
        result.put("code", code);
        result.put("message", message);
        long endTime = System.currentTimeMillis();
        logger.debug("giveUpTask total spend " + (endTime - startTime) + " ms.");
        return result;
    }

    /**
     * 放弃奖励任务
     * @param fastActive
     * @return
     */
    @ResponseBody
    @RequestMapping("giveUpAwardTask")
    public Object giveUpAwardTask(FastActive fastActive) {
        long startTime = System.currentTimeMillis();
        logger.info("FastTaskActive: {}", fastActive);
        JSONObject result = new JSONObject();
        String message = "";
        String code = "1002";
        try {
            String mobile = cookieUtils.getMobile(request);
            Long taskId = Long.valueOf(fastActive.getTaskId());
            // 查询任务激活记录
            UserTaskFastActive taskFastActive = fastActiveService.findByTaskId(mobile, taskId, Constant.CLIENTTYPE_WECHAT);
            if (null == taskFastActive) {
                // 查询对应的快速任务点击记录
                UserTaskFastClick click = fastClickService.findByTaskId(mobile, taskId);
                if (null != click) {
                    // 删除对应的快速任务点击记录
                    fastClickService.deleteById(click.getId());
                    // 将任务的份数返还一个
                    taskFastService.reduceFinished(click.getTaskId());
                }
                // 删除奖励任务记录
                UserValidate uv = userValidateService.selectByMobileAndTaskId(mobile, fastActive.getTaskId());
                if (uv != null){
                	appStoreRankUtil.assignDel(uv.getAppId());
                    userValidateService.deleteByPrimaryKey(uv.getId());
                }
                code = "1000";
                message = "success";
            } else {
                code = "1000";
                message = "任务已激活，无法放弃";
            }
        } catch (Exception e) {
            logger.error("ERROR:", e);
            code = "1001";
            message = "未知异常";
        }
        result.put("code", code);
        result.put("message", message);
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        long endTime = System.currentTimeMillis();
        logger.debug("award task give spend " + (endTime - startTime) + " ms.");
        return result;
    }

    private List<FastTask> toList(List<TaskFast> list, Boolean weight) {
        List<FastTask> ftList = new ArrayList<FastTask>();
        for (TaskFast tf : list) {
            FastTask vo = toFastTask(tf, weight);
            ftList.add(vo);
        }
        return ftList;
    }

    private String getIP() {
         String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        String IP = request.getRemoteAddr();
        String forwarded = request.getHeader("x-forwarded-for");

        if (forwarded != null) {
            forwarded = forwarded.split(",", 2)[0];
            if (pattern.matcher(forwarded).matches()) {
                return forwarded;
            }
        }
        if (pattern.matcher(IP).matches()) {
            return IP;
        } else {
            logger.warn("IP is not valid.[IP=" + IP + "]");
            return "";
        }
    }

    private FastTask toFastTask(TaskFast taskFast, Boolean weight) {
        FastTask record = new FastTask();
        record.setTaskId(taskFast.getId().toString());
        record.setIcon(taskFast.getImg());
        record.setAppID(taskFast.getAppid());
        record.setName(taskFast.getTaskname());
        record.setShortDesc(taskFast.getDescription());
        record.setRemainNum((taskFast.getTotal() - taskFast.getFinished()) + "");
        record.setRemainTime(taskFast.getRemainTime());
        // vip账号,快速任务奖励金币乘以2（新手任务和奖励任务除外）
        if (weight && (Constant.TYPE_FAST_TASK == taskFast.getTaskType() || Constant.TYPE_SEARCH_TASK == taskFast.getTaskType())) {
            record.setReward(Float.valueOf((float) taskFast.getAward()) * 2);
        } else {
            record.setReward(Float.valueOf((float) taskFast.getAward()));
        }
        record.setStatus(taskFast.getStatus());
        record.setDownloadUrl(taskFast.getDownloadUrl());
        record.setLimit(taskFast.getLimit() + "");
        if ("2".equals(taskFast.getStatus())) {
            record.setClickTime(format(taskFast.getCreateTime()));
        }
        if (taskFast.getSize() != null) {
            record.setSize(("[").concat(taskFast.getSize().toString()).concat("MB]"));
        } else {
            record.setSize("");
        }
        if (taskFast.getTotal() != null) {
            record.setTotal(taskFast.getTotal().toString());
        }
        if (taskFast.getPriority() != null) {
            record.setAdvancedTask(taskFast.getPriority().toString());
        }
        record.setType(String.valueOf(taskFast.getTaskType()));
        record.setSequence(String.valueOf(taskFast.getSearchOrderNum()));
        record.setKeyword(taskFast.getKeyWord());
        if (taskFast.getUrlscheme() != null) {
            record.setUrlscheme(taskFast.getUrlscheme());
        } else {
            record.setUrlscheme("");
        }
        if (taskFast.getCompareType() != null) {
            record.setCompareType(taskFast.getCompareType());
        } else {
            record.setCompareType("");
        }
        if (taskFast.getOperateStep() != null) {
            record.setOperateStep(taskFast.getOperateStep());
        } else {
            record.setOperateStep("");
        }
        if (taskFast.getProcessName() != null) {
            record.setProcessName(taskFast.getProcessName());

        } else {
            record.setProcessName("");
        }
        return record;
    }

    private String format(Date date) {
        if (date == null)
            return null;
        return formatter.format(date);
    }

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
}
