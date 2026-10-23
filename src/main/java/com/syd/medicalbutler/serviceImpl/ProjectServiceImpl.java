package com.syd.medicalbutler.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syd.medicalbutler.mapper.ProjectItemMapper;
import com.syd.medicalbutler.pojo.Project;
import com.syd.medicalbutler.pojo.ProjectDetailVO;
import com.syd.medicalbutler.pojo.ProjectItem;
import com.syd.medicalbutler.pojo.ProjectItemsResult;
import com.syd.medicalbutler.service.ProjectItemService;
import com.syd.medicalbutler.service.ProjectService;
import com.syd.medicalbutler.mapper.ProjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author 田学仕
* @description 针对表【project】的数据库操作Service实现
* @createDate 2025-09-03 13:53:47
*/
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    @Autowired
    private ProjectItemService projectItemService;
    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectItemMapper projectItemMapper;


    @Override
    public List<ProjectDetailVO> getProjectDetailList(String name, Integer status, int page, int size) {
        // 构建查询条件
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("isdel", 0);

        if (StringUtils.hasText(name)) {
            wrapper.like("name", name);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }

        // 分页查询
        Page<Project> projectPage = new Page<>(page, size);
        Page<Project> resultPage = this.page(projectPage, wrapper);
        List<Project> projects = resultPage.getRecords();

        if (projects == null || projects.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取所有项目的 itemIds
        List<String> allItemIds = projects.stream()
                .map(Project::getItemIds)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        // 一次性获取所有项目项
        Map<Integer, ProjectItem> allItemsMap = getAllProjectItemsMap(allItemIds);

        // 构建结果
        List<ProjectDetailVO> result = new ArrayList<>();
        for (Project project : projects) {
            ProjectDetailVO vo = new ProjectDetailVO();
            BeanUtils.copyProperties(project, vo);

            // 设置关联的项目项
            if (StringUtils.hasText(project.getItemIds())) {
                List<ProjectItem> projectItems = getProjectItemsFromMap(project.getItemIds(), allItemsMap);
                vo.setProjectItems(projectItems);
            } else {
                vo.setProjectItems(new ArrayList<>());
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    public int countProjects(String name, Integer status) {
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("isdel", 0);

        if (StringUtils.hasText(name)) {
            wrapper.like("name", name);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }

        return Math.toIntExact(this.count(wrapper));
    }

    // 辅助方法：一次性获取所有项目项
    private Map<Integer, ProjectItem> getAllProjectItemsMap(List<String> allItemIds) {
        if (allItemIds == null || allItemIds.isEmpty()) {
            return new HashMap<>();
        }

        // 提取所有唯一的项目项ID
        Set<Integer> allUniqueIds = new HashSet<>();
        for (String itemIds : allItemIds) {
            if (StringUtils.hasText(itemIds)) {
                for (String idStr : itemIds.split(",")) {
                    try {
                        allUniqueIds.add(Integer.parseInt(idStr.trim()));
                    } catch (NumberFormatException e) {
                        // 跳过无效ID
                    }
                }
            }
        }

        if (allUniqueIds.isEmpty()) {
            return new HashMap<>();
        }

        // 一次性查询所有项目项
        QueryWrapper<ProjectItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", allUniqueIds);
        queryWrapper.eq("isdel", 0);

        List<ProjectItem> allItems = projectItemService.list(queryWrapper);

        return allItems.stream()
                .collect(Collectors.toMap(ProjectItem::getId, item -> item));
    }

    // 辅助方法：从映射中获取特定项目的项目项
    private List<ProjectItem> getProjectItemsFromMap(String itemIds, Map<Integer, ProjectItem> allItemsMap) {
        if (!StringUtils.hasText(itemIds) || allItemsMap.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProjectItem> result = new ArrayList<>();
        for (String idStr : itemIds.split(",")) {
            try {
                Integer id = Integer.parseInt(idStr.trim());
                ProjectItem item = allItemsMap.get(id);
                if (item != null) {
                    result.add(item);
                }
            } catch (NumberFormatException e) {
                // 跳过无效ID
            }
        }

        return result;
    }

    @Override
    public ProjectItemsResult getProjectItemsByProjectId(Integer projectId) {
        ProjectItemsResult result = new ProjectItemsResult();

        // 1. 查询 project 表获取 items_id 字段
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            result.setSelectedIds(Collections.emptyList());
            result.setSelectedItems(Collections.emptyList());
            return result;
        }

        String itemsIdStr = project.getItemIds();
        if (itemsIdStr == null || itemsIdStr.trim().isEmpty()) {
            result.setSelectedIds(Collections.emptyList());
            result.setSelectedItems(Collections.emptyList());
            return result;
        }

        // 2. 解析 items_id 字符串为整数 id 列表，过滤非数字，保留顺序并去重
        String[] parts = itemsIdStr.split(",");
        List<Integer> ids = new ArrayList<>();
        List<String> selectedIdsAsString = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.length() == 0) continue;
            try {
                Integer parsed = Integer.valueOf(t);
                if (seen.add(parsed)) {
                    ids.add(parsed);
                    selectedIdsAsString.add(String.valueOf(parsed));
                }
            } catch (NumberFormatException ex) {
                // 忽略非数字部分
            }
        }

        if (ids.isEmpty()) {
            result.setSelectedIds(Collections.emptyList());
            result.setSelectedItems(Collections.emptyList());
            return result;
        }

        // 3. 使用 mybatis-plus 提供的 selectBatchIds 批量查询 projectitem 表
        List<ProjectItem> items = projectItemMapper.selectBatchIds(ids);

        // 4. 可选择保持返回 items 的顺序与原 items_id 顺序一致
        if (items != null && !items.isEmpty()) {
            Map<Integer, ProjectItem> map = items.stream().collect(Collectors.toMap(ProjectItem::getId, item -> item));
            List<ProjectItem> ordered = new ArrayList<>();
            for (Integer id : ids) {
                ProjectItem it = map.get(id);
                if (it != null) ordered.add(it);
            }
            result.setSelectedItems(ordered);
        } else {
            result.setSelectedItems(Collections.emptyList());
        }

        result.setSelectedIds(selectedIdsAsString);
        return result;
    }
    @Override
    public ProjectDetailVO getProjectDetailById(Integer id) {
        Project project = this.getById(id);
        if (project == null) {
            return null;
        }

        ProjectDetailVO vo = new ProjectDetailVO();
        BeanUtils.copyProperties(project, vo);

        // 设置关联的项目项
        vo.setProjectItems(getProjectItemsByIds(project.getItemIds()));

        return vo;
    }

    @Override
    public List<ProjectDetailVO> getProjectDetailList() {
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("isdel", 0);
        List<Project> projects = this.list(wrapper);

        List<ProjectDetailVO> result = new ArrayList<>();
        if (projects == null || projects.isEmpty()) {
            return result;
        }

        for (Project project : projects) {
            ProjectDetailVO vo = new ProjectDetailVO();
            BeanUtils.copyProperties(project, vo);
            vo.setProjectItems(getProjectItemsByIds(project.getItemIds()));
            result.add(vo);
        }

        return result;
    }


    @Override
    @Transactional
    public boolean createProject(ProjectDetailVO projectDetail) {
        // 转换为Project实体
        Project project = new Project();
        BeanUtils.copyProperties(projectDetail, project);

        // 保存项目
        boolean success = this.save(project);

        // 更新itemIds字段（如果需要）
        if (success && projectDetail.getProjectItems() != null && !projectDetail.getProjectItems().isEmpty()) {
            StringBuilder itemIdsBuilder = new StringBuilder();
            for (ProjectItem item : projectDetail.getProjectItems()) {
                if (itemIdsBuilder.length() > 0) {
                    itemIdsBuilder.append(",");
                }
                itemIdsBuilder.append(item.getId());
            }
            project.setItemIds(itemIdsBuilder.toString());
            this.updateById(project);
        }

        return success;
    }

    @Override
    @Transactional
    public boolean updateProject(ProjectDetailVO projectDetail) {
        // 转换为Project实体
        Project project = new Project();
        BeanUtils.copyProperties(projectDetail, project);


        if (project.getItemIds() == null) {
            project.setItemIds("");
        }

        // 更新项目
        return this.updateById(project);
    }

    @Override
    public boolean deleteProject(Integer id) {
        // 软删除，设置isdel=1
        Project project = new Project();
        project.setId(id);
        project.setIsdel(1);
        return this.updateById(project);
    }

    @Override
    public boolean batchDeleteProject(List<Integer> ids) {
        // 批量软删除
        List<Project> projects = new ArrayList<>();
        for (Integer id : ids) {
            Project project = new Project();
            project.setId(id);
            project.setIsdel(1);
            projects.add(project);
        }
        return this.updateBatchById(projects);
    }

    @Override
    public boolean batchUpdateStatus(List<Integer> ids, Integer status) {
        // 批量更新状态
        List<Project> projects = new ArrayList<>();
        for (Integer id : ids) {
            Project project = new Project();
            project.setId(id);
            project.setStatus(status);
            projects.add(project);
        }
        return this.updateBatchById(projects);
    }

    @Override
    public boolean updateStatus(Integer id, Integer status) {
        // 更新单个项目状态
        Project project = new Project();
        project.setId(id);
        project.setStatus(status);
        return this.updateById(project);
    }

    /**
     * 根据ID字符串获取项目项列表
     */
    private List<ProjectItem> getProjectItemsByIds(String itemIds) {
        if (!StringUtils.hasText(itemIds)) {
            return new ArrayList<>();
        }

        // 将字符串ID转换为整数列表
        List<Integer> ids = new ArrayList<>();
        for (String idStr : itemIds.split(",")) {
            try {
                ids.add(Integer.parseInt(idStr.trim()));
            } catch (NumberFormatException e) {
                // 记录日志或跳过无效ID
            }
        }

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询项目项
        QueryWrapper<ProjectItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        queryWrapper.eq("isdel", 0); // 只查询未删除的

        return projectItemService.list(queryWrapper);
    }
}