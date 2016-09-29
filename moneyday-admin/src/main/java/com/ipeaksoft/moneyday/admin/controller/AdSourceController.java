package com.ipeaksoft.moneyday.admin.controller;

import java.security.Principal;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.ipeaksoft.moneyday.admin.util.JsonTransfer;
import com.ipeaksoft.moneyday.core.entity.AdminUser;
import com.ipeaksoft.moneyday.core.entity.AdsSource;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.service.AdsSourceService;

@Controller
@RequestMapping(value = "/adsource")
public class AdSourceController extends BaseController {
     
	@Autowired
	private AdsSourceService adsSourceService;
	
	@RequestMapping(value = "/list")
	public String list(ModelMap map, Principal principal, HttpServletRequest request) {
		return "/adsource/list";
	}
	
	@RequestMapping(value = "/create")
	public String create(ModelMap map, Principal principal, HttpServletRequest request) {
		return "/adsource/create";
	}
	
	@RequestMapping(value = "/update")
	public ModelAndView update(HttpServletRequest request) {
		String id= request.getParameter("id");
		AdsSource adsSource =  adsSourceService.selectByPrimaryKey(Integer.parseInt(id));
		ModelAndView mv = new ModelAndView();
		mv.getModelMap().put("adsSource", adsSource);
		mv.setViewName("/adsource/update");
		return mv;
	}
	
	/**
	 * 获取广告主接口信息
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/data_load")
	public String data_load(HttpServletRequest request) throws Exception {
		try {
			String sEcho = request.getParameter("draw");// 搜索内容
			List<AdsSource> list = adsSourceService.selectAll();
			int total = list.size();
			String result = JsonTransfer.getJsonFromList(sEcho, list);
			result = "{\"draw\":" + sEcho + ",\"recordsTotal\":" + total
					+ ",\"recordsFiltered\":" + total + ",\"data\":" + result + "}";
			return result;
		} catch (Exception ex) {
			throw ex;
		}

	}
	
	/**
	 * 修改广告主接口信息
	 * @param adsSource
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/updateDate")
	public String updateDate(AdsSource adsSource, HttpServletRequest request) {
		String result = "{\"status\":true,\"msg\":\"更新成功\"}";
		try {
			String id = request.getParameter("id");
			// 进行更新操作
			AdsSource model = adsSourceService.selectByPrimaryKey(Integer.parseInt(id));
			if (model == null) {
				result = "{\"status\":true,\"msg\":\"不存在该对象\"}";
			} else {
				if (adsSourceService.updateByPrimaryKeySelective(adsSource) < 1) {
					result = "{\"status\":true,\"msg\":\"更新失败\"}";
				} 
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "{\"status\":true,\"msg\":\"更新失败\"}";
		}
		return result;
		
	}
	
	/**
	 * 创建广告主接口
	 * @param adsSource
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/createDate")
	public String createDate(AdsSource adsSource, HttpServletRequest request) {
		String result = "{\"status\":true,\"msg\":\"添加成功\"}";
		try {
			
			adsSource.setCreateTime(new Date());
			if (adsSourceService.insert(adsSource) < 1) {
				result = "{\"status\":true,\"msg\":\"添加失败\"}";
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = "{\"status\":true,\"msg\":\"添加失败\"}";
		}
		return result;
	}
   
    
}
