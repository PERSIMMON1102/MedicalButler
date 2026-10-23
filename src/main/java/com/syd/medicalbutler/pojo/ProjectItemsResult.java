package com.syd.medicalbutler.pojo;

import java.util.List;

public class ProjectItemsResult {
    private List<String> selectedIds;
    private List<ProjectItem> selectedItems;

    public List<String> getSelectedIds() { return selectedIds; }
    public void setSelectedIds(List<String> selectedIds) { this.selectedIds = selectedIds; }

    public List<ProjectItem> getSelectedItems() { return selectedItems; }
    public void setSelectedItems(List<ProjectItem> selectedItems) { this.selectedItems = selectedItems; }

    List<ProjectItem> allItems;
}