package com.syd.medicalbutler.controller;


import com.syd.medicalbutler.dto.ResultInfo;
import com.syd.medicalbutler.mapper.ProjectItemMapper;
import com.syd.medicalbutler.pojo.ProjectDetailVO;
import com.syd.medicalbutler.pojo.ProjectItem;
import com.syd.medicalbutler.pojo.ProjectItemsResult;
import com.syd.medicalbutler.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Value("${avatar.baseDir:D:/MedicalButler/uploads/avatars}")
    private String avatarBaseDir;

    @Value("${avatar.max-size:2097152}")
    private long avatarMaxSize;

    @GetMapping("/list")
    public ResultInfo getProjectList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) Integer status) {

        try {
            // 参数验证
            if (page < 1) page = 1;
            if (size < 1) size = 10;
            if (size > 100) size = 100; // 限制最大分页大小

            // 获取分页数据
            List<ProjectDetailVO> projectList = projectService.getProjectDetailList(name, status, page, size);

            // 获取总记录数
            int totalCount = projectService.countProjects(name, status);

            ResultInfo result = new ResultInfo();
            result.setCode(0);
            result.setMsg("success");
            result.setCount(totalCount);
            result.setData(projectList);

            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("获取项目列表失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> res = new HashMap<>();
        if (file == null || file.isEmpty()) {
            res.put("code", 1);
            res.put("msg", "未上传文件");
            return res;
        }


        if (file.getSize() > avatarMaxSize) {
            res.put("code", 1);
            res.put("msg", "文件不能超过 " + (avatarMaxSize / 1024) + " KB");
            return res;
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            res.put("code", 1);
            res.put("msg", "只支持图片文件");
            return res;
        }

        try {

            Path base = Paths.get(avatarBaseDir).toAbsolutePath().normalize();

            if (!base.isAbsolute()) {
                base = Paths.get(System.getProperty("user.dir")).resolve(base).toAbsolutePath().normalize();
            }

            if (!Files.exists(base)) {
                Files.createDirectories(base);
            }


            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null) {
                int idx = original.lastIndexOf('.');
                if (idx >= 0) ext = original.substring(idx).toLowerCase();
            }

            if (ext.isEmpty()) {
                if ("image/png".equals(contentType)) ext = ".png";
                else if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)) ext = ".jpg";
                else ext = ".img";
            }


            String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                    + "_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6) + ext;

            Path target = base.resolve(filename).normalize();


            if (!target.startsWith(base)) {
                res.put("code", 1);
                res.put("msg", "非法的文件路径");
                return res;
            }


            file.transferTo(target.toFile());


            String url = "/uploads/avatars/" + filename;

            res.put("code", 0);
            res.put("msg", "上传成功");
            res.put("url", url);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("path", url);
            res.put("data", data);
            return res;
        } catch (IOException ex) {
            ex.printStackTrace();
            res.put("code", 1);
            res.put("msg", "保存失败: " + ex.getMessage());
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
            res.put("code", 1);
            res.put("msg", "上传异常: " + ex.getMessage());
            return res;
        }
    }

    /**
     * 根据ID获取项目详情
     */
    @GetMapping("/{id}")
    public ResultInfo getProjectDetail(@PathVariable Integer id) {
        try {
            ProjectDetailVO projectDetail = projectService.getProjectDetailById(id);

            ResultInfo result = new ResultInfo();
            if (projectDetail != null) {
                result.setCode(0);
                result.setMsg("success");
                result.setCount(1);
                result.setData(Arrays.asList(projectDetail));
            } else {
                result.setCode(404);
                result.setMsg("项目不存在");
            }
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("获取项目详情失败: " + e.getMessage());
            return result;
        }
    }
    @GetMapping("/{id}/items")
    public ResultInfo getProjectItemsByProjectId(@PathVariable Integer id) {
        try {
            ProjectItemsResult itemsResult = projectService.getProjectItemsByProjectId(id);

            ResultInfo result = new ResultInfo();
            result.setCode(0);
            result.setMsg("success");
            Map<String, Object> data = new HashMap<>();
            data.put("selected_ids", itemsResult.getSelectedIds());
            data.put("selected", itemsResult.getSelectedItems());
            result.setData(Arrays.asList(data));
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("获取项目检查项失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 创建新项目
     */
    @PostMapping
    public ResultInfo createProject(@RequestBody ProjectDetailVO projectDetail) {
        try {
            boolean success = projectService.createProject(projectDetail);

            ResultInfo result = new ResultInfo();
            if (success) {
                result.setCode(0);
                result.setMsg("创建成功");
            } else {
                result.setCode(400);
                result.setMsg("创建失败");
            }
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("创建项目失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public ResultInfo updateProject(@PathVariable Integer id, @RequestBody ProjectDetailVO projectDetail) {
        try {
            // 确保ID一致
            projectDetail.setId(id);

            boolean success = projectService.updateProject(projectDetail);

            ResultInfo result = new ResultInfo();
            if (success) {
                result.setCode(0);
                result.setMsg("更新成功");
            } else {
                result.setCode(400);
                result.setMsg("更新失败");
            }
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("更新项目失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public ResultInfo deleteProject(@PathVariable Integer id) {
        try {
            boolean success = projectService.deleteProject(id);

            ResultInfo result = new ResultInfo();
            if (success) {
                result.setCode(0);
                result.setMsg("删除成功");
            } else {
                result.setCode(400);
                result.setMsg("删除失败");
            }
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("删除项目失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 批量删除项目
     */
    @PostMapping("/batch-delete")
    public ResultInfo batchDeleteProject(@RequestBody BatchIdsRequest request) {
        try {
            boolean success = projectService.batchDeleteProject(request.getIds());

            ResultInfo result = new ResultInfo();
            if (success) {
                result.setCode(0);
                result.setMsg("批量删除成功");
            } else {
                result.setCode(400);
                result.setMsg("批量删除失败");
            }
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("批量删除项目失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 批量更新项目状态
     */
    @PostMapping("/batch-status")
    public ResultInfo batchUpdateStatus(@RequestBody BatchStatusRequest request) {
        try {
            boolean success = projectService.batchUpdateStatus(request.getIds(), request.getStatus());

            ResultInfo result = new ResultInfo();
            if (success) {
                result.setCode(0);
                result.setMsg("批量更新状态成功");
            } else {
                result.setCode(400);
                result.setMsg("批量更新状态失败");
            }
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("批量更新状态失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 更新单个项目状态
     */
    @PostMapping("/{id}/status")
    public ResultInfo updateStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> payload) {
        try {
            Integer status = payload != null ? payload.get("status") : null;
            if (status == null) {
                ResultInfo resultNull = new ResultInfo();
                resultNull.setCode(400);
                resultNull.setMsg("更新状态失败: status 为空");
                return resultNull;
            }

            boolean success = projectService.updateStatus(id, status);

            ResultInfo result = new ResultInfo();
            if (success) {
                result.setCode(0);
                result.setMsg("更新状态成功");
            } else {
                result.setCode(400);
                result.setMsg("更新状态失败");
            }
            return result;
        } catch (Exception e) {
            ResultInfo result = new ResultInfo();
            result.setCode(500);
            result.setMsg("更新状态失败: " + e.getMessage());
            return result;
        }
    }
    /**
     * 批量ID请求体
     */
    public static class BatchIdsRequest {
        private List<Integer> ids;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }

    /**
     * 批量状态更新请求体
     */
    public static class BatchStatusRequest {
        private List<Integer> ids;
        private Integer status;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}