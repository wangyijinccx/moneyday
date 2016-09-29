/**
 * 查询_业绩查询
 */
var SearchPerformance = function() {
	"use strict";// 使用严格模式，必须放在执行体第一行才有效


	$('#btn_search2').on('click', function() {
		$('#datalist').dataTable().fnDraw();
	});

	/*
	 * 初始化时间范围选项
	 */
	var initDatePickers = function() {
		//$('#startDate').val(moment().subtract('days', 1).format('YYYY-MM-DD'));
		//$('#endDate').val(moment().subtract('days', 1).format('YYYY-MM-DD'));
		$('#startDate').val(moment().format('YYYY-MM-DD'));
		$('#endDate').val(moment().format('YYYY-MM-DD'));

		$('#date-picker').datepicker({
			format : "yyyy-mm-dd",
			language : "zh-CN",
			endDate : moment().format('YYYY-MM-DD'),
			orientation : "top left",
			autoclose : true
		});
	};

	/*
	 * 初始化表单
	 */
	var initForm = function() {
		initDatePickers();
		

		// 渠道明细导出
		$('#btn_channel_export').on('click', function(e) {
			$('#export_from').attr('action', 'export_performance_channel');
			$('#export_from').submit();
		});
		
		// 渠道点击导出
		$('#btn_channel_click_export').on('click', function(e) {
			$('#export_from').attr('action', 'export_performance_channel_click');
			$('#export_from').submit();
		});
	};

	return {
		// 初始化页面，注意加载顺序
		init : function() {
			initForm();
		}
	};

}();