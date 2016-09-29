$(document).ready(function() {
	var link = 'http://mp.weixin.qq.com/s?__biz=MzA5NzE5NjQ5NA==&mid=203723998&idx=1&sn=b84981c5e36d0efe7a1bfb509116da69#rd';
	var title = "i43网赚 - 点点手指,天天赚钱!";
	var imgUrl = 'http://mp.i43.com/img/i43share.png';
	var desc = "果粉福利到！i43网赚微信平台正式上线了！下载试玩应用就可以赚钱，每月几百块的零花钱，快抢啊！！！大家一起来！";
	var url = window.location.href;
	var index = url.indexOf('code', 0)+5;
	var code = url.substring(index, url.length);
	var timestamp = new Date().getTime().toString().substring(0, 10);
	$.post('share', {
		code : code,
		url : url,
		timestamp : timestamp
	}, function(data) {
		var signature = data.signature;
		wx.config({
			debug : false, // 开启调试模式,调用的所有api的返回值会在客户端alert出来，若要查看传入的参数，可以在pc端打开，参数信息会通过log打出，仅在pc端时才会打印。
			appId : 'wx496a0a7c766067c2', // 必填，公众号的唯一标识
			timestamp : timestamp, // 必填，生成签名的时间戳
			nonceStr : 'zhangtong', // 必填，生成签名的随机串
			signature : signature,// 必填，签名，见API文档附录1
			jsApiList : [ 'onMenuShareTimeline', 'onMenuShareAppMessage', 'showMenuItems', 'hideMenuItems' ]
		// 必填，需要使用的JS接口列表，所有JS接口列表见API文档附录2
		});
	}, 'json');

	// config信息验证后会执行ready方法，所有接口调用都必须在config接口获得结果之后，config是一个客户端的异步操作，所以如果需要在页面加载时就调用相关接口，则须把相关接口放在ready函数中调用来确保正确执行。对于用户触发时才调用的接口，则可以直接调用，不需要放在ready函数中。
	wx.ready(function() {
		wx.showMenuItems({
			menuList : [ 'menuItem:profile' ]
		// 要显示的菜单项，所有menu项见API文档附录3
		});
		wx.hideMenuItems({
			menuList : [ 'menuItem:readMode', 'menuItem:favorite', 'menuItem:copyUrl', 'menuItem:openWithQQBrowser', 'menuItem:openWithSafari' ]
		// 要隐藏的菜单项，只能隐藏“传播类”和“保护类”按钮，所有menu项见API文档附录3
		});
		wx.onMenuShareTimeline({
			title : title, // 分享标题
			link : link, // 分享链接
			imgUrl : imgUrl, // 分享图标
			success : function() {
				// 用户确认分享后执行的回调函数
			},
			cancel : function() {
				// 用户取消分享后执行的回调函数
			}
		});
		wx.onMenuShareAppMessage({
			title : title, // 分享标题
			desc : desc, // 分享描述
			link : link, // 分享链接
			imgUrl : imgUrl, // 分享图标
			type : 'link', // 分享类型,music、video或link，不填默认为link
			dataUrl : '', // 如果type是music或video，则要提供数据链接，默认为空
			success : function() {
				// 用户确认分享后执行的回调函数
			},
			cancel : function() {
				// 用户取消分享后执行的回调函数
			}
		});
	});
	// config信息验证失败会执行error函数，如签名过期导致验证失败，具体错误信息可以打开config的debug模式查看，也可以在返回的res参数中查看，对于SPA可以在这里更新签名。
	wx.error(function(res) {
		// alert(res);
	});
});
