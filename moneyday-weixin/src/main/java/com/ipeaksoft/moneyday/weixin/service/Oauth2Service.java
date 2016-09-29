package com.ipeaksoft.moneyday.weixin.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.weixin.util.HttpsExecute;

@Service
public class Oauth2Service {
	private static final String APP_ID = "wx496a0a7c766067c2";
	private static final String APP_SECRET = "96e93a5446e81ebb41da834b1afaed12";
	Logger logger = LoggerFactory.getLogger(Oauth2Service.class);


	/**
	 * 获取跳转授权页面的URL
	 * 
	 * @param fromKey
	 *            重定向后会带上fromKey参数
	 * @param backurl
	 *            授权后重定向的回调链接地址
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String getOauth2BaseUrl(String fromKey, String backurl)
			throws UnsupportedEncodingException {
		String OAYTH2_URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
				+ APP_ID;
		return OAYTH2_URL + "&redirect_uri="
				+ URLEncoder.encode(backurl, "UTF-8")
				+ "&response_type=code&scope=snsapi_base&state=" + fromKey
				+ "#wechat_redirect";
	}

	/**
	 * 通过code获取网页授权得到的openid
	 * 
	 * @param code
	 *            网页授权返回code
	 * @return 微信用户openid
	 * @throws ApiException
	 *             调用接口出错
	 */
	public JSONObject getOauth2BaseResult(String code) {
		String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + APP_ID
				+ "&secret=" + APP_SECRET + "&code=" + code
				+ "&grant_type=authorization_code";
		String result = GET(url);
		return JSONObject.parseObject(result);
	}

	public String getOauth2UserinfoUrl(String code, String backurl)
			throws UnsupportedEncodingException {
		String OAYTH2_URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
				+ APP_ID;
		return OAYTH2_URL + "&redirect_uri="
				+ URLEncoder.encode(backurl, "UTF-8")
				+ "&response_type="+code+"&scope=snsapi_userinfo&state=STATE"
				+ "#wechat_redirect";
	}

	public JSONObject getOauth2UserinfoResult(String openid, String access_token){
		String url = "https://api.weixin.qq.com/sns/userinfo?access_token="+access_token+"&openid="+openid+"&lang=zh_CN";
		String result = GET(url);
		return JSONObject.parseObject(result);
	}
	
	private String GET(String url){
		try {
			HttpsExecute execute = new HttpsExecute();
			String result = execute.getString(url);
			logger.info("Oauth2 get url: {}, return: {}", url, result);
			return result;
		} catch (Exception e) {
			logger.error("ERROR:", e);
		}
		return null;
	}

}
