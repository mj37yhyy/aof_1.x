function importConig(type){
	$("#importConfigInfo").modal("show");
	$("#importConfigInfo").find("#startImportConfig").unbind();
	var url="";
	if(type=="base"){
		url="../manager/import_dss_config";
	}else if(type=="biz"){
		url="../manager/import_biz_config";
	}
	$("#importConfigInfo").find("#startImportConfig").bind("click",function(){
		$("#importConfigInfo").find("#startImportConfig").attr("disabled",true);
		var submitJson=$("#importContent").val().replace(/(\r\n|\n|\r)/gm, '');
		if(submitJson==""){
			alert("输入不能为空");
			$("#importConfigInfo").find("#startImportConfig").removeAttr("disabled");
			return;
		}
		$.ajax({
			   type: "POST",
			   url: url,
			   dataType:"json",
			   data:{imports:submitJson},
			   success: function(msg){
				   if(msg.code=="0"){
					   alert("导入成功")
					   $("#importConfigInfo").find("#stopImportConfig").trigger("click");
					   $("#importConfigInfo").find("#startImportConfig").removeAttr("disabled");
				   }else{
					   alert(msg.msg)
					   $("#importConfigInfo").find("#startImportConfig").removeAttr("disabled");
				   }
			   }
		});
	});
	
	
}