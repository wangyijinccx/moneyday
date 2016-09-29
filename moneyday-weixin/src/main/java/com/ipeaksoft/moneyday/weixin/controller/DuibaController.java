package com.ipeaksoft.moneyday.weixin.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ipeaksoft.moneyday.core.entity.StatCash;
import com.ipeaksoft.moneyday.core.entity.User;
import com.ipeaksoft.moneyday.core.entity.UserCash;
import com.ipeaksoft.moneyday.core.entity.UserCashApprove;
import com.ipeaksoft.moneyday.core.exception.UserNotFoundException;
import com.ipeaksoft.moneyday.core.sdk.duiba.Constant;
import com.ipeaksoft.moneyday.core.sdk.duiba.CreditConsumeParams;
import com.ipeaksoft.moneyday.core.sdk.duiba.CreditConsumeResult;
import com.ipeaksoft.moneyday.core.sdk.duiba.CreditNotifyParams;
import com.ipeaksoft.moneyday.core.sdk.duiba.CreditTool;
import com.ipeaksoft.moneyday.core.service.StatCashService;
import com.ipeaksoft.moneyday.core.service.UserCashApproveService;
import com.ipeaksoft.moneyday.core.service.UserCashService;
import com.ipeaksoft.moneyday.core.service.UserRecordService;
import com.ipeaksoft.moneyday.core.service.UserService;
import com.ipeaksoft.moneyday.core.util.DateUtil;
import com.ipeaksoft.moneyday.weixin.util.CookieUtils;
import com.mysql.jdbc.StringUtils;

@Controller
@RequestMapping(value = "/duiba")
public class DuibaController extends BaseController {

    @Autowired
    private UserService       userService;
    @Autowired
    private UserRecordService userRecordService;
    @Autowired
    private UserCashService   userCashService;
    @Autowired
    private StatCashService   statCashService;
    @Autowired
    UserCashApproveService    userCashApproveService;
    @Autowired
    CookieUtils cookieUtils;

    @ResponseBody
    @RequestMapping(value = "/billinfo")
    public String billinfo(HttpServletRequest request) {
        JSONObject jsonObject = new JSONObject();
        JSONArray array = new JSONArray();
        String code = "1001";
        int account = 0;
        String message = "";
        List<Object> data = new ArrayList<Object>();
        try {
            String mobile = cookieUtils.getMobile(request);
            Integer page = Integer.valueOf(request.getParameter("page"));
            Integer pagesize = Integer.valueOf(request.getParameter("pagesize"));
            int start = page * pagesize;
            Map<String, Object> map = new HashMap<String, Object>();
            if (null != mobile && mobile.matches("\\d+")) {
                map.put("mobile", mobile);
                //                map.put("start", start);
                //                map.put("size", pagesize);
                List<UserCash> list = userCashService.getAllByCredits(map);
                if (list.size() > 0) {
                    for (UserCash userCash : list) {
                        JSONObject record = new JSONObject();
                        String type = userCash.getExchangeType();
                        Date createTime = userCash.getCreateTime();
                        String date = DateUtil.date2Str("yyyy/MM/dd", createTime);
                        String time = DateUtil.date2Str("HH:mm", createTime);
                        String name = ("alipay".equals(type)) ? "支付宝提现" : ("phonebill".equals(type)) ? "手机话费充值" : "其他";
                        Integer amount = userCash.getAmount() / 100;
                        name = name.concat(amount.toString()).concat("元");
                        record.put("ordernum", userCash.getOrdernum());
                        record.put("name", name);
                        record.put("income", userCash.getCredits());
                        int istatus = Integer.valueOf(userCash.getStatus());
                        int status = (9 == istatus) ? 1 : (4 == istatus) ? 0 : 2;
                        if (0 == status) { // 失败的记录返回一条
                            Date operateTime = userCash.getOperateTime();
                            String operatedate = DateUtil.date2Str("yyyy/MM/dd", operateTime);
                            String operatetime = DateUtil.date2Str("HH:mm", operateTime);
                            JSONObject failRecord = new JSONObject();
                            failRecord.put("ordernum", userCash.getOrdernum());
                            failRecord.put("name", name);
                            failRecord.put("income", userCash.getCredits());
                            failRecord.put("status", status);
                            failRecord.put("date", operatedate);
                            failRecord.put("time", operatetime);
                            array.add(failRecord);
                        }
                        status = (0 == status) ? 2 : status;
                        record.put("status", status);
                        record.put("date", date);
                        record.put("time", time);
                        array.add(record); // 成功的记录返回一条
                    }
//                    logger.info(array.toString());
                } else {
                    message = "暂无兑换记录";
                }
            } else {
                message = "mobile只能为数字";
            }
            account = array.size();
            if (array.size() > start) { //分页处理
                int end = (start + pagesize > array.size()) ? array.size() : start + pagesize;
                data = array.subList(start, end);
            }
        } catch (NumberFormatException e) {
            message = "参数转换失败";
            e.printStackTrace();
        } catch (Exception e) {
            message = "出现异常";
            e.printStackTrace();
        }
        
        code = (0 == account) ? "1001" : "1000";
        jsonObject.put("code", code);
        jsonObject.put("message", message);
        jsonObject.put("account", account);
        jsonObject.put("data", data);
        logger.info("request url:{}, result:{}", request.getRequestURI(), jsonObject);
        return jsonObject.toString();
    }

    @ResponseBody
    @RequestMapping(value = "/geturl")
    public String geturl(String mobile) {
        JSONObject jsonObject = new JSONObject();
//        String mobile = cookieUtils.getMobile(request);
        int code = 1001;
        String message = "";
        String url = "";
        try {
            if (null != mobile && mobile.matches("\\d+")) {
                User user = userService.getUserByMobile(mobile);
                int credits = user.getAward();
                logger.info("[geturl][mobile: " + mobile + ", credits: " + credits + "]");
                CreditTool tool = new CreditTool(Constant.WECHATKEY, Constant.WECHATSECRET);
                //此url即为积分商城的url
                String autologinUrl = tool.buildCreditAutoLoginRequest(mobile, credits);
                if (!StringUtils.isNullOrEmpty(autologinUrl)) {
                    code = 1000;
                    message = "success";
                    url = autologinUrl;
                }
            } else {
                message = "mobile只能为数字";
            }
        } catch (NumberFormatException e) {
            logger.info("mobile格式转换错误");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        jsonObject.put("code", code);
        jsonObject.put("message", message);
        jsonObject.put("url", url);
        logger.info("request url:{}, result:{}", request.getRequestURI(), jsonObject);
        return jsonObject.toString();
    }

    @ResponseBody
    @RequestMapping(value = "/dealorder")
    public String dealorder(HttpServletRequest request) {
        CreditTool tool = new CreditTool(Constant.WECHATKEY, Constant.WECHATSECRET);
        String result = null;
        try {
            CreditConsumeParams params = tool.parseCreditConsume(request);// 利用tool来解析这个请求
            String mobile = params.getUid();// 用户手机号码
            long credits = params.getCredits(); // 获取本次兑换用户所消耗的积分数
            String type = params.getType().toLowerCase(); // 获取兑换类型
            String orderNum = params.getOrderNum(); // 获取兑吧订单号
            Integer facePrice = params.getFacePrice(); // 兑换商品的市场价值，单位是分，请自行转换单位
            String description = params.getDescription(); // 获取本次积分消耗的描述(带中文，请用utf-8进行url解码)
            //            Integer actualPrice = params.getActualPrice(); // 此次兑换实际扣除开发者账户费用，单位为分，请自行转换单位
            //            String ip = params.getIp(); // 用户ip，不一定获取到
            //            boolean iswaitAudit = params.isWaitAudit(); // 是否需要审核
            // 查询相应的用户
            User user = userService.getUserByMobile(mobile);
            int totalaward = user.getAward();
            int award = totalaward - (int) credits;
            Date now = new Date();

            // 保存当前订单信息
            String ztorder = UUID.randomUUID().toString(); // 返回掌通订单号
            UserCash userCash = new UserCash();
            userCash.setOrderid(ztorder); // 插入掌通订单号
            userCash.setOrdernum(orderNum); // 插入兑吧订单号
            userCash.setUserid(user.getId() + ""); // 插入用户名称
            userCash.setCredits((int) credits); // 插入本次订单用户消耗积分
            userCash.setTotalcredits((long) award); // 插入当前用户积分余额
            userCash.setExchangeType(type); // 插入当前订单类型
            userCash.setAmount(facePrice); // 插入当前订单的充值金额/提现金额（分为单位）
            userCash.setDescription(description); // 插入当前订单的详细描述
            userCash.setCreateTime(now); // 插入当前日期
            userCash.setStatus(Constant.APPLY); // 插入订单状态:申请状态
            userCash.setOperator(Constant.SYSTEM + ""); // 插入处理人，系统处理
            userCash.setClientType(com.ipeaksoft.moneyday.core.util.Constant.CLIENTTYPE_WECHAT); // 插入平台的类型
            // 保存当前订单的状态
            UserCashApprove userCashApprove = new UserCashApprove();
            userCashApprove.setOrderid(ztorder);
            userCashApprove.setOrdernum(orderNum);
            userCashApprove.setResult(Constant.APPLY);
            userCashApprove.setApproveTime(now);
            userCashApprove.setOperator(Constant.SYSTEM);
            userCashApprove.setDescription(Constant.APPLYINFO);

            if (!params.isWaitAudit()) { // 不需审核的订单，直接处于充值状态
                userCashApprove.setResult(Constant.DO);
                userCash.setStatus(Constant.DO);
            }
            if (type.equals("phonebill")) {
                String phone = params.getPhone();// 获取手机号码
                userCash.setMobile(phone);
            } else if (type.equals("alipay")) {
                String alipay = params.getAlipay();// 获取支付宝账号
                userCash.setAlipayAccount(alipay);
            } else if (type.equals("qb")) {
                //                    String qq = params.getQq();// 获取QQ号码
            } else if (type.equals("coupon")) {
                userCash.setStatus(Constant.OK);
                userCash.setOperateResult("coupon success");
                userCash.setOperateTime(new Date());
            }
            int insert = userCashService.insertSelective(userCash);
            int insertApprove = userCashApproveService.insertSelective(userCashApprove);

            CreditConsumeResult creditResult = null;
            if (1 == insert && 1 == insertApprove) {
                logger.info("剩余积分: " + award + ", name: " + user.getName());
                // 保存积分扣除记录到用户的积分流水表中，并扣除用户积分
                userRecordService.create(userCashService.getUserByOrder(orderNum), mobile);
                // 更新订单统计表中的待处理状态
                StatCash statCash = statCashService.getStatByDay(new Date());
                if (null != statCash) {
                    statCash.setPendingCount(statCash.getPendingCount() + 1);
                    statCashService.updateUser(statCash);
                }
                // 订单插入成功，则返回成功信息
                creditResult = new CreditConsumeResult(true);
                creditResult.setBizId(ztorder);
                creditResult.setCredits((long) award);
            } else {
                // 订单插入失败
                creditResult = new CreditConsumeResult(false);
                creditResult.setErrorMessage("订单保存失败，积分无扣除");
                creditResult.setCredits((long) award);
            }
            result = creditResult.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("request url:{}, result:{}", request.getRequestURI(), result);
        return result;
    }

    @ResponseBody
    @RequestMapping(value = "/result")
    public String result(HttpServletRequest request) {
        CreditTool tool = new CreditTool(Constant.WECHATKEY, Constant.WECHATSECRET);
        try {
            CreditNotifyParams params = tool.parseCreditNotify(request);//利用tool来解析这个请求
            String mobile = params.getUid(); // 获取用户的手机号码
            String ordernum = params.getOrderNum(); // 获取兑吧订单号
            UserCash userCash = userCashService.getUserByOrder(ordernum);
            String ztorder = userCash.getOrderid(); // 获取掌通订单号
            logger.info("ztorder： " + ztorder);
            Date now = new Date();

            // 保存当前订单的状态
            UserCashApprove userCashApprove = new UserCashApprove();
            userCashApprove.setOrderid(ztorder);
            userCashApprove.setOrdernum(ordernum);
            userCashApprove.setOperator(Constant.SYSTEM);
            userCashApprove.setApproveTime(now);

            if (params.isSuccess()) {
                logger.info("success ordernum: " + ordernum);
                // 保存当前订单的状态
                userCashApprove.setResult(Constant.OK);
                userCashApprove.setDescription(Constant.SUCCESSINFO);
                userCashApproveService.insertSelective(userCashApprove);
                // 兑换成功，更改用户的交易状态
                userCash.setStatus(Constant.OK);
                userCash.setOperateResult(Constant.SUCCESSINFO);
                userCash.setOperateTime(now);
                int flag = userCashService.updateUser(userCash);
                logger.info("修改交易状态: " + flag);
                return "ok";
            } else {
                userCashApprove.setResult(Constant.FAIL);
                userCashApprove.setDescription(params.getErrorMessage());
                userCashApproveService.insertSelective(userCashApprove);

                int credits = userCash.getCredits();
                User user = userService.getUserByMobile(mobile);
                int totalaward = user.getAward();
                logger.info("fail credits: " + credits);
                int award = totalaward + credits;
                logger.info("返还后的积分: " + award + ", name: " + user.getName());
                // 兑换失败，更改用户的交易状态
                logger.info("fail ordernum: " + ordernum);
                logger.info("fail errorMessage: " + params.getErrorMessage());
                userCash.setStatus(Constant.FAIL);
                userCash.setTotalcredits((long) award);
                userCash.setOperateResult(params.getErrorMessage());
                userCash.setOperateTime(now);
                int update = userCashService.updateUser(userCash);
                logger.info("修改交易状态: " + update);

                // 插入积分返还记录到用户的积分流水表中
                userRecordService.create(userCash, mobile);
                return "ok";
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "fail";
    }
}