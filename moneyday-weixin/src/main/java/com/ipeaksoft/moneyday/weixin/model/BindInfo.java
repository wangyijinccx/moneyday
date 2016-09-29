package com.ipeaksoft.moneyday.weixin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindInfo extends BaseObject{
	private static final long serialVersionUID = 1L;
	private String mobile;
	private String openid;
	private String idfa;
	private String phoneType;
	private String OSversion;
	private String system;
	private String IP;
	private String province;
	private String city;
	private String region;
	private String appVersion;
	private String clientType;

}
