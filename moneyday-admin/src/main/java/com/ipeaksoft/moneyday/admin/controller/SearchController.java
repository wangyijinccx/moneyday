package com.ipeaksoft.moneyday.admin.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.ipeaksoft.moneyday.admin.util.CommonUtil;
import com.ipeaksoft.moneyday.core.entity.AdminUser;
import com.ipeaksoft.moneyday.core.service.SearchService;
import com.ipeaksoft.moneyday.core.service.StatDayService;
import com.ipeaksoft.moneyday.core.service.StatDayTaskService;
import com.ipeaksoft.moneyday.core.service.TaskFastService;

@Controller
@RequestMapping(value = "/search")
public class SearchController extends BaseController {
	private static final Logger logger = LoggerFactory
			.getLogger(SearchController.class);

	@Autowired
	private SearchService searchService;

	@Autowired
	private StatDayService statDayService;

	@Autowired
	private StatDayTaskService statDayTaskService;

	@Autowired
	private TaskFastService taskFastService;
	
	@RequestMapping(value = "/search_performance")
    public String searchPerformance(ModelMap map, Principal principal, HttpServletRequest request) {
        AdminUser adminUser = this.getUser();
        map.put("adminUser", adminUser);
        return "/search/search_performance";
    }

	@RequestMapping(value = "/export_performance_channel", method = RequestMethod.POST)
	public void exportPerformanceChannel(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "start_date") String startDate,
			@RequestParam(value = "end_date") String endDate) {

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date = format.parse(endDate);
		} catch (ParseException e1) {
			date = new Date();
		}
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, 1);
		date = calendar.getTime();
		endDate = format.format(date);

		List<Map<String, Object>> data = searchService.getPerformanceChannel(
				startDate, endDate);

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("渠道明细_" + startDate + "~"
				+ endDate);

		// 列头
		XSSFRow row = sheet.createRow(0);
		row.setHeight((short) 300);
		row.createCell(0).setCellValue("激活日期");
		row.createCell(1).setCellValue("广告ID");
		row.createCell(2).setCellValue("IDFA");
		row.createCell(3).setCellValue("appid");
		row.createCell(4).setCellValue("任务名称");
		row.createCell(5).setCellValue("任务金额");
		row.createCell(6).setCellValue("任务来源");
		row.createCell(7).setCellValue("渠道名称");

		XSSFCell cell = null;
		for (int i = 0; i < data.size(); i++) {
			row = sheet.createRow(i + 1);
			row.setHeight((short) 300);
			row.createCell(0).setCellValue(
					data.get(i).get("create_time").toString());
			row.createCell(1).setCellValue(
					data.get(i).get("task_id").toString());
			row.createCell(2).setCellValue(data.get(i).get("idfa").toString());
			row.createCell(3).setCellValue(data.get(i).get("appid").toString());
			row.createCell(4).setCellValue(
					data.get(i).get("taskname").toString());
			cell = row.createCell(5);
			Double _award = Double.valueOf(data.get(i).get("award").toString()) / 100;
			String award = _award.toString().concat("元");
			cell.setCellValue(award);
			row.createCell(6).setCellValue(
					data.get(i).get("task_source").toString());
			if (data.get(i).get("channel_name") != null) {
				row.createCell(7).setCellValue(
						data.get(i).get("channel_name").toString());
			}
		}

		String filename = "渠道明细查询_" + endDate + ".xls";// 设置下载时客户端Excel的名称
		filename = CommonUtil.encodeFilename(filename, request);// 处理中文文件名
		// 表示以附件的形式把文件发送到客户端
		response.setHeader("Content-Disposition", "attachment;filename="
				+ filename);// 设定输出文件头
		response.setContentType("application/vnd.ms-excel;charset=UTF-8");// 定义输出类型

		OutputStream outputStream = null;
		try {
			outputStream = response.getOutputStream();
			workbook.write(outputStream);
			outputStream.flush();
		} catch (Exception e) {
			logger.error("导出渠道明细查询失败：" + e.getMessage());
		} finally {
			try {
				workbook.close();
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
			}
		}
	}

	@RequestMapping(value = "/export_performance_channel_click", method = RequestMethod.POST)
	public void exportPerformanceChannelClick(HttpServletRequest request,
			HttpServletResponse response,
			@RequestParam(value = "start_date") String startDate,
			@RequestParam(value = "end_date") String endDate) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date = format.parse(endDate);
		} catch (ParseException e1) {
			date = new Date();
		}
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, 1);
		date = calendar.getTime();
		endDate = format.format(date);

		SXSSFWorkbook workbook = new SXSSFWorkbook(1000);
		// XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("渠道点击_" + startDate + "~" + endDate);
		OutputStream outputStream = null;
		try {
			long total = Runtime.getRuntime().totalMemory(); // byte
			long m1 = Runtime.getRuntime().freeMemory();
			logger.debug("total: {}, free:{}, use:{}", total / 1024, m1 / 1024,
					(total - m1) / 1024);

			logger.debug("start query");
			List<Map<String, Object>> data = searchService
					.getPerformanceChannelClick(startDate, endDate);
			logger.debug("complete query");
			m1 = Runtime.getRuntime().freeMemory();
			logger.debug("total: {}, free:{}, use:{}", total / 1024, m1 / 1024,
					(total - m1) / 1024);
			// XSSFWorkbook workbook = new XSSFWorkbook();
			// XSSFSheet sheet = workbook.createSheet(admin.getUsername() +
			// "_渠道点击_" + startDate + "~" + endDate);

			// 列头
			Row row = sheet.createRow(0);
			row.setHeight((short) 300);
			row.createCell(0).setCellValue("点击日期");
			row.createCell(1).setCellValue("广告ID");
			row.createCell(2).setCellValue("IDFA");
			row.createCell(3).setCellValue("appid");
			row.createCell(4).setCellValue("任务名称");
			row.createCell(5).setCellValue("任务金额");
			row.createCell(6).setCellValue("任务来源");
			row.createCell(7).setCellValue("渠道名称");

			row.createCell(8).setCellValue("IP");
			row.createCell(9).setCellValue("国家");
			row.createCell(10).setCellValue("地区");
			row.createCell(11).setCellValue("省份");
			row.createCell(12).setCellValue("城市");
			row.createCell(13).setCellValue("区县");
			row.createCell(14).setCellValue("ISP");

			Cell cell = null;
			for (int i = 0; i < data.size(); i++) {
				row = sheet.createRow(i + 1);
				row.setHeight((short) 300);
				row.createCell(0).setCellValue(
						data.get(i).get("create_time").toString());
				row.createCell(1).setCellValue(
						data.get(i).get("task_id").toString());
				row.createCell(2).setCellValue(
						data.get(i).get("idfa").toString());
				row.createCell(3).setCellValue(
						data.get(i).get("appid").toString());
				row.createCell(4).setCellValue(
						data.get(i).get("taskname").toString());
				cell = row.createCell(5);
				Double _award = Double.valueOf(data.get(i).get("award")
						.toString()) / 100;
				String award = _award.toString().concat("元");
				cell.setCellValue(award);
				row.createCell(6).setCellValue(
						data.get(i).get("task_source").toString());
				if (data.get(i).get("channel_name") != null) {
					row.createCell(7).setCellValue(
							data.get(i).get("channel_name").toString());
				}
				row.createCell(8).setCellValue(
						data.get(i).get("clientip") != null ? data.get(i)
								.get("clientip").toString() : "");
				row.createCell(9).setCellValue(
						data.get(i).get("country") != null ? data.get(i)
								.get("country").toString() : "");
				row.createCell(10).setCellValue(
						data.get(i).get("area") != null ? data.get(i)
								.get("area").toString() : "");
				row.createCell(11).setCellValue(
						data.get(i).get("province") != null ? data.get(i)
								.get("province").toString() : "");
				row.createCell(12).setCellValue(
						data.get(i).get("city") != null ? data.get(i)
								.get("city").toString() : "");
				row.createCell(13).setCellValue(
						data.get(i).get("county") != null ? data.get(i)
								.get("county").toString() : "");
				row.createCell(14).setCellValue(
						data.get(i).get("isp") != null ? data.get(i).get("isp")
								.toString() : "");
			}
			logger.debug("complete construct excel");
			m1 = Runtime.getRuntime().freeMemory();
			logger.debug("total: {}, free:{}, use:{}", total / 1024, m1 / 1024,
					(total - m1) / 1024);

			String filename = "渠道点击查询_" + endDate + ".xlsx";// 设置下载时客户端Excel的名称
			filename = CommonUtil.encodeFilename(filename, request);// 处理中文文件名
			// 表示以附件的形式把文件发送到客户端
			response.setHeader("Content-Disposition", "attachment;filename="
					+ filename);// 设定输出文件头
			response.setContentType("application/vnd.ms-excel;charset=UTF-8");// 定义输出类型
			outputStream = response.getOutputStream();
			workbook.write(outputStream);
			outputStream.flush();
			logger.debug("complete all.");
			m1 = Runtime.getRuntime().freeMemory();
			logger.debug("total: {}, free:{}, use:{}", total / 1024, m1 / 1024,
					(total - m1) / 1024);
		} catch (Exception e) {
			logger.error("导出渠道点击查询失败：", e);
		} finally {
			try {
				workbook.close();
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
			}
		}

	}
}
