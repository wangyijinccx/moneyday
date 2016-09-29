package com.ipeaksoft.moneyday.weixin.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


public class HttpsExecute {

	private static final String ENCODING = "UTF-8";

	private HttpClient httpClient;

	public HttpsExecute() throws NoSuchAlgorithmException,
			KeyManagementException {
		// TODO Auto-generated method stub
		X509TrustManager xtm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		SSLContext ctx = SSLContext.getInstance("TLS");
		// 使用TrustManager来初始化该上下文，TrustManager只是被SSL的Socket所使用
		ctx.init(null, new TrustManager[] { xtm }, null);
		// 创建SSLSocketFactory
		SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
		httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例

		// 通过SchemeRegistry将SSLSocketFactory注册到我们的HttpClient上
		httpClient.getConnectionManager().getSchemeRegistry().register(
				new Scheme("https", 443, socketFactory));
	}

	public String getString(String url) throws Exception {
		HttpEntity httpEntity = get(url, false);
		try {
			return EntityUtils.toString(httpEntity, ENCODING);
		} finally {
			EntityUtils.consume(httpEntity);
		}
	}

	private HttpEntity get(String url, boolean retByte) throws Exception {
		HttpGet httpGet = new HttpGet(url);
		HttpResponse httpResponse = httpClient.execute(httpGet);
		// 请求成功
		if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			return httpResponse.getEntity();
		} else {
			throw new Exception("Get: " + url + "; Error:The request failed, "
					+ httpResponse.getStatusLine().getStatusCode());
		}
	}

}
