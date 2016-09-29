package com.ipeaksoft.moneyday.weixin.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.Device;
import com.ipeaksoft.moneyday.core.entity.UserTaskFast;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastActive;
import com.ipeaksoft.moneyday.core.entity.UserTaskFastClick;
import com.ipeaksoft.moneyday.core.entity.Version;
import com.ipeaksoft.moneyday.core.service.DeviceService;
import com.ipeaksoft.moneyday.core.service.FastActiveService;
import com.ipeaksoft.moneyday.core.service.FastClickService;
import com.ipeaksoft.moneyday.core.service.TaskFastService;
import com.ipeaksoft.moneyday.core.service.UserFastService;
import com.ipeaksoft.moneyday.core.service.UserRecordService;
import com.ipeaksoft.moneyday.core.util.Constant;
import com.ipeaksoft.moneyday.core.util.FastActive;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;
import com.ipeaksoft.moneyday.weixin.vo.VersionVo;

@Controller
public class DeviceController extends BaseController {
	@Autowired
	DeviceService deviceService;
	@Autowired
	TaskFastService taskFastService;
	@Autowired
	FastClickService fastClickService;
	@Autowired
	FastActiveService fastActiveService;
	@Autowired
	UserFastService userFastService;
	@Autowired
	UserRecordService userRecordService;
	@Autowired
	CookieUtils cookieUtils;
	/**
	 * 检测小助手更新接口 test
	 * example:http://mp.i43.com/checkversion?appVersion=1.0.0&system=ios
	 * 
	 * @param VersionVo
	 * @return
	 */
	@ResponseBody
	@RequestMapping("checkVersion")
	public Object checkversion(VersionVo vo) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			Device device = deviceService.selectByIdfa(cookieUtils.getIDFA(request));
			Version v = deviceService.getDeviceByVersion(toVersion(vo));
			if (v == null) {
				map.put("code", "1000");
				map.put("message", "没有该软件版本！");
				return JSONObject.toJSON(map);
			}
			if (toInt(vo.getAppVersion()) > toInt(v.getVersion())) {
				map.put("code", "1013");
				map.put("message", "当前版本不需更新");
				return JSONObject.toJSON(map);
			}
			if (device == null) {
				device = new Device();
				device.setClientType(Constant.CLIENTTYPE_WECHAT.toString());
				String idfa = cookieUtils.getIDFA(request);
				device.setIdfa(idfa);
				device.setOs(vo.getSystem());
				device.setAppVersion(vo.getAppVersion());
				device.setModifyTime(new Date());
				deviceService.addDevice(device);
				Map<String, Object> data = new HashMap<String, Object>();
				if (v.getVersion() == null) {
					data.put("appVersion", "");
				} else {
					data.put("appVersion", v.getVersion());
				}
				if (v.getUrl() == null) {
					data.put("url", "");
				} else {
					data.put("url", v.getUrl());
				}
				if (v.getVersionDesc() == null) {
					data.put("versionDesc", "");
				} else {
					data.put("versionDesc", v.getVersionDesc());
				}
				map.put("code", "1000");
				map.put("message", "成功");
				map.put("data", data);
				return JSONObject.toJSON(map);
			}
			if (vo.getAppVersion().equals(v.getVersion())) {
				map.put("code", "1013");
				map.put("message", "已经更新至最新版本");
				map.put("data", "");
				return JSONObject.toJSON(map);
			} else {
				Map<String, Object> data = new HashMap<String, Object>();
				if (v.getVersion() == null) {
					data.put("appVersion", "");
				} else {
					data.put("appVersion", v.getVersion());
				}
				if (v.getUrl() == null) {
					data.put("url", "");
				} else {
					data.put("url", v.getUrl());
				}
				if (v.getVersionDesc() == null) {
					data.put("versionDesc", "");
				} else {
					data.put("versionDesc", v.getVersionDesc());
				}
				map.put("code", "1000");
				map.put("message", "成功");
				map.put("data", data);
				logger.info("request url:{}, result:{}", request.getRequestURI(), map);
				return JSONObject.toJSON(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
			map.put("code", "1002");
			map.put("message", "failue");
			logger.info("request url:{}, result:{}", request.getRequestURI(), map);
			return JSONObject.toJSON(map);
		}
	}

	/**
	 * 小助手激活 test
	 * example:http://mp.i43.com/helperActive?idfa=asdf&mobile=1237&macAddress
	 * =fe80:604f:1768:a321:f48e&clientIP=10.8.10.232&OSVersion=OS
	 * 7&language=zh_cn&SSID=test&CarrierName=unioncom&jailBreak=0
	 * 
	 * @param helperActive
	 * @return
	 */
	@ResponseBody
	@RequestMapping("helperActive")
	public Object active(FastActive fastActive) {
		logger.info("helperActive: {}", fastActive);
		JSONObject result = new JSONObject();
		String code = "1002";
		String message = "";
		try {
			UserTaskFastClick click = null;
			UserTaskFastActive active = null;
			String mobile = fastActive.getMobile();
			String appid = fastActive.getAppID();
			click = fastClickService.findByAppid(mobile, appid, Constant.CLIENTTYPE_WECHAT);
			active = fastActiveService.findByAppid(mobile, appid, Constant.CLIENTTYPE_WECHAT);
			if (null != click) {
				if (null != active && "1".equals(active.getStatus())) {
					message = "新手任务已完成";
				} else {
					// 记录快速任务激活记录
					UserTaskFastActive newActive = toActiveObject(fastActive, click);
					newActive = fastActiveService.create(newActive);
					if ("1".equals(newActive.getStatus())) {
						UserTaskFast fast = toUserTaskFast(click, newActive);
						fast = userFastService.create(fast);
						// 记录用户积分流水
						userRecordService.create(fast);
						code = "1000";
						message = "success";
					}
				}
			} else {
				message = "未开始任务";
			}
		} catch (Exception e) {
			logger.error("ERROR:", e);
			code = "1001";
			message = "未知异常";
		}
		result.put("code", code);
		result.put("message", message);
		logger.info("request url:{}, result:{}", request.getRequestURI(), result);
		return result;
	}

	private Version toVersion(VersionVo vo) {
		Version version = new Version();
		version.setOs(vo.getSystem());
		version.setClientType(Constant.CLIENTTYPE_WECHAT);
		return version;
	}

	private UserTaskFast toUserTaskFast(UserTaskFastClick click, UserTaskFastActive active) {
		UserTaskFast record = new UserTaskFast();
		record.setMobile(click.getMobile());
		record.setTaskId(null == click.getTaskId() ? 0 : click.getTaskId());
		record.setAppid(click.getAppid());
		record.setAward(Integer.parseInt(click.getPoint()));
		record.setCreateTime(new Date());
		record.setDescription("快速任务 " + click.getAppname());
		record.setClickId(click.getId());
		record.setActiveId(active.getId());
		return record;
	}

	private UserTaskFastActive toActiveObject(FastActive active, UserTaskFastClick click) {
		UserTaskFastActive record = new UserTaskFastActive();
		record.setIdfa(active.getIdfa());
		long taskId = 0;
		try {
			taskId = Long.parseLong(active.getTaskId());
		} catch (NumberFormatException e) {
		}
		record.setTaskId(taskId);
		record.setIp(active.getClientIP());
		record.setCreateTime(new Date());
		record.setStatus("0");
		record.setClientType(Constant.CLIENTTYPE_WECHAT);
		if (click != null) {
			record.setMobile(click.getMobile());
			record.setAppid(click.getAppid());
			record.setStatus("1");
		}
		return record;
	}


    private int toInt(String appVersion) {
        String[] versionFlags = appVersion.split("\\.");
        int versionNo = 0;
        for (int i = 0; i < versionFlags.length; i++) {
            int flag = Integer.valueOf(versionFlags[i]);
            int temp = (int)Math.pow(1000, versionFlags.length-i-1);
            versionNo += flag*temp;
        }
        return versionNo;
    }
}
