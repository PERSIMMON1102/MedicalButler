function pics(el,count){
    if(!count)count=1;
    var self=this;
    var box=el.parent();
    var item="";
    for(var i=0;i<count;i++){
    item+='<div class="item" index="'+i+'">+</div>'
    }
    box.append($('<div class="items">'+item+'</div>'));
    if($("[type=file]").length==0){
       $("body").append($('<input type="file" style="display: none;"  />	'))
    }
    this.items=box.find(".items");
    this.picindex=0;
    $("body").on("click",".item",function(){
         self.picindex=$(this).attr("index");
        $("[type=file]").click();
    })
    $("body").on("change","[type=file]",function(){
            var formData = new FormData();
                	    formData.append("file", $("[type=file]")[0].files[0]);
                	    var u="/file_upload";
                	    AjaxSendFile(u,formData,function(data){
                	    if(data.status<0) openAlert("长传错误");
                	   else appendpic(data.msg)
                	    });
        })

    function appendpic(url){
        var item=self.items.find("[index='"+ self.picindex+"']");
        item.empty();
        item.append($(' <img src="'+url+'"><i class="layui-icon layui-icon-delete"></i>'))
            resetinput();
    }
    function resetinput(){
        var txt="";
       var items=self.items.find(".item");
       for(var i=0;i<items.length;i++){
            var img=$(items[i]).find("img");
                   if(img.length>0){
                       txt+=img.attr("src")+",";
                   }
       }

        if(txt.length>0) txt=txt.substring(0,txt.length-1);
        el.val(txt);
    }
    this.show=function(urls){
     var  us=  urls.split(",");
        for(var i=0;i<us.length;i++){
          var item=self.items.find("[index='"+ i+"']");
                item.empty();
                item.append($(' <img src="'+us[i]+'"><i class="layui-icon layui-icon-delete"></i>'))
        }
    }
}