package com.zhangtong.core.appstore.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.alibaba.fastjson.JSONObject;
import com.zhangtong.core.appstore.web.BaseController;

@Aspect
@Configuration
public class LogAdvice {
	Logger logger = LoggerFactory.getLogger(getClass().getName());
	
	@Pointcut("execution(public * com.zhangtong.core.appstore.web.*.*(..))")
	public void log() {
	}

	@Around("log()")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
		long start = System.currentTimeMillis();
		Object target = joinPoint.getTarget();
		String requestUrl = null;
		String params = null;
		if (target instanceof BaseController){
			BaseController controller = (BaseController) target;
			requestUrl = controller.requestUrl();
			params = controller.params();
		}
		Object result = joinPoint.proceed();
		logger.info("request:{}, param:{}, result:{}", requestUrl, params, JSONObject.toJSONString(result));
		logger.info("request:{}, time:{}", requestUrl, System.currentTimeMillis() - start);
		return result;
	}


}
