package com.syd.medicalbutler.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.syd.medicalbutler.dto.ResultInfo;
import com.syd.medicalbutler.mapper.UserMapper;
import com.syd.medicalbutler.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    UserMapper mapper;

    @PostMapping("login")
    public ResultInfo login(@RequestParam(value = "name", required = false) String name,
                            @RequestParam(value = "pass", required = false) String pass) {
        ResultInfo info = new ResultInfo();

        // 参数校验
        if (name == null || name.trim().isEmpty()) {
            info.setCode(1);
            info.setMsg("用户名不能为空");
            info.setData(null);
            return info;
        }
        if (pass == null || pass.trim().isEmpty()) {
            info.setCode(1);
            info.setMsg("密码不能为空");
            info.setData(null);
            return info;
        }

        // 查询用户（假设 name 唯一）
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("name", name.trim());
        User user = mapper.selectOne(qw);

        if (user == null) {
            info.setCode(1);
            info.setMsg("用户名或密码错误");
            info.setData(null);
            return info;
        }

        // =========================
        // 使用未加盐的 MD5 校验密码
        // =========================
        String dbPass = user.getPass() == null ? "" : user.getPass().trim();
        String md5Pass = md5Hex(pass); // 将明文密码转为 md5 十六进制字符串（小写）
        if (!dbPass.equalsIgnoreCase(md5Pass)) {
            info.setCode(1);
            info.setMsg("用户名或密码错误");
            info.setData(null);
            return info;
        }

        // 检查账号是否已删除或被禁用
        if (user.getIsdel() != null && user.getIsdel() == 1) {
            info.setCode(1);
            info.setMsg("账户已被删除");
            info.setData(null);
            return info;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            info.setCode(1);
            info.setMsg("账户已被禁用");
            info.setData(null);
            return info;
        }

        // 判断是否管理员（示例：power==1 表示管理员）
        boolean isAdmin = (user.getPower() != null && user.getPower() == 1);

        // 准备返回的 user 简化信息（不包含密码）
        java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getName());
        userInfo.put("power", user.getPower());
        userInfo.put("departmentId", user.getDepartmentId());

        // 构建 data 返回结构：包含 is_admin 和 user
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("is_admin", isAdmin);
        payload.put("user", userInfo);
        payload.put("status", 1);

        // 填充 ResultInfo
        info.setCode(0);
        info.setMsg("登录成功");
        info.setCount(0);

        java.util.List<Object> dataList = new java.util.ArrayList<>();
        dataList.add(payload);
        info.setData(dataList);

        return info;
    }

    // 辅助方法：计算字符串的 MD5 十六进制表示（小写）
    private String md5Hex(String input) {
        if (input == null) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // 转为十六进制字符串
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // 不太可能发生，因为 MD5 是内建算法，但如果发生则返回 null
            return null;
        }
    }


    // 查询列表，支持按 name 模糊查询
    @GetMapping("index")
    public ResultInfo index(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", required = false, defaultValue = "1") long page,
            @RequestParam(value = "limit", required = false, defaultValue = "10") long limit) {

        QueryWrapper<User> qw = new QueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            qw.like("name", name.trim());
        }

        Page<User> pager = new Page<>(page, limit);
        IPage<User> resultPage = mapper.selectPage(pager, qw);

        ResultInfo info = new ResultInfo();
        info.setCode(0);
        info.setMsg("success");
        info.setCount(resultPage.getTotal());
        info.setData(resultPage.getRecords());
        return info;
    }

    // 添加用户（仅需 name）
    @PostMapping("insert")
    public ResultInfo insert(User u){
        ResultInfo info = new ResultInfo();
        if (u == null || u.getName() == null || u.getName().trim().isEmpty()) {
            info.setCode(1);
            info.setMsg("姓名不能为空");
            return info;
        }
        int rows = mapper.insert(u);
        if (rows > 0) {
            info.setCode(0);
            info.setMsg("添加成功");

            info.setData(java.util.Arrays.asList(u));
        } else {
            info.setCode(1);
            info.setMsg("添加失败");
        }
        return info;
    }

    // 删除
    @GetMapping("delete")
    public ResultInfo delete(@RequestParam("id") int id){
        ResultInfo info = new ResultInfo();
        int rows = mapper.deleteById(id);
        if (rows > 0) {
            info.setCode(0);
            info.setMsg("删除成功");
        } else {
            info.setCode(1);
            info.setMsg("删除失败，ID不存在");
        }
        return info;
    }

    // 更新（仅更新姓名）
    @PostMapping("update")
    public ResultInfo update(@RequestParam("employeeId") Integer employeeId,
                             @RequestParam("name") String name) {
        ResultInfo info = new ResultInfo();
        if (employeeId == null) {
            info.setCode(1);
            info.setMsg("缺少 employeeId");
            return info;
        }
        if (name == null || name.trim().isEmpty()) {
            info.setCode(1);
            info.setMsg("姓名不能为空");
            return info;
        }
        User u = mapper.selectById(employeeId);
        if (u == null) {
            info.setCode(1);
            info.setMsg("未找到该用户");
            return info;
        }
        u.setName(name.trim());
        int rows = mapper.updateById(u);
        if (rows > 0) {
            info.setCode(0);
            info.setMsg("更新成功");
            info.setData(java.util.Arrays.asList(u));
        } else {
            info.setCode(1);
            info.setMsg("更新失败");
        }
        return info;
    }
}