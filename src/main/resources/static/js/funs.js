function openAlert(info){
	layer.alert(info)
}
function openConfirm(info,fn1,bts,fn2){
	if(!bts)bts= ['确认','取消']
	layer.confirm(info, {
		  btn:bts //按钮
		}, function(){
			if(fn1)fn1();
			layer.closeAll("dialog")
		}, function(){
			if(fn2)fn2();
		});
}

function openLayer(url,title,area){
	if(!area)area=['480px', '300px'];
	layer.open({
		  type: 2,
		  title: title,
		  shadeClose: false,
		  shade: 0.8,
		  area: area,
		  content: url 
		});
	
}
function closeLayer(){
	var index = parent.layer.getFrameIndex(window.name); //先得到当前iframe层的索引
	parent.layer.close(index); //再执行关闭
}

function ajax_get(url,fn){
	$.get(url,{},function(json){
		if(fn)fn(json);
	},"json");
}

function ajax_post(url,params,fn){
	$.post(url,params,function(json){
		if(fn)fn(json);
	},"json");
}

function AjaxSendFile(url,FormData,fn,types){
	if(typeof(types)=='undefined'||types==null)types=[];
	if(types.length>0){
		var File_box = $("[type=file]")[0];
	    var extend = File_box.value.substring(File_box.value.lastIndexOf(".") + 1);
	    var ts=types.join(',');
	    var c=0;
	    for(var i=0;i<types.length;i++){
	    	 if ((extend.toLowerCase() == types[i]))
	 	    {
	 	      c++;
	 	    }
	    }
	    if(c==0){
	    	  alert("只支持"+ts+"格式文件，请重新上传。");
	 	        return ;
	    }
	}
	$.ajax({
        type: "POST",           //因为是传输文件，所以必须是post
        url: url,         //对应的后台处理类的地址
        data: FormData,
        processData: false,
        contentType: false,
        success: function (json) {
            fn(json);
        }
    });
}

function getParam(name) {
var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
var r = window.location.search.substr(1).match(reg);
if (r != null) return unescape(r[2]); return null;
}



var layer,$,table,form,laydate;
function LayTable(url,params){
	var self=this;
	this.url=url;
	if(!params)params={};
	this.params=params;
	
	this.RowClick;
	this.ToolClick;
	this.loaded;
	
	this.init=function(cols,page,total){
	    var limit=10;
	     if(!total)total=false;
	    if(page===false)limit=100000;
		layui.use(["layer","jquery","table","form"],function(){
			 layer=layui.layer;
			 $=layui.jquery;
			  form=layui.form;
			 table=layui.table;
			 table.render({
				    elem: '#mytab'
				    ,where:params
				    ,toolbar: '#toptoolbar'
				    ,url: self.url //数据接口
				    ,page: page //开启分页
				    ,limit:limit
				    ,totalRow: total //开启合计行
				    ,cols: [cols]
			 		,done: function(res, curr, count){
			 			var ps=res.params;
			 			if(ps){
			 			$("[name=sea_val]").val(ps);

//			 				$(".layui-form input , .layui-form select ").each(function(){
//				 				var n=$(this).attr("name");
//				 				if(ps[n])$(this).val(ps[n]);
//				 			})
			 			}
			 			
			 			if(self.loaded)self.loaded(res);
			 		}
				  });
			 
			 table.on('tool(mytab)', function(obj){ 
					if(self.RowClick)self.RowClick(obj.event,obj.data);
				})
			 table.on('toolbar(mytab)', function(obj){
					 if(self.ToolClick)self.ToolClick(obj.event);
				 })
		});
	}
	
	
	 this.fresh=function(){
		table.reload("mytab",{where:{}})
	}
	this.selectAll=function(){
		var obj={};
		$(".layui-form input , .layui-form select ").each(function(){
			var v=$(this).val();
			var n=$(this).attr("name");
			if(n)obj[n]=v;
		})
		table.reload("mytab",{where:obj})
	}
}

function LayForm(){
	var self=this;
	this.onSubmit;
	this.loaded;
	this.init=function(){
	layui.use(["layer","jquery","form","laydate"],function(){
		 layer=layui.layer;
		 $=layui.jquery;
		 form=layui.form;
		 laydate=layui.laydate;
		 form.on('submit(save)', function(data){
			if(self.onSubmit)self.onSubmit(data.field)
			  return false; //阻止表单跳转。如果需要表单跳转，去掉这段即可。
			});

		 if(self.loaded)self.loaded();
	});
	}
	this.render=function(){
		form.render(); 
	}
	this.setval=function(json){
		form.val("myform", json);
	}
}

function data_array(lay,el,data,def){
	el.empty();
		for(var i=0;i<data.length;i++){
			var opt=$("<option>").val(i).text(data[i]);
			if(def==i) opt.attr("selected","selected");
			el.append(opt);
		}
		if(form)form.render("select");
}
function ajax_array(lay,el,url,def,exarr){
	el.empty();
	$.get(url,{},function(json){
		for(var i=0;i<json.length;i++){
		     if(exarr&&exarr.indexOf(i)>=0) continue;
			var opt=$("<option>").val(i).text(json[i]);
			if(def==i) opt.attr("selected","selected");
			el.append(opt);
		}
		lay.render("select"); 
	},"json");
}
function ajax_list(lay,el,url,def,idcol,namecol,fn){
	if(!idcol)idcol="id";
	if(!namecol)namecol="name";
	el.empty();
	var curr=0;
	$.get(url,{},function(json){
		for(var i=0;i<json.length;i++){
			var opt=$("<option>").val(json[i][idcol]).text(json[i][namecol]);
			if(def==json[i][idcol]) {
			opt.attr("selected","selected");
			    curr=json[i][idcol];
			}
			el.append(opt);
		}
		if(curr==0&&json.length>0)  curr=json[0][idcol];
		lay.render("select");
		var filter=el.attr("lay-filter");
		if(filter){
		    form.on('select('+filter+')', function(){
                              var curr=$("[name="+filter+"] option:selected").val();
                               fn(curr);
                         });
           if(fn) fn(curr)
		}


	},"json");
}
