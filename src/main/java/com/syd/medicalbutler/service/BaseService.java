package com.syd.medicalbutler.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syd.medicalbutler.dto.ResultInfo;
import com.syd.medicalbutler.dto.SearchInfo;

import java.util.List;

public interface BaseService<T> {

    public ResultInfo select(SearchInfo sea, QueryWrapper<T> ex);
    public void insert(T u);
    public void update(T u);
    public void delete(int id);
    public T getById(int id);
    public List<T> SelectList(Wrapper<T> q);
}
