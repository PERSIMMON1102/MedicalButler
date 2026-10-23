package com.syd.medicalbutler.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syd.medicalbutler.pojo.Client;
import com.syd.medicalbutler.service.ClientService;
import com.syd.medicalbutler.mapper.ClientMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
* @author 田学仕
* @description 针对表【client】的数据库操作Service实现
* @createDate 2025-09-03 13:53:00
*/
@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client>
    implements ClientService{

    @Autowired
    private ClientMapper clientMapper;
    /**
     * 用户登录验证
     * @param tel 手机号
     * @param pass 密码
     * @return 用户信息，登录失败返回null
     */
    @Override
    public Client login(String tel, String pass) {
        try {
            // 方法1：使用 QueryWrapper 查询用户
            QueryWrapper<Client> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("tel", tel);
            queryWrapper.eq("isdel", 0); // 只查询未删除的用户

            Client client = clientMapper.selectOne(queryWrapper);

            if (client == null) {
                return null; // 用户不存在
            }

            // 验证密码（MD5加密验证）
            String encryptedPass = DigestUtils.md5DigestAsHex(pass.getBytes()).toLowerCase();
            if (encryptedPass.equals(client.getPass())) {
                return client; // 密码正确
            }

            return null; // 密码错误

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes()).toLowerCase();
    }
    /**
     * 检查手机号是否已注册
     * @param tel 手机号
     * @return 是否已注册
     */
    @Override
    public boolean isTelRegistered(String tel) {
        QueryWrapper<Client> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tel", tel);
        return this.count(queryWrapper) > 0;
    }

    /**
     * 根据手机号获取用户
     * @param tel 手机号
     * @return 用户信息
     */
    @Override
    public Client getClientByTel(String tel) {
        QueryWrapper<Client> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tel", tel);
        return this.getOne(queryWrapper);
    }

}




