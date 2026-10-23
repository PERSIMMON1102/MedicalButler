package com.syd.medicalbutler.pojo;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class R {
    private Integer code;
    private String msg;
    private Map<String, Object> data = new HashMap<>();

    public static R ok() {
        R r = new R();
        r.setCode(200);
        r.setMsg("成功");
        return r;
    }

    public static R ok(String msg) {
        R r = new R();
        r.setCode(200);
        r.setMsg(msg);
        return r;
    }

    public static R error() {
        R r = new R();
        r.setCode(500);
        r.setMsg("失败");
        return r;
    }

    public static R error(String msg) {
        R r = new R();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }

    public R data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    // 添加一些常用状态码
    public static R notFound() {
        R r = new R();
        r.setCode(404);
        r.setMsg("资源未找到");
        return r;
    }

    public static R unauthorized() {
        R r = new R();
        r.setCode(401);
        r.setMsg("未授权");
        return r;
    }
}