package com.syd.medicalbutler.mapper;

import com.syd.medicalbutler.pojo.Client;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 田学仕
* @description 针对表【client】的数据库操作Mapper
* @createDate 2025-09-03 13:53:00
* @Entity com.syd.medicalbutler.pojo.Client
*/
@Mapper
public interface ClientMapper extends BaseMapper<Client> {

}




