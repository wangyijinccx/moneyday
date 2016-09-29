package com.ipeaksoft.moneyday.weixin.model;

public enum SMSType{

    /**
     * 用户账号绑定手机
     */
    CONFIRM_AUTHENTICATE_MOBILE("confirm.ipeaksoft.authenticate.mobile");


    private final String key;

    private SMSType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}