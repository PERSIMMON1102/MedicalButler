package com.syd.medicalbutler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syd.medicalbutler.dto.ResultInfo;
import com.syd.medicalbutler.pojo.ProjectItem;
import com.syd.medicalbutler.service.ProjectItemService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectItemController {

    private final ProjectItemService itemService;



    @Value("${avatar.baseDir:D:/MedicalButler/uploads/avatars}")
    private String avatarBaseDir;

    @Value("${avatar.max-size:2097152}")
    private long avatarMaxSize;

    // 列表（分页 + 名称模糊搜索）
    @GetMapping("/items")
    public ResultInfo list(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size,
            @RequestParam(value = "name", required = false) String name
    ) {
        ResultInfo r = new ResultInfo();
        try {
            Page<ProjectItem> pg = new Page<>(page, size);
            LambdaQueryWrapper<ProjectItem> qw = new LambdaQueryWrapper<>();
            qw.eq(ProjectItem::getIsdel, 0);
            if (StringUtils.hasText(name)) {
                qw.like(ProjectItem::getName, name);
            }
            // 按 id 升序
            qw.orderByAsc(ProjectItem::getId);

            IPage<ProjectItem> result = itemService.page(pg, qw);
            r.setCode(0);
            r.setMsg("ok");
            r.setCount(result.getTotal());
            r.setData(result.getRecords());
        } catch (Exception ex) {
            r.setCode(1);
            r.setMsg("查询失败: " + ex.getMessage());
            r.setCount(0);
            r.setData(Collections.emptyList());
        }
        return r;
    }

    // 获取单条
    @GetMapping("/items/{id}")
    public ResultInfo getById(@PathVariable("id") Integer id) {
        ResultInfo r = new ResultInfo();
        try {
            ProjectItem item = itemService.getById(id);
            if (item == null || (item.getIsdel() != null && item.getIsdel() == 1)) {
                r.setCode(1);
                r.setMsg("未找到记录");
                r.setData(Collections.emptyList());
                r.setCount(0);
            } else {
                r.setCode(0);
                r.setMsg("ok");
                // 前端期望res.data 为对象，这里我们把 data 放成单对象（注意你的 ResultInfo.data 是 List）
                r.setData(Collections.singletonList(item));
                r.setCount(1);
            }
        } catch (Exception ex) {
            r.setCode(1);
            r.setMsg("查询失败: " + ex.getMessage());
            r.setData(Collections.emptyList());
            r.setCount(0);
        }
        return r;
    }

    // 新增
    @PostMapping("/items")
    public ResultInfo create(@RequestBody ProjectItem item) {
        ResultInfo r = new ResultInfo();
        try {
            item.setId(null);
            if (item.getIsdel() == null) item.setIsdel(0);
            boolean ok = itemService.save(item);
            if (ok) {
                r.setCode(0);
                r.setMsg("新增成功");
                r.setData(Collections.singletonList(item));
                r.setCount(1);
            } else {
                r.setCode(1);
                r.setMsg("新增失败");
            }
        } catch (Exception ex) {
            r.setCode(1);
            r.setMsg("新增异常: " + ex.getMessage());
        }
        return r;
    }

    // 更新
    @PutMapping("/items/{id}")
    public ResultInfo update(@PathVariable("id") Integer id, @RequestBody ProjectItem item) {
        ResultInfo r = new ResultInfo();
        try {
            ProjectItem exist = itemService.getById(id);
            if (exist == null || (exist.getIsdel() != null && exist.getIsdel() == 1)) {
                r.setCode(1);
                r.setMsg("记录不存在");
                return r;
            }
            // 设置 id，避免客户端篡改
            item.setId(id);
            boolean ok = itemService.updateById(item);
            if (ok) {
                r.setCode(0);
                r.setMsg("更新成功");
                r.setData(Collections.singletonList(itemService.getById(id)));
                r.setCount(1);
            } else {
                r.setCode(1);
                r.setMsg("更新失败");
            }
        } catch (Exception ex) {
            r.setCode(1);
            r.setMsg("更新异常: " + ex.getMessage());
        }
        return r;
    }

    // 删除（逻辑删除）
    @DeleteMapping("/items/{id}")
    public ResultInfo delete(@PathVariable("id") Integer id) {
        ResultInfo r = new ResultInfo();
        try {
            ProjectItem exist = itemService.getById(id);
            if (exist == null || (exist.getIsdel() != null && exist.getIsdel() == 1)) {
                r.setCode(1);
                r.setMsg("记录不存在");
                return r;
            }
            exist.setIsdel(1);
            boolean ok = itemService.updateById(exist);
            if (ok) {
                r.setCode(0);
                r.setMsg("删除成功");
            } else {
                r.setCode(1);
                r.setMsg("删除失败");
            }
        } catch (Exception ex) {
            r.setCode(1);
            r.setMsg("删除异常: " + ex.getMessage());
        }
        return r;
    }

    // 批量删除（逻辑删除）
    @PostMapping("/items/batch-delete")
    public ResultInfo batchDelete(@RequestBody IdsDto idsDto) {
        ResultInfo r = new ResultInfo();
        if (idsDto == null || idsDto.getIds() == null || idsDto.getIds().isEmpty()) {
            r.setCode(1);
            r.setMsg("参数错误");
            return r;
        }
        try {
            List<Integer> ids = idsDto.getIds();
            List<ProjectItem> list = new ArrayList<>();
            for (Integer id : ids) {
                ProjectItem pi = new ProjectItem();
                pi.setId(id);
                pi.setIsdel(1);
                list.add(pi);
            }
            boolean ok = itemService.updateBatchById(list);
            if (ok) {
                r.setCode(0);
                r.setMsg("删除成功");
            } else {
                r.setCode(1);
                r.setMsg("删除失败");
            }
        } catch (Exception ex) {
            r.setCode(1);
            r.setMsg("删除异常: " + ex.getMessage());
        }
        return r;
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

    @Data
    public static class IdsDto {
        private List<Integer> ids;
    }
}