package com.ipeaksoft.moneyday.weixin.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.Notice;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.service.NoticeService;
import com.ipeaksoft.moneyday.core.service.UserAwardService;
import com.ipeaksoft.moneyday.core.service.UserCashService;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;
import com.ipeaksoft.moneyday.weixin.util.DateUtil;
import com.ipeaksoft.moneyday.weixin.vo.NoticeVo;

@Controller
public class NoticeController extends BaseController {

    @Autowired
    NoticeService    noticeService;

    @Autowired
    CookieUtils      cookieUtils;

    @Autowired
    UserService      userService;

    @Autowired
    UserCashService  userCashService;

    @Autowired
    UserAwardService userAwardService;

    /**
     * 5. 获取新公告 /newAnnounce
     * test example：http://localhost:8080/moneyday-weixin/newAnnounce
     * @return
     */
    @ResponseBody
    @RequestMapping("newAnnounce")
    public Object newAnnounce() {

        String mobile = cookieUtils.getMobile(request);
        Map<String, String> map = new HashMap<String, String>();
        try {
            String hasNew = noticeService.hasNewAnnounce(mobile);
            map.put("code", "1000");
            map.put("newAnnouncement", hasNew);
            map.put("message", "success");
        } catch (Exception e) {
            logger.error("ERROR:", e);
            map.put("code", "1001");
            map.put("message", "数据库异常");
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), JSONObject.toJSON(map));
        return JSONObject.toJSON(map);
    }

    /**
     * 6. 是否有可领取的奖励 /newReward
     * test example:http://localhost:8080/moneyday-weixin/newReward
     * @return
     */
    @ResponseBody
    @RequestMapping("newReward")
    public Object newReward() {

        String mobile = cookieUtils.getMobile(request);
        Map<String, String> map = new HashMap<String, String>();
        try {
            User usr = userService.getUserByMobile(mobile);
            if (null == usr) {
                map.put("code", "1013");
                map.put("message", "账号不存在");
                return JSONObject.toJSON(map);
            }
            String hasNew = noticeService.hasNewReward(mobile);
            map.put("newReward", hasNew);
            map.put("code", "1000");
            map.put("message", "success");

            return JSONObject.toJSON(map);
        } catch (Exception e) {
            logger.error("ERROR:", e);
            map.put("code", "1001");
            map.put("message", "未知异常");
            return JSONObject.toJSON(map);
        }
    }

    /**
     * 7. 当前参与任务人数，可领取金币数，累计兑换金额 /newDynamic
     * test example:http://localhost:8080/moneyday-weixin/newDynamic
     * @return
     */
    @ResponseBody
    @RequestMapping("newDynamic")
    public Object newDynamic() {
        try {
            String mobile = cookieUtils.getMobile(request);
            Map<String, Object> map = new HashMap<String, Object>();
            Integer all = userCashService.getAllMoney();
            all = (null == all) ? 0 : all / 100;
            Integer peoples = noticeService.getAllClickPeoples();
            Integer award = userAwardService.getAward(mobile);
            if (award == null) {
                map.put("reward", 0);
            } else {
                map.put("reward", award);
            }
            map.put("code", "1000");
            map.put("message", "success");
            map.put("people", peoples);
            map.put("amount", all);
            return JSONObject.toJSON(map);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> map = new HashMap<String, String>();
            map.put("code", "1002");
            map.put("message", "failue");
            return JSONObject.toJSON(map);
        }
    }

    /**
     * 8. 公告模块 /announcement
     * test example:http://localhost:8080/moneyday-weixin/announcement
     * @return
     */
    @ResponseBody
    @RequestMapping("announcement")
    public Object announcement() {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            String mobile = cookieUtils.getMobile(request);
            List<Notice> list = noticeService.selectAll(mobile);
            if (list == null || list.size() == 0) {
                map.put("code", "1000");
                map.put("allCount", 0);
                map.put("message", "success");
                return JSONObject.toJSON(map);
            } else {
                List<NoticeVo> vos = toList(list);
                map.put("code", "1000");
                map.put("allCount", list.size());
                map.put("data", vos);
                map.put("message", "success");
                return JSONObject.toJSON(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("code", "1002");
            map.put("message", "failue");
            return JSONObject.toJSON(map);
        }
    }

    /**
     * 9. 设置公告已读状态 /announceStatus
     * test example:http://localhost:8080/moneyday-weixin/announceStatus
     * @param fastActive
     * @return
     */
    @ResponseBody
    @RequestMapping("announceStatus")
    public Object announceStatus(String announceId) {

        String mobile = cookieUtils.getMobile(request);
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Notice notice = noticeService.selectByPrimaryKey(Long.parseLong(announceId));
            if (notice == null) {
                map.put("code", "1004");
                map.put("message", "查无此公告");
                return JSONObject.toJSON(map);
            } else {
                noticeService.updateReadStatus(Long.parseLong(announceId), mobile);
                map.put("code", "1000");
                map.put("message", "success");
                return JSONObject.toJSON(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("code", "1002");
            map.put("message", "failue");
            return JSONObject.toJSON(map);
        }
    }

    private List<NoticeVo> toList(List<Notice> list) {
        List<NoticeVo> ftList = new ArrayList<NoticeVo>();
        for (Notice notice : list) {
            NoticeVo vo = toNoticeVo(notice);
            ftList.add(vo);
        }
        return ftList;
    }

    private NoticeVo toNoticeVo(Notice notice) {
        NoticeVo vo = new NoticeVo();
        vo.setName(notice.getTitle());
        vo.setAnnounceId(notice.getId().toString());
        if (notice.getCreateTime() != null) {
            vo.setPublishDate(DateUtil.date2Str(DateUtil.DATEPATTERN, notice.getCreateTime()));
            vo.setPublishTime(DateUtil.getOnlyTime(notice.getCreateTime()));
        }
        if (notice.getEndTime() != null) {
            vo.setDeadDate(DateUtil.date2Str(DateUtil.DATEPATTERN, notice.getEndTime()));
            vo.setDeadTime(DateUtil.getOnlyTime(notice.getEndTime()));
        }
        vo.setDesc(StringEscapeUtils.unescapeHtml(notice.getContent()));
        vo.setImageUrl(notice.getImg());
        vo.setStatus(notice.getStatus());
        vo.setSummary(notice.getSummary());
        //		if(notice.getStatus()==null||"".equals(notice.getStatus())){
        //			vo.setStatus("0");
        //		}else{
        //			vo.setStatus("1");
        //		}
        //	vo.setStatus(notice.getStatus());
        return vo;
    }

}
