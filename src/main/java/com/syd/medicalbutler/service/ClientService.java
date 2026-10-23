package com.syd.medicalbutler.service;

import com.syd.medicalbutler.pojo.Client;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 田学仕
* @description 针对表【client】的数据库操作Service
* @createDate 2025-09-03 13:53:00
*/
public interface ClientService extends IService<Client> {
    /**
     * 用户登录
     * @param tel 手机号
     * @param pass 密码
     * @return 用户信息
     */
    Client login(String tel, String pass);

    /**
     * 检查手机号是否已注册
     * @param tel 手机号
     * @return 是否已注册
     */
    boolean isTelRegistered(String tel);

    /**
     * 根据手机号获取用户
     * @param tel 手机号
     * @return 用户信息
     */
    Client getClientByTel(String tel);
}
