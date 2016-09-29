package com.ipeaksoft.moneyday.weixin.util;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.util.HttpRequest;

public class WechatUtil {

    public static final String APPID       = "wx496a0a7c766067c2";
    public static final String APPSECRET   = "96e93a5446e81ebb41da834b1afaed12";

    private static Logger      logger      = LoggerFactory.getLogger(WechatUtil.class.getName());
    private static String      ACCESSTOKEN = null;
    private static Date        tokenTime   = null;

    public static JSONObject getInfo(String openid){
        String token = getToken();
        String url = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=%s&openid=%s&lang=zh_CN";
        url = String.format(url, token, openid);
        String result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
        JSONObject json = JSONObject.parseObject(result);
        return json;
    }

    public static String getTicket(Long userid) throws Exception {
        String token = getToken();
        String url = "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=".concat(token);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("action_name", "QR_LIMIT_SCENE");
        JSONObject scene = new JSONObject();
        JSONObject sceneid = new JSONObject();
        sceneid.put("scene_id", userid);
        scene.put("scene", sceneid);
        jsonObject.put("action_info", scene);
        String params = jsonObject.toString();
        String result = HttpsPostRequest.postReq(url, params);
        if (!StringUtils.isBlank(result)) {
            JSONObject json = JSONObject.parseObject(result);
            if (json.containsKey("ticket")) {
                return json.getString("ticket");
            }
        }
        return null;
    }

    public static String getOpenid(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        url = String.format(url, APPID, APPSECRET, code);
        String result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
        if (!StringUtils.isBlank(result)) {
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.containsKey("openid")) {
                return jsonObject.getString("openid");
            }
        }
        return null;
    }

    // 每60分钟修改一次ACCESSTOKEN
    public static String getToken() {
        if (null != ACCESSTOKEN && 1 > compareTime(tokenTime, new Date())) {
            logger.info("ACCESSTOKEN: " + ACCESSTOKEN);
            return ACCESSTOKEN;
        } else {
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
            url = String.format(url, APPID, APPSECRET);
            String result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
            if (!StringUtils.isBlank(result)) {
                JSONObject jsonObject = JSONObject.parseObject(result);
                if (jsonObject.containsKey("access_token")) {
                    tokenTime = new Date();
                    ACCESSTOKEN = jsonObject.getString("access_token");
                }
            }
        }
        logger.info("ACCESSTOKEN: " + ACCESSTOKEN);
        return ACCESSTOKEN;
    }

    public static String getJsapiTicket() {
        String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?type=jsapi&access_token=" + getToken();
        String result = HttpRequest.sendHttpRequest(url, "GET", "UTF-8");
        if (!StringUtils.isBlank(result)) {
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.containsKey("ticket")) {
                return jsonObject.getString("ticket");
            }
        }
        return null;
    }

    private static long compareTime(Date begin, Date end) {
        long beginTime = begin.getTime();
        long endTime = end.getTime();
        long between = (endTime - beginTime) / 1000;
        long day = between / (24 * 3600);
        long hour = between % (24 * 3600) / 3600;
//        long minute = between % 3600 / 60;
//        long second = between % 60 / 60;
        return day*24+hour;
    }
}
