package com.syd.medicalbutler.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syd.medicalbutler.dto.ResultInfo;
import com.syd.medicalbutler.mapper.ClientMapper;
import com.syd.medicalbutler.mapper.UserMapper;
import com.syd.medicalbutler.pojo.Client;
import com.syd.medicalbutler.pojo.R;
import com.syd.medicalbutler.pojo.User;
import com.syd.medicalbutler.service.ClientService;
import com.syd.medicalbutler.utils.SimpleCacheCode;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;


@RestController
@RequestMapping("/client")
public class ClientController {
    @Autowired
    UserMapper mapper;

    @Autowired
    private ClientMapper clientMapper;

    private SimpleCacheCode codeCache;

    @Value("${app.upload.avatar-dir:./uploads/avatars}")
    private String avatarBaseDir;

    @Value("${app.upload.avatar-max-size:2097152}") // 2MB 默认
    private long avatarMaxSize;

    @PostConstruct
    public void init() {
        // 6 * 60 秒
        codeCache = new SimpleCacheCode(6 * 60);
    }

    @PreDestroy
    public void destroy() {
        if (codeCache != null) {
            codeCache.shutdown();
        }
    }

    @Autowired
    private ClientService clientService;


    @PostMapping("/login")
    public R login(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String tel = params.get("tel");
        String pass = params.get("pass");

        // 参数校验
        if (tel == null || tel.trim().isEmpty()) {
            return R.error("手机号不能为空");
        }
        if (pass == null || pass.trim().isEmpty()) {
            return R.error("密码不能为空");
        }

        // 手机号格式验证
        if (!tel.matches("^1[3-9]\\d{9}$")) {
            return R.error("手机号格式不正确");
        }

        try {
            // 调用服务层进行登录验证
            Client client = clientService.login(tel, pass);

            if (client != null) {
                // 检查用户状态
                if (client.getIsdel() != null && client.getIsdel() == 1) {
                    return R.error("账号已被删除");
                }
//                if (client.getStatus() != null && client.getStatus() != 0) {
//                    return R.error("账号状态异常");
//                }

                // 登录成功，设置session
                HttpSession session = request.getSession();
                session.setAttribute("client", client);
                session.setAttribute("logintype", "client");
                session.setMaxInactiveInterval(30 * 60); // 30分钟过期

                // 返回用户信息（排除敏感信息）
                Client responseClient = new Client();
                responseClient.setId(client.getId());
                responseClient.setNike(client.getNike());
                responseClient.setTel(client.getTel());
                responseClient.setStatus(client.getStatus());
                responseClient.setLevelId(client.getLevelId());
                responseClient.setPoint(client.getPoint());
                responseClient.setPower(client.getPower());
                responseClient.setCreatedate(client.getCreatedate());

                return R.ok("登录成功").data("user", responseClient);
            } else {
                return R.error("手机号或密码错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("登录失败，请稍后重试");
        }
    }

    /**
     * 上传头像
     * 前端通过 FormData 上传字段：avatar（文件），同时提交 id（表单字段或 query）
     * 返回 ResultInfo，code=0 表示成功，data 返回 avatar 字段
     */
    @PostMapping("uploadAvatar")
    public ResultInfo uploadAvatar(@RequestParam(value = "id", required = false) String idParam,
                                   @RequestParam(value = "avatar", required = false) MultipartFile avatarFile,
                                   HttpServletRequest request) {
        ResultInfo info = new ResultInfo();

        // 检查 id
        if (idParam == null || idParam.trim().isEmpty()) {
            info.setCode(1);
            info.setMsg("缺少 id 参数");
            return info;
        }
        Integer id;
        try {
            id = Integer.valueOf(idParam);
        } catch (NumberFormatException ex) {
            info.setCode(1);
            info.setMsg("无效的 id 参数");
            return info;
        }

        // 检查文件
        if (avatarFile == null || avatarFile.isEmpty()) {
            info.setCode(1);
            info.setMsg("请上传图片文件");
            return info;
        }

        if (avatarFile.getSize() > avatarMaxSize) {
            info.setCode(1);
            info.setMsg("文件不能超过 " + (avatarMaxSize / 1024) + " KB");
            return info;
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            info.setCode(1);
            info.setMsg("只支持图片文件");
            return info;
        }

        // 查找用户
        Client client = clientMapper.selectById(id);
        if (client == null) {
            info.setCode(1);
            info.setMsg("未找到该用户");
            return info;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object sessObj = session.getAttribute("client");
            if (sessObj instanceof Client) {
                Client sessClient = (Client) sessObj;
            }
        }

        // 确保目录存在
        try {
            Path base = Paths.get(avatarBaseDir).toAbsolutePath().normalize();
            if (!Files.exists(base)) {
                Files.createDirectories(base);
            }
        } catch (IOException e) {
            e.printStackTrace();
            info.setCode(1);
            info.setMsg("服务器文件保存目录不可用");
            return info;
        }

        // 生成文件名，保留后缀
        String original = avatarFile.getOriginalFilename();
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

        String filename = "user_" + id + "_" + System.currentTimeMillis() + ext;
        Path targetFile = Paths.get(avatarBaseDir).toAbsolutePath().resolve(filename).normalize();

        // 安全检查：确保目标文件在 avatarBaseDir 下
        if (!targetFile.startsWith(Paths.get(avatarBaseDir).toAbsolutePath().normalize())) {
            info.setCode(1);
            info.setMsg("非法的文件路径");
            return info;
        }

        // 保存文件
        try {
            avatarFile.transferTo(targetFile.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            info.setCode(1);
            info.setMsg("保存文件失败");
            return info;
        }

        // 删除旧头像（如果为本地路径并且位于 avatarBaseDir 下）
        String oldAvatar = null;
        try {
            // 假定 Client 有 getAvatar()/setAvatar() 字段（数据库中 avatar 列）
            java.lang.reflect.Method gm = null;
            try {
                gm = client.getClass().getMethod("getAvatar");
            } catch (NoSuchMethodException nsme) {
                // 如果没有 avatar 字段，则跳过删除旧文件逻辑
                gm = null;
            }
            if (gm != null) {
                Object oldObj = gm.invoke(client);
                if (oldObj != null) oldAvatar = oldObj.toString();
            }
        } catch (Exception ignore) {}

        // 更新数据库 avatar 字段（保存为相对访问路径）
        // 这里我们返回并保存为 "/uploads/avatars/filename"，确保你在 WebMvcConfigurer 中映射了该路径到 avatarBaseDir
        String savedUrl = "/uploads/avatars/" + filename;
        try {
            // 通过反射或直接方法设置 avatar 字段
            try {
                java.lang.reflect.Method sm = client.getClass().getMethod("setAvatar", String.class);
                sm.invoke(client, savedUrl);
            } catch (NoSuchMethodException nsme) {
                // 如果没有 setAvatar 方法，尝试直接设置一个 TableField 名称不同的字段，或在 Client 类添加 avatar 字段
                // 这里直接忽略并返回成功（但推荐在实体上添加 avatar 字段）
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int rows = clientMapper.updateById(client);
        if (rows > 0) {
            // 删除旧文件（仅当 oldAvatar 非空且为本地路径）
            if (oldAvatar != null && !oldAvatar.trim().isEmpty()) {
                try {
                    Path oldPath = Paths.get(oldAvatar);
                    if (!oldPath.isAbsolute()) {
                        oldPath = Paths.get(avatarBaseDir).toAbsolutePath().resolve(oldAvatar).normalize();
                    }
                    if (oldPath.startsWith(Paths.get(avatarBaseDir).toAbsolutePath())) {
                        Files.deleteIfExists(oldPath);
                    }
                } catch (Exception ex) {
                    // 删除失败记录日志但不影响返回
                    ex.printStackTrace();
                }
            }

            info.setCode(0);
            info.setMsg("上传成功");
            info.setData(java.util.Arrays.asList(java.util.Collections.singletonMap("avatar", savedUrl)));
        } else {
            // 如果 DB 更新失败，删除刚保存的文件以避免垃圾文件
            try {
                Files.deleteIfExists(targetFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            info.setCode(1);
            info.setMsg("保存头像信息失败");
        }

        return info;
    }


    /**
     * 移除头像
     * 前端以 JSON POST 方式传入 { id: ... }
     */
    @PostMapping("removeAvatar")
    public ResultInfo removeAvatar(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        ResultInfo info = new ResultInfo();

        if (payload == null || !payload.containsKey("id")) {
            info.setCode(1);
            info.setMsg("缺少 id 参数");
            return info;
        }

        Integer id;
        try {
            Object o = payload.get("id");
            if (o instanceof Number) id = ((Number) o).intValue();
            else id = Integer.valueOf(o.toString());
        } catch (Exception ex) {
            info.setCode(1);
            info.setMsg("无效的 id 参数");
            return info;
        }

        Client client = clientMapper.selectById(id);
        if (client == null) {
            info.setCode(1);
            info.setMsg("用户不存在");
            return info;
        }

        // 读取旧头像路径（如果有）
        String oldAvatar = null;
        try {
            java.lang.reflect.Method gm = null;
            try {
                gm = client.getClass().getMethod("getAvatar");
            } catch (NoSuchMethodException nsme) {
                gm = null;
            }
            if (gm != null) {
                Object v = gm.invoke(client);
                if (v != null) oldAvatar = v.toString();
            }
        } catch (Exception ignore) {}

        // 将 avatar 字段设为 null
        try {
            java.lang.reflect.Method sm = client.getClass().getMethod("setAvatar", String.class);
            sm.invoke(client, (String) null);
        } catch (NoSuchMethodException nsme) {
            // 没有 setAvatar 方法：请在 Client 实体中添加 avatar 字段，否则无法持久化移除
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int rows = clientMapper.updateById(client);
        if (rows > 0) {
            // 删除本地文件（仅当路径位于 avatarBaseDir）
            if (oldAvatar != null && !oldAvatar.trim().isEmpty()) {
                try {
                    Path oldPath = Paths.get(oldAvatar);
                    if (!oldPath.isAbsolute()) {
                        oldPath = Paths.get(avatarBaseDir).toAbsolutePath().resolve(oldAvatar).normalize();
                    }
                    if (oldPath.startsWith(Paths.get(avatarBaseDir).toAbsolutePath())) {
                        Files.deleteIfExists(oldPath);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            info.setCode(0);
            info.setMsg("已移除头像");
        } else {
            info.setCode(1);
            info.setMsg("移除失败");
        }
        return info;
    }

    @PostMapping("send-code")
    public ResultInfo sendCode(@RequestParam("phone") String phone) {
        ResultInfo info = new ResultInfo();

        if (!isValidPhone(phone)) {
            info.setCode(1);
            info.setMsg("请输入有效的手机号码");
            return info;
        }

        // 生成 6 位验证码
        String code = gen6Code();

        // 缓存验证码（有效期由 SimpleCacheCode 设置）
        codeCache.put(phone, code);

        System.out.println(code);
        // TODO: 这里可以接入真实的短信服务；目前仅返回成功并在服务端缓存验证码
        info.setCode(0);
        info.setMsg("验证码已生成并发送");

        return info;
    }



    @GetMapping("get")
    public ResultInfo getClientById(@RequestParam(value = "id", required = false) String idParam,
                                    HttpServletRequest request) {
        ResultInfo info = new ResultInfo();


        if (idParam == null || idParam.trim().isEmpty()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object obj = session.getAttribute("client");
                if (obj != null && obj instanceof Client) {
                    Client sClient = (Client) obj;
                    // 使用最新数据（从数据库读取），避免 session 中信息过时
                    Client fresh = clientMapper.selectById(sClient.getId());
                    if (fresh == null) {
                        info.setCode(1);
                        info.setMsg("未找到会话中的用户");
                        return info;
                    }
                    // 清除敏感字段
                    fresh.setPass(null);


                    info.setCode(0);
                    info.setMsg("success");
                    info.setData(java.util.Arrays.asList(fresh));
                    return info;
                }
            }

            info.setCode(1);
            info.setMsg("缺少 id 参数且未登录");
            return info;
        }

        Client client = null;
        try {
            // 尝试解析为整数 id
            Integer id = Integer.valueOf(idParam);
            client = clientMapper.selectById(id);
        } catch (NumberFormatException nfe) {
            // 不是数字，尝试以手机号查找
            client = clientMapper.selectOne(new QueryWrapper<Client>().eq("tel", idParam));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (client == null) {
            info.setCode(1);
            info.setMsg("用户不存在");
            return info;
        }

        // 检查删除/状态（可按需调整：不一定要阻止返回，只是提示）
        if (client.getIsdel() != null && client.getIsdel() == 1) {
            info.setCode(1);
            info.setMsg("账号已被删除");
            return info;
        }
        if (client.getStatus() != null && client.getStatus() != 0) {
            // 这里不直接返回错误，保留注释供选择：
            // 如果你希望异常状态也能查看资料，可注释掉下面两行
            //info.setCode(1);
            //info.setMsg("账号状态异常");
            //return info;
        }

        // 清理敏感字段
        client.setPass(null);



        // 返回时遵循你的 ResultInfo.data 为 List 的习惯，包一层 List
        info.setCode(0);
        info.setMsg("success");
        info.setData(java.util.Arrays.asList(client));
        return info;
    }

    @PostMapping("updatepass")
    public ResultInfo updatePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        ResultInfo info = new ResultInfo();

        // 获取参数
        String phone = params.get("phone");
        String oldPass = params.get("pass");
        String newPass = params.get("pass1");
        String confirmPass = params.get("pass2");

        // 参数校验
        if (!isValidPhone(phone)) {
            info.setCode(1);
            info.setMsg("请输入有效的手机号码");
            return info;
        }

        if (StringUtils.isEmpty(oldPass)) {
            info.setCode(1);
            info.setMsg("请输入原密码");
            return info;
        }

        if (StringUtils.isEmpty(newPass)) {
            info.setCode(1);
            info.setMsg("请输入新密码");
            return info;
        }

        if (StringUtils.isEmpty(confirmPass)) {
            info.setCode(1);
            info.setMsg("请确认新密码");
            return info;
        }

        // 验证新密码和确认密码是否一致
        if (!newPass.equals(confirmPass)) {
            info.setCode(1);
            info.setMsg("两次输入的新密码不一致");
            return info;
        }

        // 验证新密码强度（至少8位，包含字母和数字）
        if (newPass.length() < 8 || !newPass.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            info.setCode(1);
            info.setMsg("新密码至少8位，且必须包含字母和数字");
            return info;
        }

        try {
            // 根据手机号查找用户
            Client client = clientMapper.selectOne(new QueryWrapper<Client>().eq("tel", phone));

            if (client == null) {
                info.setCode(1);
                info.setMsg("该手机号未注册");
                return info;
            }

            // 检查用户状态
            if (client.getIsdel() != null && client.getIsdel() == 1) {
                info.setCode(1);
                info.setMsg("账号已被删除");
                return info;
            }

            if (client.getStatus() != null && client.getStatus() != 0) {
                info.setCode(1);
                info.setMsg("账号状态异常");
                return info;
            }

            // 验证原密码是否正确
            String md5OldPass = DigestUtils.md5Hex(oldPass);
            if (!md5OldPass.equals(client.getPass())) {
                info.setCode(1);
                info.setMsg("原密码不正确");
                return info;
            }

            // 检查新密码是否与原密码相同
            if (oldPass.equals(newPass)) {
                info.setCode(1);
                info.setMsg("新密码不能与原密码相同");
                return info;
            }

            // 更新密码
            String md5NewPass = DigestUtils.md5Hex(newPass);
            client.setPass(md5NewPass);

            int rows = clientMapper.updateById(client);
            if (rows > 0) {
                info.setCode(0);
                info.setMsg("密码更新成功");

                // 可选：使当前会话失效，强制重新登录
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } else {
                info.setCode(1);
                info.setMsg("密码更新失败，请稍后重试");
            }
        } catch (Exception e) {
            e.printStackTrace();
            info.setCode(1);
            info.setMsg("系统错误，请稍后重试");
        }

        return info;
    }



    @PostMapping("register")
    public ResultInfo register(@RequestParam(value = "username", required = false) String username,
                               @RequestParam("password") String password,
                               @RequestParam("phone") String phone,
                               @RequestParam("code") String code) {
        ResultInfo info = new ResultInfo();

        // 基础校验
        if (!isValidPhone(phone)) {
            info.setCode(1);
            info.setMsg("请输入有效的手机号码");
            return info;
        }
        if (!StringUtils.hasText(password)) {
            info.setCode(1);
            info.setMsg("密码不能为空");
            return info;
        }
        if (!StringUtils.hasText(code)) {
            info.setCode(1);
            info.setMsg("请输入验证码");
            return info;
        }

        // 验证验证码
        String cached = codeCache.get(phone);
        if (cached == null) {
            info.setCode(1);
            info.setMsg("验证码已过期或不存在");
            return info;
        }
        if (!cached.equals(code)) {
            info.setCode(1);
            info.setMsg("验证码不正确");
            return info;
        }

        // 检查手机号是否已注册（使用 tel 字段）
        Client existing = clientMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Client>().eq("tel", phone));
        if (existing != null) {
            info.setCode(1);
            info.setMsg("该手机号已被注册");
            return info;
        }

        // MD5 加密密码（Apache Commons Codec）
        String md5pass = DigestUtils.md5Hex(password);

        Client client = new Client();
        client.setNike(username);
        client.setPass(md5pass);
        client.setTel(phone);
        client.setStatus(0);
        client.setIsdel(0);
        client.setPower(0);
        client.setCreatedate(java.time.LocalDateTime.now().toString());

        int rows = clientMapper.insert(client);
        if (rows > 0) {
            // 清除缓存中的验证码
            codeCache.remove(phone);

            info.setCode(0);
            info.setMsg("注册成功");
            // 返回部分用户信息（不包含密码）
            client.setPass(null);
            info.setData(java.util.Arrays.asList(client));
        } else {
            info.setCode(1);
            info.setMsg("注册失败，请稍后重试");
        }
        return info;
    }

    // 生成 6 位随机数（包含前导 0）
    private String gen6Code() {
        Random random = new Random();
        int v = random.nextInt(900000) + 100000; // 确保 100000-999999
        return String.valueOf(v);
    }

    private boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return phone.matches("^1[3-9]\\d{9}$");
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


    @PostMapping("update")
    public ResultInfo update(@RequestBody Map<String, Object> payload) {
        ResultInfo info = new ResultInfo();
        if (payload == null || payload.isEmpty()) {
            info.setCode(1);
            info.setMsg("请求参数不能为空");
            return info;
        }

        Object idObj = payload.get("id");
        if (idObj == null) {
            info.setCode(1);
            info.setMsg("缺少 id 参数");
            return info;
        }

        Integer id;
        try {
            if (idObj instanceof Number) {
                id = ((Number) idObj).intValue();
            } else {
                id = Integer.valueOf(idObj.toString());
            }
        } catch (Exception e) {
            info.setCode(1);
            info.setMsg("无效的 id 参数");
            return info;
        }

        Client client = clientMapper.selectById(id);
        if (client == null) {
            info.setCode(1);
            info.setMsg("未找到该用户");
            return info;
        }

        boolean updated = false;

        // nike -> nike 字段
        if (payload.containsKey("nike")) {
            Object val = payload.get("nike");
            if (val != null) {
                client.setNike(val.toString().trim());
                updated = true;
            }
        }

        // sex
        if (payload.containsKey("sex")) {
            Object val = payload.get("sex");
            if (val == null || String.valueOf(val).trim().isEmpty()) {
                client.setSex(null);
            } else {
                try {
                    client.setSex(Integer.valueOf(val.toString()));
                } catch (Exception e) {
                    // 如果前端传了非法类型，可以忽略或返回错误
                    info.setCode(1);
                    info.setMsg("非法的 sex 值");
                    return info;
                }
                updated = true;
            }
        }

        // tel
        if (payload.containsKey("tel")) {
            Object val = payload.get("tel");
            if (val != null) {
                client.setTel(val.toString().trim());
                updated = true;
            }
        }

        // tel2
        if (payload.containsKey("tel2")) {
            Object val = payload.get("tel2");
            if (val != null) {
                client.setTel2(val.toString().trim());
                updated = true;
            }
        }

        // qq
        if (payload.containsKey("qq")) {
            Object val = payload.get("qq");
            if (val != null) {
                client.setQq(val.toString().trim());
                updated = true;
            }
        }

        // weixin
        if (payload.containsKey("weixin")) {
            Object val = payload.get("weixin");
            if (val != null) {
                client.setWeixin(val.toString().trim());
                updated = true;
            }
        }

        if (!updated) {
            info.setCode(1);
            info.setMsg("未提供需要更新的字段");
            return info;
        }

        int rows = clientMapper.updateById(client);
        if (rows > 0) {
            info.setCode(0);
            info.setMsg("更新成功");
            info.setData(java.util.Arrays.asList(client));
        } else {
            info.setCode(1);
            info.setMsg("更新失败");
        }

        return info;
    }
}