<%@ page language="java" pageEncoding="UTF-8"
	contentType="text/html; charset=UTF-8"%>
<%@ include file="/common/taglibs.jsp"%>
<div class="navbar navbar-inverse navbar-fixed-top">
	<div class="container">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle" data-toggle="collapse"
				data-target=".navbar-collapse">
				<span class="icon-bar"></span> <span class="icon-bar"></span> <span
					class="icon-bar"></span>
			</button>
			<a class="navbar-brand">广告平台</a>
		</div>
		<div class="navbar-collapse collapse">
			<ul class="nav navbar-nav">
				<li class=""><a
					href="${pageContext.request.contextPath}/search/search_performance"><font style="font-weight:bold;color:#ffffff">业绩查询</font></a></li>
				<li class=""><a
					href="${pageContext.request.contextPath}/account/create"><font style="font-weight:bold;color:#ffffff">创建用户</font></a></li>
				<li class="dropdown"><a class="dropdown-toggle"
					data-toggle="dropdown" href="#"><font style="font-weight:bold;color:#ffffff">发布</font><span class="caret"></span></a>
					<ul class="dropdown-menu">
						<li class=""><a
							href="${pageContext.request.contextPath}/task/published">已发布任务</a></li>
						<li class=""><a
							href="${pageContext.request.contextPath}/adsource/list">广告主接口配置</a></li>
					</ul></li>
			</ul>
		</div>
	</div>
</div>
