/**
 * 
 */
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
						message : '用户名不能为空2'
					},
					remote:{
						url:"/account/ValidatorUserName",
						type:"POST"
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
	                        message: '两次输入密码不一致1'
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
		alert("1111");
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
           	                label: '返回列表',
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
           		            window.location="list"
           		        }
           		    },
           		    title: "提示",
           	    });
            }
        });
	});
});