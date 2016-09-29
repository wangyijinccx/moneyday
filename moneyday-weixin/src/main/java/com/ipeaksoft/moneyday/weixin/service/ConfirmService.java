package com.ipeaksoft.moneyday.weixin.service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ipeaksoft.moneyday.core.service.BaseService;
import com.ipeaksoft.moneyday.weixin.model.SMSType;

@Service
public class ConfirmService extends BaseService {

    /**
     * The interval time for sending another sms to one mobile, in ms
     */
	private static final long INTERVAL_SMS = 60 * 1000;

    /**
     * Default TTL (time to live) in ms for sms confirmation
     */
	private static final long TTL_DEFAULT = 10 * 60 * 1000;

    
    @Autowired
    SMSService smsService;

    private static final Map<SMSType, ConcurrentHashMap<String, Map<String, Pair<Long, Long>>>> smsCaptchaMap = new HashMap<>();

    private static final Map<SMSType, ConcurrentHashMap<String, String>> smsCaptchaIndex = new ConcurrentHashMap<>();

    @PostConstruct
    private void init(){
    	SMSType[] types = SMSType.values();
        for (SMSType type : types) {
            if (!smsCaptchaMap.containsKey(type)) {
                ConcurrentHashMap<String, Map<String, Pair<Long, Long>>> map = new ConcurrentHashMap<>();
                smsCaptchaMap.put(type, map);
            }
            if (!smsCaptchaIndex.containsKey(type)) {
                ConcurrentHashMap<String, String> index = new ConcurrentHashMap<>();
                smsCaptchaIndex.put(type, index);
            }
        }
    }
    public boolean sendCaptcha(String mobileNumber, SMSType type) {
        ConcurrentHashMap<String, Map<String, Pair<Long, Long>>> captchaMap = getCaptchaMap(type);
        ConcurrentHashMap<String, String> captchIndex = getCaptchaIndex(type);

        String captcha = randomCaptcha(captchIndex);
        if (captchaMap.containsKey(mobileNumber)) {
            long time = System.currentTimeMillis();
            for (Entry<String, Pair<Long, Long>> entry : captchaMap.get(mobileNumber).entrySet()) {
                if (entry.getValue().getLeft() + TTL_DEFAULT < time) { //sent before but this one expired
                    expired(mobileNumber, entry.getKey(), type);
                    logger.debug("Captcha " + captcha + " expired, been send to " + mobileNumber + " at " + new Date(time).toString());
                    continue;
                } else if (entry.getValue().getRight() + INTERVAL_SMS < time) { //sent before, not expired and after interval already, so we send again
                    try {
						smsService.sendMessage(type, mobileNumber, entry.getKey());
					} catch (IOException e) {
						return false;
					}
                    long createTime = entry.getValue().getLeft();
                    entry.setValue(Pair.of(createTime, time));
                    return true;
                } else {
                    logger.debug("Captcha " + entry.getKey() + " was sent to " + mobileNumber + " at " + (new Date(time).toString()) + ", still in inerval so skip this try.");
                    return false;
                }
            }
            //here we have all captcha expired actually, so send
            try {
				smsService.sendMessage(type, mobileNumber, captcha);
			} catch (IOException e) {
				return false;
			}
            sent(mobileNumber, captcha, type);
            return true;
        } else {
            try {
				smsService.sendMessage(type, mobileNumber, captcha);
			} catch (IOException e) {
				return false;
			}
            sent(mobileNumber, captcha, type);
            return true;
        }
    }

    public Map<String, Pair<Long, Long>> getCapByTypeMobile(SMSType type, String mobile) {
        Map<String, Map<String, Pair<Long, Long>>> captchaMap = getCaptchaMap(type);
        Map<String, Pair<Long, Long>> map = captchaMap.get(mobile);
        return map;
    }

    public String checkMobile(String captcha, SMSType type) {
        long time = System.currentTimeMillis();
        Map<String, Map<String, Pair<Long, Long>>> captchaMap = getCaptchaMap(type);
        Map<String, String> captchaInex = getCaptchaIndex(type);

        String mobile = captchaInex.get(captcha);
        if (mobile != null) {
            Map<String, Pair<Long, Long>> map = captchaMap.get(mobile);
            if (map.containsKey(captcha) && map.get(captcha).getLeft() + TTL_DEFAULT > time) {
                return mobile;
            }
        }
        return null;
    }

    public void clearMobile(String mobile, SMSType type) {
        Map<String, Map<String, Pair<Long, Long>>> captchaMap = getCaptchaMap(type);
        Map<String, String> captchaInex = getCaptchaIndex(type);

        if (captchaMap.containsKey(mobile)) {
            for (String captcha : captchaMap.get(mobile).keySet()) {
                if (!mobile.equalsIgnoreCase(captchaInex.remove(captcha))) {
                    logger.warn("Inconsistence Captcha -> Mobile index.");
                }
            }
        }
        captchaMap.remove(mobile);
    }

    /**
     * Generate UNIQUE numeric captcha in given size
     *
     * @param captchaIndex
     * @return
     */
    private String randomCaptcha(Map<String, String> captchaIndex) {
        String captcha = RandomStringUtils.randomNumeric(4);
        if (captchaIndex.containsKey(captcha)) {    //we need to generate UNIQUE captcha
            logger.warn("Captcha " + captcha + " already exist! Sent to " + (captchaIndex.get(captcha)));
            return randomCaptcha(captchaIndex);
        } else {
            return captcha;
        }
    }

    private void sent(String mobile, String captcha, SMSType type) {
        long time = System.currentTimeMillis();
        ConcurrentHashMap<String, Map<String, Pair<Long, Long>>> captchaMap = getCaptchaMap(type);
        ConcurrentHashMap<String, String> captchaIndex = getCaptchaIndex(type);
        if (!captchaMap.containsKey(mobile)) {
            Map<String, Pair<Long, Long>> map = new HashMap<>();
            captchaMap.putIfAbsent(mobile, map);
        }
        captchaMap.get(mobile).put(captcha, Pair.of(time, time));
        captchaIndex.put(captcha, mobile);
        logger.debug("Captcha " + captcha + " sent to " + mobile + " at " + new Date(time).toString());
    }

    private void expired(String mobile, String captcha, SMSType type) {
        getCaptchaMap(type).get(mobile).remove(captcha);
        getCaptchaIndex(type).remove(captcha);
    }

    private ConcurrentHashMap<String, Map<String, Pair<Long, Long>>> getCaptchaMap(SMSType type) {
        ConcurrentHashMap<String, Map<String, Pair<Long, Long>>> result = smsCaptchaMap.get(type);
        if (result == null) {
            throw new IllegalArgumentException(String.format("SMSType %s not registered in ConfirmBean", type));
        }
        return result;
    }

    private ConcurrentHashMap<String, String> getCaptchaIndex(SMSType type) {
        ConcurrentHashMap<String, String> result = smsCaptchaIndex.get(type);
        if (result == null) {
            throw new IllegalArgumentException(String.format("SMSType %s not registered in ConfirmBean", type));
        }
        return result;
    }
}
