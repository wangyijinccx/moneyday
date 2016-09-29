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

select.form-control {
	width: auto;
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
					<li><a href="#" class="active">创建账号</a></li>
				</ol>
			</div>
		</div>
		<div class="row padding-15-tb">
			<div class="col-xs-12">
				<form id="account_form" class="form-horizontal" role="form"
					action="${pageContext.request.contextPath}/account/create">
					<div class="form-group">
						<label for="username" class="col-sm-2 control-label">用户名</label>
						<div class="col-sm-4">
							<input type="text" class="form-control" id="username"
								name="username" placeholder="用户名">
						</div>
					</div>
					<div class="form-group">
						<label for="password" class="col-sm-2 control-label">密码</label>
						<div class="col-sm-4">
							<input type="password" class="form-control" id="password"
								name="password" placeholder="密码">
						</div>
					</div>
					<div class="form-group">
						<label for="confirmpassword" class="col-sm-2 control-label">密码确认</label>
						<div class="col-sm-4">
							<input type="password" class="form-control" id="confirmpassword"
								name="confirmpassword" placeholder="密码确认">
						</div>
					</div>
					<div class="form-group">
						<div class="col-sm-offset-2 col-sm-6 text-center">
							<button type="submit" class="btn btn-primary">创建</button>
							<button type="reset" class="btn btn-default">重置</button>
							<a href="list" type="reset" class="btn btn-default">返回</a>
						</div>
					</div>
				</form>
			</div>
		</div>
	</div>
	</main>
	<script type="text/javascript">
	$(function(){
		$("#account_form").bootstrapValidator({
			framework: 'bootstrap',
			feedbackIcons : {
				valid : 'glyphicon glyphicon-ok',
				invalid : 'glyphicon glyphicon-remove',
				validating : 'glyphicon glyphicon-refresh'
			},
			fields : {
				username:{
					validators : {
						notEmpty : {
							message : '用户名不能为空'
						}
					}
				},
				password:{
					validators:{
						notEmpty:{
							message:'密码不能为空'
						},
						  identical: {
		                        field: 'confirmpassword',
		                        message: '两次输入密码不一致'
		                    }
					}
				},
				confirmpassword:{
					validators:{
						notEmpty:{
							message:'密码确认不能为空'
						},
						  identical: {
		                        field: 'password',
		                        message: '两次输入密码不一致'
		                    }
					}
				}
			}
		}).on('success.form.bv', function(e) {
	        e.preventDefault();
	        var $form = $(e.target);
	        var bv = $form.data('formValidation');
	        $.ajax({
	            type: "POST",
	            url:$form.attr('action'),
	            data:$form.serialize(),
	            error: function(request) {
	                console.info(request);
	                alert("Connection error");
	            },
	            success: function(data) {
	            	
	            	   bootbox.alert({
	           	        buttons: {
	           	            list: {
	           	                label: '返回首页',
	           	                className: 'btn-myStyle'
	           	            },
	           			   ok: {
	           				    label: '继续添加',
	           				    className: 'btn-myStyle'
	           			   }
	           		    },
	           		    message: JSON.parse(data).msg,
	           		    callback: function (e) {
	           		        if (e == "list") {
	           		            window.location="../search/search_performance"
	           		        }
	           		    },
	           		    title: "提示",
	           	    });
	            }
	        });
		});
	});
	
	</script>
</body>
</html>
