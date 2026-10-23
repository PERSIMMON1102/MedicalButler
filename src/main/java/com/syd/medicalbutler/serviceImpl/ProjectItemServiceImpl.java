package com.syd.medicalbutler.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syd.medicalbutler.pojo.ProjectItem;
import com.syd.medicalbutler.service.ProjectItemService;
import com.syd.medicalbutler.mapper.ProjectItemMapper;
import org.springframework.stereotype.Service;

/**
* @author 田学仕
* @description 针对表【project_item】的数据库操作Service实现
* @createDate 2025-09-03 13:53:51
*/
@Service
public class ProjectItemServiceImpl extends ServiceImpl<ProjectItemMapper, ProjectItem>
    implements ProjectItemService{

}




