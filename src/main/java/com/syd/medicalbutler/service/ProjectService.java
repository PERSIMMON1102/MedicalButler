package com.syd.medicalbutler.service;

import com.syd.medicalbutler.pojo.Project;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syd.medicalbutler.pojo.ProjectDetailVO;
import com.syd.medicalbutler.pojo.ProjectItemsResult;


import java.util.List;

/**
* @author 田学仕
* @description 针对表【project】的数据库操作Service
* @createDate 2025-09-03 13:53:47
*/
public interface ProjectService extends IService<Project> {

    /**
     * 根据ID获取项目详情（包含关联的项目项）
     */
    ProjectDetailVO getProjectDetailById(Integer id);

    /**
     * 获取所有项目详情列表
     */
    List<ProjectDetailVO> getProjectDetailList();

    /**
     * 创建项目（包含检查项关联）
     */
    boolean createProject(ProjectDetailVO projectDetail);

    /**
     * 更新项目（包含检查项关联）
     */
    boolean updateProject(ProjectDetailVO projectDetail);

    /**
     * 删除项目
     */
    boolean deleteProject(Integer id);

    /**
     * 批量删除项目
     */
    boolean batchDeleteProject(List<Integer> ids);

    /**
     * 批量更新项目状态
     */
    boolean batchUpdateStatus(List<Integer> ids, Integer status);

    /**
     * 更新项目状态
     */
    boolean updateStatus(Integer id, Integer status);

    ProjectItemsResult getProjectItemsByProjectId(Integer projectId);

    List<ProjectDetailVO> getProjectDetailList(String name, Integer status, int page, int size);
    int countProjects(String name, Integer status);


}