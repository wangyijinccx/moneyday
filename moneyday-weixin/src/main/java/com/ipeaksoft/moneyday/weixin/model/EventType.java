package com.ipeaksoft.moneyday.weixin.model;

public class EventType {

	public static final String SUB = "subscribe"; // 关注事件

	public static final String UNSUB = "unsubscribe"; // 取消关注事件

	public static final String CLICK = "CLICK"; // 点击菜单

	public static final String SCAN = "SCAN"; // 带参数二维码扫描事件

	public static final String LOCAL = "LOCATION"; // 上报地理位置事件

}
