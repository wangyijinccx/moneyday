package com.ipeaksoft.moneyday.weixin.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ipeaksoft.moneyday.core.service.BaseService;
import com.ipeaksoft.moneyday.weixin.model.SMSType;

@Service
public class SMSService extends BaseService {
	public static final String SMS_URL = "http://utf8.sms.webchinese.cn/";
	public static final String SMS_UID = "zhangtongwireless";
	public static final String SMS_KEY = "4e40d5a2077f4584278b";
	public static final String SMS_SHORTNAME = "【i43网赚】";

	public static final String SMS_CL_URL = "http://222.73.117.158/msg/HttpBatchSendSM";
	public static final String SMS_CL_UID = "tjztwx";
	public static final String SMS_CL_PSWD = "Tjztwx123";
//	public static final String SMS_SHORTNAME = "";

    @Async
    public void sendMessage(SMSType type, String mobile, String... contents) throws IOException {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("com.ipeaksoft.sms.messages", Locale.CHINESE);
        String contentTemplate = resourceBundle.getString(type.getKey());

        switch (type) {
            case CONFIRM_AUTHENTICATE_MOBILE:
                sendSMS(mobile, String.format(contentTemplate, SMS_SHORTNAME, contents[0]));
                break;
            default:
                //do nothing
        }
    }


    /**
     * @param destination Phone number
     * @param type        smsType
     * @param content     Actual content sent to user as sms
     * @throws IOException 
     */
    private void sendSMS(String destination, String... content) throws IOException {
        HttpPost post = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            logger.debug("Sending messge. [destination=" + destination + "][content=" + content[0] + "]");
            HttpClient httpClient = new DefaultHttpClient();
            post = new HttpPost(SMS_URL);
//            post = new HttpPost(SMS_CL_URL);
            post.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(initnameValuePairs(destination, content), "UTF-8");
            post.setEntity(formEntity);
            HttpResponse resp = httpClient.execute(post);
            //记录短信平台返回
            resp.getEntity().writeTo(baos);
            logger.debug("Message sent. [responseCode=" + resp.getStatusLine().getStatusCode() + "][response=" + baos.toString() + "][destination=" + destination + "][content=" + content + "]");
        } catch (UnsupportedEncodingException ex) {
        	logger.info("Exception when sending short message", ex);
        	throw ex;
        } catch (ClientProtocolException ex) {
            logger.info("Exception when sending short message", ex);
            throw ex;
        } catch (IOException ex) {
            logger.info("Exception when sending short message", ex);
            throw ex;
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
    }

    private CopyOnWriteArrayList<NameValuePair> initnameValuePairs(String destination, String... content) {
      return initWebChineseValuePairs(destination, content);
//      return initCLValuePairs(destination, content);
    }

    // 配置 webChinese 参数
    private CopyOnWriteArrayList<NameValuePair> initWebChineseValuePairs(String destination, String... content) {
        CopyOnWriteArrayList<NameValuePair> nameValuePairs = new CopyOnWriteArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("Uid", SMS_UID));
        nameValuePairs.add(new BasicNameValuePair("Key", SMS_KEY));
        nameValuePairs.add(new BasicNameValuePair("smsMob", destination));
        nameValuePairs.add(new BasicNameValuePair("smsText", content[0]));
        return nameValuePairs;
    }

    // 配置 创蓝cl2009 参数
    private CopyOnWriteArrayList<NameValuePair> initCLValuePairs(String destination, String... content) {
        CopyOnWriteArrayList<NameValuePair> nameValuePairs = new CopyOnWriteArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("account", SMS_CL_UID));
        nameValuePairs.add(new BasicNameValuePair("pswd", SMS_CL_PSWD));
        nameValuePairs.add(new BasicNameValuePair("mobile", destination));
        nameValuePairs.add(new BasicNameValuePair("msg", content[0]));
        nameValuePairs.add(new BasicNameValuePair("needstatus", "true"));
        return nameValuePairs;
    }

}
