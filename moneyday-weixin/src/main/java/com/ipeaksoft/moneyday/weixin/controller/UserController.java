package com.ipeaksoft.moneyday.weixin.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.TaskSearch;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.core.service.WeixinUserService;
import com.ipeaksoft.moneyday.core.util.DateUtil;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;
import com.ipeaksoft.moneyday.weixin.vo.TaskBillDetail;

@Controller
@RequestMapping(value = "User")
public class UserController extends BaseController {
    @Autowired
    UserService userService;
    @Autowired
    CookieUtils cookieUtils;
    @Autowired 
    WeixinUserService weixinUserService;

    @ResponseBody
    @RequestMapping("MyInfomation")
    public Object MyInfomation() {
        JSONObject result = new JSONObject();
        User user = getWeixinUser(request);
        if (user != null) {
        	Integer weightFlag = user.getWeightFlag();
            String status = userService.getValidStatus(user.getMobile());
            result.put("code", "1000");
            result.put("message", "success");
            result.put("userID", user.getUserid().toString());
            result.put("name", user.getName());
            result.put("mobile", user.getMobile());
            result.put("address", user.getAddress());
            result.put("userType", user.getFromto());
            if(2 == weightFlag) {
            	result.put("highUser", "1");
            } else {
            	result.put("highUser", "0");
            }
            result.put("status", status);
            result.put("exception", user.getStatus());
            String nickname = (null==user.getNickname()) ? "" : user.getNickname();
            result.put("nickname", nickname);
            String headimgurl = (null==user.getHeadimgurl()) ? "" : user.getHeadimgurl();
            result.put("headimgurl", headimgurl);
            if (user.getAppleAccount() == null) {
                result.put("appleId", "");
            } else {
                result.put("appleId", user.getAppleAccount());
            }
        } else {
            result.put("code", "1001");
            result.put("message", "账号异常");
        }

        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }

    @ResponseBody
    @RequestMapping("MyInfomation/money")
    public Object money() {
        JSONObject result = new JSONObject();
        User user = getUser(request);
        if (user != null) {
            Integer total = userService.getTotalAwardByMobile(user.getMobile());
            result.put("code", "1000");
            result.put("message", "success");
            if (total != null) {
                result.put("allIncome", total);
            } else {
                result.put("allIncome", 0);
            }
            if (user.getAward() == null) {
                result.put("balance", 0);
            } else {
                result.put("balance", user.getAward());
            }
            Integer i = userService.getTodayAwardByUserId(user.getMobile());
            if (i == null) {
                result.put("todayIncome", 0);
            } else {
                result.put("todayIncome", i);
            }
        } else {
            result.put("code", "1001");
            result.put("message", "账号异常");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }

    /**
     * 10.	任务收入明细 
     * test example：http://localhost:8080/moneyday-weixin/User/theTaskBillDetail?clientType=0&page=1&pagesize=50
     * @return
     */
    @ResponseBody
    @RequestMapping("theTaskBillDetail")
    public Object theTaskBillDetail(String page, String pagesize) {
        JSONObject result = new JSONObject();
        try {
            Map<String, Object> searchMap=  new HashMap<String, Object>();
            searchMap.put("page", Integer.parseInt(page));
            searchMap.put("pagesize", Integer.parseInt(pagesize));
            searchMap.put("mobile", cookieUtils.getMobile(request));
            List<TaskSearch> list = userService.findTaskBillDetailByWhere(searchMap);
            result.put("code", "1000");
            result.put("message", "success");
            if(list!=null){
                result.put("account", list.size());
                result.put("data", toList(list));
            }
        } catch (Exception e) {
            logger.error("ERROR:", e);
            result.put("code", "1002");
            result.put("message", "插入异常");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }
    
    private List<TaskBillDetail> toList(List<TaskSearch> list){
        List<TaskBillDetail> ftList = new ArrayList<TaskBillDetail>();
        for(TaskSearch notice:list){
            TaskBillDetail vo = toTaskBillDetail(notice);
            ftList.add(vo);
        }
        return ftList;
    }
    
    private TaskBillDetail toTaskBillDetail(TaskSearch taskSearch){
        TaskBillDetail vo= new TaskBillDetail();
        vo.setName(taskSearch.getTaskname());
        vo.setIncome(taskSearch.getAward().toString());
        vo.setDate(DateUtil.date2Str(taskSearch.getCreateTime()));
        vo.setTime(DateUtil.getOnlyTime(taskSearch.getCreateTime()));
        vo.setTaskType(taskSearch.getTaskType());
        return vo;
    }

    private User getUser(HttpServletRequest request) {
        String mobile = cookieUtils.getMobile(request);
        if (StringUtils.isNotEmpty(mobile)) {
            return userService.getUserByMobile(mobile);
        }
        return null;
    }

    private User getWeixinUser(HttpServletRequest request) {
        String mobile = cookieUtils.getMobile(request);
        if (StringUtils.isNotEmpty(mobile)) {
            return userService.getWeixinUserByMobile(mobile);
        }
        return null;
    }
}
