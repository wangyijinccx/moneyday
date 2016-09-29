<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<base href="<%=basePath%>">

<title>邀请好友</title>
<meta http-equiv="pragma" content="no-cache">
<meta http-equiv="cache-control" content="no-cache">
<meta http-equiv="expires" content="0">
<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
<meta http-equiv="description" content="This is my page">
<script type="text/javascript" src="<%=basePath%>js/jquery-1.3.2.min.js"></script>
<script type="text/javascript" src="<%=basePath%>js/jweixin-1.0.0.js"></script>
<script type="text/javascript" src="<%=basePath%>js/wechat.js"></script>
</head>
<body>
	<input type="hidden" id="code"
		value="<%=request.getParameter("code")%>" /> 点击右上角分享到朋友圈
	<br />
	<br /> 推广介绍
	<br /> 1 .成功邀请好友直接获得好友任务收益的10%奖励。邀请越多奖励越多。
	<br /> 2.您邀请来的好友，首次进入即获得200金币（￥2.00）
	<br />
	<img id="type" style="-webkit-user-select: none; cursor: zoom-in;" width="242" height="242"></img>
</body>
</html>
