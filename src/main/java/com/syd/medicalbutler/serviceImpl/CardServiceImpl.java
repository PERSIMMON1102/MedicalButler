package com.syd.medicalbutler.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syd.medicalbutler.pojo.Card;
import com.syd.medicalbutler.service.CardService;
import com.syd.medicalbutler.mapper.CardMapper;
import org.springframework.stereotype.Service;

/**
* @author 田学仕
* @description 针对表【card】的数据库操作Service实现
* @createDate 2025-09-03 13:52:33
*/
@Service
public class CardServiceImpl extends ServiceImpl<CardMapper, Card>
    implements CardService{

}




