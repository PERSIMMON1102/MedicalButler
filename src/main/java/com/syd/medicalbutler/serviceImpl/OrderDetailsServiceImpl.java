package com.syd.medicalbutler.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syd.medicalbutler.pojo.OrderDetails;
import com.syd.medicalbutler.service.OrderDetailsService;
import com.syd.medicalbutler.mapper.OrderDetailsMapper;
import org.springframework.stereotype.Service;

/**
* @author 田学仕
* @description 针对表【order_details】的数据库操作Service实现
* @createDate 2025-09-03 13:53:25
*/
@Service
public class OrderDetailsServiceImpl extends ServiceImpl<OrderDetailsMapper, OrderDetails>
    implements OrderDetailsService{

}




