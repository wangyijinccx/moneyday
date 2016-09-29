package com.ipeaksoft.moneyday.weixin.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.Competitor;
import com.ipeaksoft.moneyday.core.entity.CompetitorUser;
import com.ipeaksoft.moneyday.core.service.CompetitorService;
import com.ipeaksoft.moneyday.weixin.vo.CompetitorVo;

/**
 * @description 竞品相关的接口
 * @author sxy
 * 2015年3月2日 下午2:23:37
 * 
 */
@Controller
@RequestMapping(value = "/competitor")
public class CompetitorController extends BaseController {

    @Autowired
    CompetitorService      competitorService;

    public static String[] appNames = { "钱咖钥匙", "试客小兵", "小薇红包", "米赚", "口袋ATM", "红包锁屏", "欢乐试玩", "IW积分墙", "巨宝朋助手", "酷乐先锋", "部落钥匙", "疯狂赚钱", "全民欢试", "热葫芦", "玩辅助手", "小任性" };
    public static String[] schemes  = { "s782b6d12|wb4187078302|tencent801550642|wb801550642|wx39c7c84a4198a50b|QQ2FC6B132", "itry", "WeChatQuickTask",
            "feedbackapp|wxc482fb2ddcc69b0d|QQ075BCD15|tencent100371282|QQ05FB8B52|QQ2FC31054|fb58f7c87af5d", "wxfcb4821f844e8ea8|QQ41C99A54|tencent1103731284",
            "lock001|wxc17eeea161696fe7|wb2032427802|tencent1102302632", "CC0F3A3923842CCE", "g42", "", "", "earncode", "beavere|sina.546060e4fd98c53405000d8e|QQ41C6361D|wx1dad9ae3af5c5849",
            "CC0F3A3923842CCE", "rhlbx155010", "WanpuHelper", "xiaorenxing" };

    /**
     * @description 获取竞品信息 http://localhost:8080/moneyday-weixin/competitor/getSchemes
     * 2015年3月23日 下午3:37:01
     * @return 
     * Object
     * 
     */
    @ResponseBody
    @RequestMapping(value = "/getSchemes")
    public Object getSchemes() {
        JSONObject result = new JSONObject();
        int code = 1001;
        String message = "fail";
        Object applist = "";
        try {
            //            for (int i = 0; i < appNames.length; i++) {
            //                String appName = appNames[i];
            //                String[] schemesArray = schemes[i].split("\\|");
            //                for (String scheme : schemesArray) {
            //                    Competitor record = new Competitor();
            //                    record.setAppname(appName);
            //                    record.setUrlscheme(scheme+"://");
            //                    if (scheme.length()>0) {
            //                        competitorService.addCompetitor(record);
            //                    }
            //                }
            //            }
            List<Competitor> list = competitorService.getAll();
            if (list.size() > 0) {
                code = 1000;
                message = "success";
                applist = toApplist(list);
            }
        } catch (Exception e) {
            code = 1002;
            message = "异常";
            e.printStackTrace();
        }
        result.put("code", code);
        result.put("message", message);
        result.put("applist", applist);
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }

    // 上传用户的竞品信息
    /**
     * @description 上传用户的竞品信息 http://localhost:8080/moneyday-weixin/competitor/uploadSchemes?idfa=XX&mobile=XX&competitorid=X
     * 2015年3月23日 下午3:35:43
     * @return 
     * Object
     */
    @ResponseBody
    @RequestMapping(value = "/uploadSchemes")
    public Object uploadSchemes(CompetitorUser competitorUser) {
        logger.info("competitorUser: {}", competitorUser);
        JSONObject result = new JSONObject();
        int code = 1001;
        String message = "fail";
        try {
            String competitorIds = competitorUser.getCompetitorid();
            String[] competitoridArray = competitorIds.split("\\|");
            for (String competitorid : competitoridArray) {
                competitorUser.setCompetitorid(competitorid);
                // 确认竞品字典中存在该竞品信息
                Competitor competitor = competitorService.getCompetitorById(Integer.valueOf(competitorid));
                // 确认用户竞品表中未插入该用户的此条竞品记录
                Integer isExists = competitorService.countByMobileAndId(competitorUser.getMobile(), competitorid);
                if (null != competitor && 0 == isExists) {
                    competitorService.addCompetitorUser(competitorUser);
                }
            }
            code = 1000;
            message = "success";
        } catch (Exception e) {
            code = 1002;
            message = "异常";
            e.printStackTrace();
        }
        result.put("code", code);
        result.put("message", message);
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }

    private Object toApplist(List<Competitor> list) {
        List<CompetitorVo> _list = new ArrayList<CompetitorVo>();
        for (Competitor competitor : list) {
            CompetitorVo vo = new CompetitorVo();
            vo.setCompetitorid(competitor.getId());
            vo.setAppname(competitor.getAppname());
            vo.setUrlscheme(competitor.getUrlscheme());
            _list.add(vo);
        }
        return JSONObject.toJSON(_list);
    }

}