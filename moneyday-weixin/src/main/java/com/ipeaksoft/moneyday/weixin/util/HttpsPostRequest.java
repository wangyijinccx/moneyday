package com.ipeaksoft.moneyday.weixin.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * httpspost请求
 * @author chenbin
 *
 */
public class HttpsPostRequest {
	private static class TrustAnyTrustManager implements X509TrustManager {

		   public void checkClientTrusted(X509Certificate[] chain, String authType)
		     throws CertificateException {
		   }

		   public void checkServerTrusted(X509Certificate[] chain, String authType)
		     throws CertificateException {
		   }

		   public X509Certificate[] getAcceptedIssuers() {
		    return new X509Certificate[] {};
		   }
		}

		private static class TrustAnyHostnameVerifier implements HostnameVerifier {
		   public boolean verify(String hostname, SSLSession session) {
		    return true;
		   }
		}

		/**
		 * 
		 * @param url	请求地址
		 * @param param	post发送内容
		 * @return	请求返回结果
		 * @throws Exception
		 */
		public static String postReq(String url,String param) throws Exception {
		   InputStream in = null;
		   OutputStreamWriter out = null;
		   String str_return = "";
		   try {
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },
		      new java.security.SecureRandom());
		    URL console = new URL(url);
		    HttpsURLConnection conn = (HttpsURLConnection) console
		      .openConnection();
		    conn.setSSLSocketFactory(sc.getSocketFactory());
		    conn.setHostnameVerifier(new TrustAnyHostnameVerifier());

		    conn.setDoOutput(true);
		    conn.connect();
		    
		    out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            out.write(param);
            out.flush();
            out.close();
            
		    InputStream is = conn.getInputStream();
		    DataInputStream indata = new DataInputStream(is);
		    String ret = "";

		    while (ret != null) {
		     ret = indata.readLine();
		     if (ret != null && !ret.trim().equals("")) {
		      str_return = str_return
		        + new String(ret.getBytes("ISO-8859-1"), "GBK");
		     }
		    }
		    conn.disconnect();
		   } catch (ConnectException e) {
		    System.out.println("ConnectException");
		    System.out.println(e);
		    throw e;

		   } catch (IOException e) {
		    System.out.println("IOException");
		    System.out.println(e);
		    throw e;

		   } finally {
		    try {
		     in.close();
		    } catch (Exception e) {
		    }
		    try {
		     out.close();
		    } catch (Exception e) {
		    }
		   }
		   return str_return;
		}

}
