package com.syd.medicalbutler.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syd.medicalbutler.pojo.Department;
import com.syd.medicalbutler.service.DepartmentService;
import com.syd.medicalbutler.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

/**
* @author 田学仕
* @description 针对表【department】的数据库操作Service实现
* @createDate 2025-09-03 13:53:20
*/
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
    implements DepartmentService{

}




