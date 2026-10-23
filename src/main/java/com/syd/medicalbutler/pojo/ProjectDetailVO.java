package com.syd.medicalbutler.pojo;


import lombok.Data;

import java.util.List;

@Data
public class ProjectDetailVO extends Project {
    // 关联的项目项列表

    private List<ProjectItem> projectItems;
}