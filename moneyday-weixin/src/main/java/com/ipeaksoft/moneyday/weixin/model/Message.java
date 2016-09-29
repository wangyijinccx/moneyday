package com.ipeaksoft.moneyday.weixin.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 请求过来的消息<br>
 * 当普通微信用户向公众账号发消息时，微信服务器将POST消息的XML数据包到开发者填写的URL上。
 * @author
 * 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name="xml")
@XmlAccessorType(XmlAccessType.FIELD)
public class Message {

    @XmlElement(name = "MsgId")
	private String msgId;

    @XmlElement(name = "groupId")
	private String groupId;// 系统标识群ID

    @XmlElement(name = "MsgType")
	private String msgType;// 消息类型

    @XmlElement(name = "Event")
	private String event;// 事件类型

    @XmlElement(name = "eventKey")
	private String eventKey;// 事件KEY�?

    @XmlElement(name = "content")
	private String content;// 文本消息内容

    @XmlElement(name = "ToUserName")
	private String toUserName;// 如：微信公号

    @XmlElement(name = "FromUserName")
	private String fromUserName;// 如：微信用户�?

    @XmlElement(name = "data")
	private String data;// 发�?的XML数据�?

    @XmlElement(name = "ticket")
	private String ticket;// 二维码扫描ticket

    @XmlElement(name = "latitude")
	private String latitude;// 地理位置纬度

    @XmlElement(name = "longitude")
	private String longitude;// 地理位置经度

    @XmlElement(name = "precision")
	private String precision;// 地理位置精度

    @XmlElement(name = "from")
	private String from;

    @XmlElement(name = "fromKey")
	private String fromKey;

    @XmlElement(name = "fromId")
	private String fromId;

    @XmlElement(name = "fansId")
	private String fansId;

    @XmlElement(name = "sceneId")
	private Integer sceneId;

    @XmlElement(name = "createDate")
	private Date createDate;

    @XmlElement(name = "cusReply")
	private boolean cusReply;
	
    @XmlElement(name = "orderId")
	private String orderId;
    
    @XmlElement(name = "orderStatus")
	private String orderStatus;
    
    @XmlElement(name = "productId")
	private String productId;
    
    @XmlElement(name = "skuInfo")
	private String skuInfo;
    
    @XmlElement(name = "CreateTime")
	private String createTime;

}
