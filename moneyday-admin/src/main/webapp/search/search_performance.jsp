<%@ page language="java" pageEncoding="UTF-8"
	contentType="text/html; charset=UTF-8"%>
<%@ include file="/common/taglibs.jsp"%>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<%@ include file="/common/meta.jsp"%>
<style type="text/css">
.logo_top_left {
	background-color: #428bca;
	width: auto;
	height: 46px;
	margin: 0 5px;
	border-radius: 3px;
	float: left;
	box-sizing: border-box;
	display: block;
	padding-left: 5px;
	padding-right: 5px;
}

.border-bottom {
	border-bottom: 1px solid #eee;
}

.padding-top-15 {
	padding-top: 15px;
}

.padding-15-tb {
	padding: 15px 0px 15px 0px;
}

.line-height-34 {
	line-height: 34px;
}

div.dataTables_info {
	float: left;
	padding-top: 4px;
}
</style>
</head>

<body>
	<c:set var="currentNav" value="search"></c:set>
	<%@ include file="/common/nav.jsp"%>
	<main>
		<div class="container-fluid">
			<div class="row border-bottom padding-top-15 ">
				<div class="col-xs-2 text-left">
					<ol class="breadcrumb">
						<li><a href="#" class="active">业绩查询</a></li>
					</ol>
				</div>
			</div>
			<div class="row border-bottom padding-15-tb">
				<div class="col-xs-12 text-right">			
					<div class="form-inline" role="form">
						<form id="export_from" method="POST">
							<div class="form-group">						
								<div id="date-picker" class="input-group date-picker input-daterange">
									<span class="input-group-addon">
										从
									</span>							
									<input type="text" class="form-control" id="startDate" name="start_date">
									<span class="input-group-addon to">
										到
									</span>
									<input type="text" class="form-control" id="endDate" name="end_date">
								</div>
							</div>
							<button id="btn_channel_export" type="button"
								class="btn btn-primary">渠道明细</button>
							<button id="btn_channel_click_export" type="button"
								class="btn btn-primary">渠道点击</button>
						</form>
					</div>
				</div>
			</div>
		</div>
	</main>
	<script src="../js/search_performance.js"></script>
	<script type="text/javascript">
	jQuery(document).ready(function() {       
			SearchPerformance.init();
		});
	</script>
</body>
</html>
