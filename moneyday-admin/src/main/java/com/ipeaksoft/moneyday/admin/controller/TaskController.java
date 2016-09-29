package com.ipeaksoft.moneyday.admin.controller;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.ipeaksoft.moneyday.admin.util.HttpUtil;
import com.ipeaksoft.moneyday.admin.util.JsonTransfer;
import com.ipeaksoft.moneyday.core.dto.Dictionary;
import com.ipeaksoft.moneyday.core.entity.AdminUser;
import com.ipeaksoft.moneyday.core.entity.TaskAds;
import com.ipeaksoft.moneyday.core.entity.TaskFast;
import com.ipeaksoft.moneyday.core.service.TaskAdsService;
import com.ipeaksoft.moneyday.core.service.TaskFastService;

@Controller
@RequestMapping(value = "/task")
public class TaskController extends BaseController {
	@Autowired
	private TaskFastService taskFastService;
	@Autowired
	private TaskAdsService taskAdsService;

	@RequestMapping(value = "/published")
	public String Published_Task(ModelMap map, Principal principal, HttpServletRequest request) {
		return "/task/task_published";
	}

	@RequestMapping(value = "/published/create")
	public ModelAndView Published_Create(ModelMap map, Principal principal, HttpServletRequest request) {
	    // 加载任务来源列表和渠道商名称列表
	    ModelAndView mv = new ModelAndView();
        List<Dictionary> taskSourceList = taskFastService.listTaskSources();
        List<Dictionary> channelNameList = taskFastService.listChannelNames();
        mv.getModelMap().put("taskSourceList", taskSourceList);
        mv.getModelMap().put("channelNameList", channelNameList);
        mv.setViewName("/task/task_create");
		return mv;
	}

	@RequestMapping(value = "/published/update")
	public ModelAndView Published_Update(TaskFast task, HttpServletRequest request) {
		// 加载要修改的快速任务
		ModelAndView mv = new ModelAndView();
		mv.addObject("id", Long.parseLong(request.getParameter("id")));
		List<Dictionary> taskSourceList = taskFastService.listTaskSources();
		List<Dictionary> channelNameList = taskFastService.listChannelNames();
		task = taskFastService.getUserById(Long.parseLong(request.getParameter("id")));
		mv.getModelMap().put("taskSourceList", taskSourceList);
		mv.getModelMap().put("channelNameList", channelNameList);
		mv.getModelMap().put("task", task);
		mv.setViewName("/task/task_update");
		return mv;
	}


	/**
	 * 加载快速任务
	 * 
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/data_load")
	public String data_load(HttpServletRequest request) throws Exception {
		try {
			int start = Integer.parseInt(request.getParameter("start"));// 开始记录数
			int pageSize = Integer.parseInt(request.getParameter("length"));// 每页记录数
			String sEcho = request.getParameter("draw");// 搜索内容

			String from = request.getParameter("start_date");
			String end = request.getParameter("end_date");
			String searchContent = request.getParameter("search[value]");
			String orderColumnIndex = request.getParameter("order[0][column]");
			String orderDir = request.getParameter("order[0][dir]");
			String ColumnName = null;
			if (orderColumnIndex != null && !orderColumnIndex.equals("") && orderDir != null
					&& !orderDir.equals("")) {
				switch (orderColumnIndex) {
				case "0":
					ColumnName = " id " + orderDir + ",";
					break;
				case "1":
					ColumnName = " create_time " + orderDir + ",";
					break;
				case "2":
					ColumnName = " end_time " + orderDir + ",";
					break;
				case "3":
					ColumnName = " taskname " + orderDir + ",";
					break;
				case "4":
					ColumnName = " award " + orderDir + ",";
					break;
				case "5":
					ColumnName = " start_time " + orderDir + ", end_time " + orderDir + ",";
					break;
				case "6":
					ColumnName = " task_type " + orderDir + ",";
					break;
				case "7":
					ColumnName = " priority " + orderDir + ",";
					break;
				case "8":
					ColumnName = " description " + orderDir + ",";
					break;
				case "9":
					ColumnName = " statusName " + orderDir + ",";
					break;
				}
			}

			TaskFast tf = new TaskFast();
			tf.setSearchFrom(from.trim().equals("") ? null : from.trim());
			tf.setSearchEnd(end.trim().equals("") ? null : end.trim());
			tf.setCurrentPage(start);// 当前第几页
			tf.setPageSize(pageSize);// 当页共有多少条

			tf.setOrderStr(ColumnName);// 排序
			tf.setTaskname(searchContent);
			java.util.List<TaskFast> list = taskFastService.findByWhere(tf);
			int total = taskFastService.findCountByWhere(tf);

			String result = JsonTransfer.getJsonFromList(sEcho, list);
			result = "{\"draw\":" + sEcho + ",\"recordsTotal\":" + pageSize
					+ ",\"recordsFiltered\":" + total + ",\"data\":" + result + "}";
			return result;
		} catch (Exception ex) {
			throw ex;
		}

	}

	/**
	 * 创建快速任务
	 * 
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/add")
	public String CreateTask(TaskFast task, HttpServletRequest request) {
		String result = "{\"status\":true,\"msg\":\"添加成功\"}";
		try {
			task.setFinished(0);
			task.setCreateTime(new Date());
			task.setLimit(Integer.parseInt(request.getParameter("limittime")));

			if (taskFastService.addTaskFast(task) < 1) {
				result = "{\"status\":true,\"msg\":\"添加失败\"}";
			}

		} catch (Exception e) {
			e.printStackTrace();
			result = "{\"status\":true,\"msg\":\"添加失败\"}";
		}
		return result;
	}

	/**
	 * 更新快速任务
	 * 
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/update")
	public String UpdateTask(TaskFast task, HttpServletRequest request) {
		String result = "{\"status\":true,\"msg\":\"更新成功\"}";
		try {
			// 进行更新操作
			TaskFast model = taskFastService.getUserById(task.getId());
			if (model == null) {
				result = "{\"status\":true,\"msg\":\"不存在该对象\"}";
			} else {
				model.setTaskname(task.getTaskname());
				model.setDescription(task.getDescription());
				model.setImg(task.getImg());
				model.setPriority(task.getPriority());
				model.setNumorder(task.getNumorder());
				model.setDownloadUrl(task.getDownloadUrl());
				model.setAward(task.getAward());
				model.setStartTime(task.getStartTime());
				model.setEndTime(task.getEndTime());
				model.setTotal(task.getTotal());
				model.setAppDescription(task.getAppDescription());
				model.setShowStartTime(task.getShowStartTime());
				model.setShowEndTime(task.getShowEndTime());
				model.setShowNumorder(task.getShowNumorder());
				model.setTaskType(task.getTaskType());
				model.setSearchOrderNum(task.getSearchOrderNum());
				model.setKeyWord(task.getKeyWord());
				model.setUrlscheme(task.getUrlscheme());
				model.setSize(task.getSize());
				model.setCompareType(task.getCompareType());
				model.setLimit(Integer.parseInt(request.getParameter("limittime")));
				model.setTaskSource(task.getTaskSource());
				model.setOperateStep(task.getOperateStep());
				model.setAdId(task.getAdId());
				model.setChannelName(task.getChannelName());
				model.setAppid(task.getAppid());
				model.setSdkLink(task.getSdkLink());
				model.setActiveUpload(task.isActiveUpload());
				model.setAdId2(task.getAdId2());
				model.setDuplicate(task.isDuplicate());
				

				if (taskFastService.updateByPrimaryKey(model) < 1) {
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
	 * 测试广告主响应速度
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "testSpeed")
	public long testSpeed(HttpServletRequest request) {
		String id = request.getParameter("id");
		String idfa = "FCDEFD53-C8D2-4909-8443-2C6B302C0C35";
		String url = "";
		long time = HttpUtil.get(url);
		return time;
	}
}
