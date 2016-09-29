package com.ipeaksoft.moneyday.admin.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ipeaksoft.moneyday.admin.util.CSVUtil;
import com.ipeaksoft.moneyday.core.entity.AdminUser;
import com.ipeaksoft.moneyday.core.entity.IdfaComp;
import com.ipeaksoft.moneyday.core.service.IdfaCompService;

@Controller
@RequestMapping(value = "/upload")
public class FileUploadController extends BaseController {

	public static final String FILE_ROOT_PATH = "/mnt/data/moneyday";

	public static final String FILE_ROOT_URL = "";

	@Autowired
	IdfaCompService idfaCompService;
	
	@ResponseBody
	@RequestMapping(value = "/uploadfile")
	public String upload(@RequestParam(value = "file", required = false) MultipartFile file,
			HttpServletRequest request, ModelMap model) {
		try {
			String randomFilename = "";
			Random rand = new Random();// 生成随机数
			int random = rand.nextInt();
			Calendar calCurrent = Calendar.getInstance();
			int intDay = calCurrent.get(Calendar.DATE);
			int intMonth = calCurrent.get(Calendar.MONTH) + 1;
			int intYear = calCurrent.get(Calendar.YEAR);
			String now = String.valueOf(intYear) + "_" + String.valueOf(intMonth) + "_"
					+ String.valueOf(intDay) + "_";
			randomFilename = now
					+ String.valueOf(random > 0 ? random : (-1) * random)
					+ file.getOriginalFilename()
							.substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();
			File targetFile = new File(FILE_ROOT_PATH, randomFilename);
			if (!targetFile.exists()) {
				targetFile.mkdirs();
			}
			// 保存
			try {
				file.transferTo(targetFile);
			} catch (Exception e) {
				throw e;
			}
			return  randomFilename;
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	@ResponseBody
	@RequestMapping(value = "/importIdfa")
	public String importIdfa(@RequestParam(value = "importIdfafile", required = false) MultipartFile file,
			@RequestParam(value = "appid", required = false) String appid) {
		try {
			AdminUser userInfo = getUser();
			if (userInfo == null) {
				return "{\"status\":false,\"msg\":\"当前登录用户不能为空\"}";
			}
			
			
			List<String> result=CSVUtil.importCsv(file.getInputStream());
			if (result.size() ==0 ){
				return "{\"status\":false,\"msg\":\"导入文件数据不能为空\"}";
			}
			Set<String> set = new HashSet<String>();
			set.addAll(result);
			
			List<IdfaComp> list = new ArrayList<IdfaComp>();
			set.forEach(t->{
				IdfaComp record = new IdfaComp();
				record.setIdfa(t);
				record.setAppid(appid);
				list.add(record);
			});
			
			idfaCompService.truncate();
			
			int len = list.size()%200 ==0 ?list.size()/200:list.size()/200+1;
			for (int i = 0 ; i < len ; i++){
				int fromIndex = i*200;
				int toIndex = 200*(i+1);
				if (toIndex > list.size()){
					toIndex = list.size();
				}
				idfaCompService.insertBatch(list.subList(fromIndex, toIndex));
			}
			
			return "{\"status\":true,\"msg\":\"导入成功\"}";
		} catch (Exception ex) {
			ex.printStackTrace();
			return "{\"status\":true,\"msg\":\"导入失败，原因："+ex.getMessage()+"\"}";
		}
	}

}