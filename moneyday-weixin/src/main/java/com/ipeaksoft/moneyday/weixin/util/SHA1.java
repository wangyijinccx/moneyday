package com.ipeaksoft.moneyday.weixin.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class SHA1 {

	public String Encrypt(String strSrc, String encName) {
		// parameter strSrc is a string will be encrypted,
		// parameter encName is the algorithm name will be used.
		// encName dafault to "MD5"
		MessageDigest md = null;
		String strDes = null;

		byte[] bt = strSrc.getBytes();
		try {
			if (encName == null || encName.equals("")) {
				encName = "MD5";
			}
			md = MessageDigest.getInstance(encName);
			md.update(bt);
			strDes = bytes2Hex(md.digest()); // to HexString
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Invalid algorithm.");
			return null;
		}
		return strDes;
	}

	public String bytes2Hex(byte[] bts) {
		String des = "";
		String tmp = null;
		for (int i = 0; i < bts.length; i++) {
			tmp = (Integer.toHexString(bts[i] & 0xFF));
			if (tmp.length() == 1) {
				des += "0";
			}
			des += tmp;
		}
		return des;
	}

	public static void main(String[] args) {
//		String string1 = "jsapi_ticket=sM4AOVdWfPE4DxkXGEs8VDKd1DuKrdGy2ZX48nH1b7OeG-1zVwat3y3OEYUJJ0mTA7GKxlDND_oOBIF8LYbw-g&noncestr=zhangtong&timestamp=1423305910447&url=http://www.g3home.com/wechat/test.jsp?code=011f92fccad255278edef72f59351fez&state=";
//		String string2 = "jsapi_ticket=sM4AOVdWfPE4DxkXGEs8VDKd1DuKrdGy2ZX48nH1b7OeG-1zVwat3y3OEYUJJ0mTA7GKxlDND_oOBIF8LYbw-g&noncestr=zhangtong&timestamp=1423306355&url=http://www.g3home.com/wechat/test.jsp?code=0216693e23f11e96557ac06b684737aO&state=";
		String string2 = "jsapi_ticket=sM4AOVdWfPE4DxkXGEs8VDKd1DuKrdGy2ZX48nH1b7OeG-1zVwat3y3OEYUJJ0mTA7GKxlDND_oOBIF8LYbw-g&noncestr=zhangtong&timestamp=1423306355&url=http://www.g3home.com/wechat/test.jsp?code=0216693e23f11e96557ac06b684737aO";
		SHA1 te = new SHA1();
		System.out.println(new Date().getTime());
		String strSrc = "可以加密汉字.Oh,and english";
		System.out.println("Source String:" + strSrc);
		System.out.println("Encrypted String:");
		System.out.println("Use Def:" + te.Encrypt(strSrc, null));
		System.out.println("Use MD5:" + te.Encrypt(strSrc, "MD5"));
		System.out.println("Use SHA:" + te.Encrypt(strSrc, "SHA-1"));
		System.out.println("Use SHA:" + te.Encrypt(string2, "SHA-1"));
		System.out.println("Use SHA-256:" + te.Encrypt(strSrc, "SHA-256"));
	}
}
