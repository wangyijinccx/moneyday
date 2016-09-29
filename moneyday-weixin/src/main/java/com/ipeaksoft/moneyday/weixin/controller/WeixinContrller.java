package com.ipeaksoft.moneyday.weixin.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.WeixinUser;
import com.ipeaksoft.moneyday.core.service.WeixinUserService;
import com.ipeaksoft.moneyday.weixin.model.Message;
import com.ipeaksoft.moneyday.weixin.service.MessageHanlerService;
import com.ipeaksoft.moneyday.weixin.util.SHA1;
import com.ipeaksoft.moneyday.weixin.util.WechatUtil;

/**
 * @author sxy
 * 2015年4月20日 下午5:00:03
 * 
 */
@Controller
public class WeixinContrller extends BaseController {
    private static final String AUTH_TOKEN = "zhangtong";
    @Autowired
    MessageHanlerService        handler;
    @Autowired
    WeixinUserService           weixinUserService;

    /**
     * 1、获取用户的推广二维码
     * 2、对weixinJS接口的参数进行SHA1加密
     * @param code
     * @param params
     * @return 
     */
    @ResponseBody
    @RequestMapping({ "share" })
    public Object share(String code, String url, String timestamp) {
        JSONObject result = new JSONObject();
        String ticket = "";
        String signature = "";
        try {
            logger.info("code: " + code);
            String openid = WechatUtil.getOpenid(code);
            logger.info("openid: " + openid);
            // 根据openid获取userid
            WeixinUser user = weixinUserService.findByOpenid(openid);
            Long userid = (null == user || null == user.getUserid()) ? 5 : user.getUserid();
            // 获取二维码链接
            ticket = WechatUtil.getTicket(userid);
            String jsapi = WechatUtil.getJsapiTicket();
            String params = "jsapi_ticket=%s&noncestr=%s&timestamp=%s&url=%s";
            params = String.format(params, jsapi, AUTH_TOKEN, timestamp, url);
            logger.info("params: " + params);
            signature = new SHA1().Encrypt(params, "SHA-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.put("ticket", ticket);
        result.put("signature", signature);
        logger.info("request url:{}, result:{}", request.getRequestURI(), JSONObject.toJSON(result));
        return JSONObject.toJSON(result);
    }
    
    // 获取用户的昵称
    @ResponseBody
    @RequestMapping(value = "nickname", method = RequestMethod.GET)
    public Object nickname(String openid) {
        JSONObject result = new JSONObject();
        String code = "1001";
        String message = "";
        String nickname = "";
            try {
                nickname = WechatUtil.getInfo(openid).getString("nickname");
                code = "1000";
                message = "success";
            } catch (Exception e) {
                e.printStackTrace();
                message = "未知异常";
            }
            result.put("code", code);
            result.put("message", message);
            result.put("nickname", nickname);
            logger.info("request url:{}, message:{}", request.getRequestURI(), result);
            return result;
    }
    
    // 内部查询使用，以手机号查询对应的OPENID
    @ResponseBody
    @RequestMapping(value = "getOpenid", method = RequestMethod.GET)
    public Object getOpenid(String mobile) {
        JSONObject result = new JSONObject();
        int code = 1001;
        Object message = "";
        if (null != mobile && mobile.matches("\\d+")) {
            WeixinUser user = weixinUserService.findByMobile(mobile);
            code = 1000;
            message = (null==user) ? "" : JSONObject.toJSON(user);
        }
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    // 显示用户关注后的消息内容
    @ResponseBody
    @RequestMapping(value = "subscribe", method = RequestMethod.POST)
    public Object subscribe(String message) {
        if (null != message) {
            logger.info("request url:{}, message:{}", request.getRequestURI(), message);
            return "success";
        }
        return "fail";
    }

    @ResponseBody
    @RequestMapping({ "/" })
    public Object index() {
        logger.debug("request param: " + request.getQueryString());
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");
        List<String> list = new ArrayList<String>();
        list.add(AUTH_TOKEN);
        list.add(timestamp);
        list.add(nonce);
        Collections.sort(list);// 参数排序
        String str = "";
        for (String s : list) {
            str = str + s;
        }
        String mySign = DigestUtils.shaHex(str);
        if (!signature.equals(mySign)) {
            // 签名错误
            logger.error("Signature error:signature:{}; mySign:{}", signature, mySign);
            return null;
        }

        // 当echostr不为空时，验证接口配置信息，直接返回echostr表示接口有效。
        String echostr = request.getParameter("echostr");
        if (StringUtils.isNotEmpty(echostr)) {
            return echostr;
        }

        String xml = getRequestXmlData(request);
        logger.debug("receive data: " + xml);
        Message message = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(Message.class);
            Unmarshaller unmar = jc.createUnmarshaller();
            message = (Message) unmar.unmarshal(new StringReader(xml));
            logger.debug("receive message: {}", message);
        } catch (JAXBException e) {
            logger.error("ERROR:", e);
        }

        return handler.handler(message);
    }

    private String getRequestXmlData(HttpServletRequest request) {
        InputStream is = null;
        String requestXmlData = null;
        try {
            is = request.getInputStream();
            // 取HTTP请求流长度
            int size = request.getContentLength();
            // 用于缓存每次读取的数据
            byte[] buffer = new byte[size];
            // 用于存放结果的数组
            byte[] xmldataByte = new byte[size];
            int count = 0;
            int rbyte = 0;
            // 循环读取
            while (count < size) {
                // 每次实际读取长度存于rbyte中
                rbyte = is.read(buffer);
                for (int i = 0; i < rbyte; i++) {
                    xmldataByte[count + i] = buffer[i];
                }
                count += rbyte;
            }
            requestXmlData = new String(xmldataByte, "UTF-8");
        } catch (IOException e) {
            logger.error("ERROR:", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("ERROR:", e);
                }
            }
        }
        return requestXmlData;
    }
}
