package com.syd.medicalbutler.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.syd.medicalbutler.dto.ResultInfo;
import com.syd.medicalbutler.dto.SearchInfo;
import com.syd.medicalbutler.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;


public class BaseServiceImpl<T> {
    @Autowired
    BaseMapper<T> mymapper;

    User current;

    private  Class<T> getTClass(){
        Class<?> subclass = this.getClass();
        Type genericSuperclass = subclass.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericSuperclass;
            return (Class<T>) pt.getActualTypeArguments()[0];
        }
        return null;
    }

    public ResultInfo select(SearchInfo sea, QueryWrapper<T> ex) {
        ResultInfo info=new ResultInfo();
        QueryWrapper<T> q=sea.getQuery(ex,getTClass());
        Class cls=getTClass();
        try {
            cls.getDeclaredField("isdel");
            q.eq("isdel",0);
        }catch (Exception e){
        }
        if(sea.getLimit()<10000){
            IPage<T> po=sea.getPageInfo(getTClass());
            po=  mymapper.selectPage(po,q);
             info.setData(po.getRecords());
             info.setCount(po.getTotal());
        }else{
            info.setData(mymapper.selectList(q));
        }
        info.setParams(sea.getSea_val());
            return info;
    }

    public List<T> SelectList(Wrapper<T> q){
        return mymapper.selectList(q);
    }
    public void insert(T u) {
        mymapper.insert(u);
    }

    public void update(T u) {
        mymapper.updateById(u);
    }

    public void delete(int id) {
        Class cls=getTClass();
        try {
            cls.getDeclaredField("isdel");
            mymapper.update(null,new UpdateWrapper<T>().eq("id",id).set("isdel","1"));
        }catch (Exception e){
            mymapper.deleteById(id);
        }

    }

    public T getById(int id) {
        return mymapper.selectById(id);
    }
}
