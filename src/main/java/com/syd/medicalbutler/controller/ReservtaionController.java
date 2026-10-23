package com.syd.medicalbutler.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syd.medicalbutler.pojo.*;
import com.syd.medicalbutler.service.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/reservation")
public class ReservtaionController {


    @Autowired
    private PayinfoService payinfoService;

    @Autowired
    private ReserverService reserverService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Autowired
    private CardService cardService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private OrderMainService orderMainService;



    @GetMapping("/reserverList")
    public ApiResponse reserverList(
            @RequestParam(value = "clientId", required = false) Integer clientId,
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        // 分页构造
        Page<Reserver> pg = new Page<>(page, size);
        QueryWrapper<Reserver> qw = new QueryWrapper<>();
        // 过滤未删除
        qw.eq("isdel", 0);

        if (clientId != null) {
            qw.eq("client_id", clientId);
        }

        if (status != null) {
            qw.eq("status", status);
        }

        if (StringUtils.hasText(q)) {
            // 简单模糊：尝试匹配 todate / date / remark 等字段之一（根据实际字段调整）
            // 这里使用 'todate LIKE q OR date LIKE q OR remark LIKE q'
            qw.and(w -> w.like("todate", q).or().like("date", q).or().like("remark", q));
        }

        Page<Reserver> res = reserverService.page(pg, qw);

        Map<String,Object> data = new HashMap<>();
        data.put("total", res.getTotal());
        data.put("records", res.getRecords());
        data.put("page", res.getCurrent());
        data.put("size", res.getSize());
        return ApiResponse.ok(data);
    }

    /**
     * 根据预约 id 获取单条预约详情（包含尝试附带 client/project）
     * GET /reservation/reserverDetail?id=123
     */
    @GetMapping("/reserverDetail")
    public ApiResponse reserverDetail(@RequestParam("id") Integer id) {
        if (id == null) return ApiResponse.fail("id 不能为空");
        Reserver r = reserverService.getById(id);
        if (r == null || (r.getIsdel() != null && r.getIsdel() == 1)) {
            return ApiResponse.fail("预约不存在");
        }
        Map<String,Object> data = new HashMap<>();
        data.put("reserver", r);
        try {
            if (r.getClientId() != null) {
                Object clientObj = clientService.getById(r.getClientId());
                data.put("client", clientObj);
            } else {
                data.put("client", null);
            }
        } catch (Exception ex) {
            data.put("client", null);
        }
        try {
            if (r.getProjectId() != null && r.getProjectId() > 0) {
                Object projectObj = projectService.getById(r.getProjectId());
                data.put("project", projectObj);
            } else {
                data.put("project", null);
            }
        } catch (Exception ex) {
            data.put("project", null);
        }
        return ApiResponse.ok(data);
    }

    /**
     * 更新预约（部分更新）
     * POST /reservation/reserverUpdate
     * 接收 JSON，例如：
     * { "id":123, "todate":"2025-09-20", "totime":"10:00", "status":1, "remark":"更改备注", "projectId":5 }
     */
    @PostMapping("/reserverUpdate")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse reserverUpdate(@RequestBody Map<String,Object> payload) {
        if (payload == null) return ApiResponse.fail("参数为空");
        Object idObj = payload.get("id");
        if (idObj == null) return ApiResponse.fail("缺少 id");
        Integer id;
        try {
            id = Integer.valueOf(String.valueOf(idObj));
        } catch (Exception ex) {
            return ApiResponse.fail("id 格式错误");
        }

        Reserver exist = reserverService.getById(id);
        if (exist == null || (exist.getIsdel() != null && exist.getIsdel() == 1)) {
            return ApiResponse.fail("预约不存在或已删除");
        }

        boolean modified = false;

        if (payload.containsKey("todate")) {
            Object v = payload.get("todate");
            if (v != null && StringUtils.hasText(String.valueOf(v))) {
                exist.setTodate(String.valueOf(v));
                modified = true;
            }
        }
        if (payload.containsKey("totime")) {
            Object v = payload.get("totime");
            exist.setTotime(v == null ? null : String.valueOf(v));
            modified = true;
        }
        if (payload.containsKey("status")) {
            Object v = payload.get("status");
            if (v != null) {
                try {
                    exist.setStatus(Integer.valueOf(String.valueOf(v)));
                    // 若需要在 status 变化时记录 confirm_time / cancel_time，可在此处设置
                    if (exist.getStatus() != null) {
                        int s = exist.getStatus();
                        String now = LocalDateTime.now().format(DT_FMT);
                        if (s == 1) { // 已确认
                            try {
                                java.lang.reflect.Method m = exist.getClass().getMethod("setConfirmTime", String.class);
                                if (m != null) m.invoke(exist, now);
                            } catch (Exception ignore) {}
                        } else if (s == 2) { // 已取消
                            try {
                                java.lang.reflect.Method m = exist.getClass().getMethod("setCancelTime", String.class);
                                if (m != null) m.invoke(exist, now);
                            } catch (Exception ignore) {}
                        }
                    }
                    modified = true;
                } catch (Exception ignore) {}
            }
        }
        if (payload.containsKey("projectId") || payload.containsKey("project_id")) {
            Object v = payload.containsKey("projectId") ? payload.get("projectId") : payload.get("project_id");
            if (v != null) {
                try {
                    exist.setProjectId(Integer.valueOf(String.valueOf(v)));
                    modified = true;
                } catch (Exception ignore) {}
            }
        }
        if (payload.containsKey("clientId") || payload.containsKey("client_id")) {
            Object v = payload.containsKey("clientId") ? payload.get("clientId") : payload.get("client_id");
            if (v != null) {
                try {
                    exist.setClientId(Integer.valueOf(String.valueOf(v)));
                    modified = true;
                } catch (Exception ignore) {}
            }
        }

        if (!modified) {
            return ApiResponse.fail("未提供可更新的字段");
        }

        try {
            reserverService.updateById(exist);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponse.fail("更新失败：" + ex.getMessage());
        }

        Map<String,Object> data = new HashMap<>();
        data.put("reserver", exist);
        try {
            if (exist.getClientId() != null) data.put("client", clientService.getById(exist.getClientId()));
        } catch (Exception ignore) { data.put("client", null); }
        try {
            if (exist.getProjectId() != null && exist.getProjectId() > 0) data.put("project", projectService.getById(exist.getProjectId()));
        } catch (Exception ignore) { data.put("project", null); }

        return ApiResponse.ok(data);
    }



    // 新增 DTO 用于退款请求
    @Data
    public static class RefundRequest {
        private Integer id;
        private Double amount;
        private String reason;
    }

    // Refund 接口
    @PostMapping("/refund")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse refund(@RequestBody RefundRequest req) {
        if (req == null || req.getId() == null) return ApiResponse.fail("缺少 id");
        Integer id = req.getId();
        Payinfo p = payinfoService.getById(id);
        if (p == null || (p.getIsdel() != null && p.getIsdel() == 1)) {
            return ApiResponse.fail("订单不存在");
        }
        // 检查支付状态：仅已支付的可退款（paystatus == 1）
        if (p.getPaystatus() == null || p.getPaystatus() != 1) {
            return ApiResponse.fail("仅已支付订单可退款");
        }

        // 判定支付方式：优先使用 paymode 字段（如果存在）
        String payMode = null;
        try {
            // 推断：如果 Payinfo 有 getPaymode / getPayMode 方法
            java.lang.reflect.Method m = p.getClass().getMethod("getPaymode");
            if (m != null) {
                Object v = m.invoke(p);
                if (v != null) payMode = v.toString();
            }
        } catch (Exception ignore) {
            // 忽略反射失败 —— 后续用 card 关联判断
        }

        // 如果 payMode 为 online，返回前端说明（不在此接口直接退）
        if ("online".equalsIgnoreCase(payMode)) {
            return ApiResponse.fail("该订单为在线支付，请在支付平台/渠道发起退款或联系管理员");
        }

        // 计算退款金额
        double refundAmount = 0.0;
        if (req.getAmount() != null && req.getAmount() > 0) {
            refundAmount = req.getAmount();
        } else {
            // 尝试使用 price 字段
            refundAmount = (p.getPrice() == null ? 0.0 : p.getPrice());
        }
        if (refundAmount <= 0) {
            return ApiResponse.fail("退款金额无效");
        }

        // 找到关联卡：优先通过 card 表中 payinfo_id 关联，或通过 Payinfo 中 cardId 字段（若存在）
        Card card = null;
        // 1. 通过 cardService 查询 card.payinfo_id = p.id
        QueryWrapper<Card> cw = new QueryWrapper<>();
        cw.eq("isdel", 0).eq("payinfo_id", p.getId());
        card = cardService.getOne(cw);

        // 2. 若未找到，尝试通过 payinfo 中的 cardId 字段（反射读取或直接字段）
        if (card == null) {
            try {
                java.lang.reflect.Method gm = p.getClass().getMethod("getCardId");
                if (gm != null) {
                    Object cv = gm.invoke(p);
                    if (cv != null) {
                        Integer cardId = Integer.valueOf(cv.toString());
                        card = cardService.getById(cardId);
                        if (card != null && (card.getIsdel() != null && card.getIsdel() == 1)) {
                            card = null;
                        }
                    }
                }
            } catch (Exception ignored) { }
        }

        if (card == null) {
            return ApiResponse.fail("未找到关联的实卡信息，无法退回至卡");
        }

        // 幂等与并发控制：若 payinfo 已标记退款则直接返回成功（幂等）
        if (p.getPaystatus() != null && p.getPaystatus() == 2) {
            Map<String,Object> data = new HashMap<>();
            data.put("message", "订单已退款");
            return ApiResponse.ok(data);
        }

        // 事务更新：回退卡余额 & 更新 payinfo 状态
        // 可选：使用乐观锁或 FOR UPDATE；此处使用简单更新并校验结果
        double before = card.getCount() == null ? 0.0 : card.getCount();
        double after = before + refundAmount;
        card.setCount(after);
        // 可设置 card.tocount 或其它字段以记录变动
        try {
            // 更新 card
            cardService.updateById(card);

            // 更新 payinfo 状态为已退款(paystatus=2)
            p.setPaystatus(2);
            payinfoService.updateById(p);

            // 可记录退款流水：写入 refund_log 表或系统日志（此处放入返回）
            Map<String,Object> resp = new HashMap<>();
            resp.put("payinfoId", p.getId());
            resp.put("refundAmount", refundAmount);
            resp.put("cardId", card.getId());
            resp.put("cardBefore", before);
            resp.put("cardAfter", after);
            resp.put("message", "退款成功（已退回至实卡账户）");

            return ApiResponse.ok(resp);

        } catch (Exception ex) {
            // 回滚由 @Transactional 处理
            ex.printStackTrace();
            return ApiResponse.fail("退款操作失败：" + ex.getMessage());
        }
    }


    /**
     * 列表：分页返回支付记录（购买历史）
     * 前端可以通过 q、paystatus 等参数过滤
     */
    @GetMapping("/list")
    public ApiResponse list(
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "paystatus", required = false) Integer paystatus,
            @RequestParam(value = "clientId", required = false) Integer clientId
    ) {
        Page<Payinfo> pg = new Page<>(page, size);
        QueryWrapper<Payinfo> qw = new QueryWrapper<>();
        qw.eq("isdel", 0);
        if (paystatus != null) {
            qw.eq("paystatus", paystatus);
        }
        if (clientId != null) {
            qw.eq("client_id", clientId);
        }
        // 简单模糊搜索：这里 Payinfo 没有 client 名称字段，如果需要基于客户名搜索需联表或在 service 层扩展
        if (StringUtils.hasText(q)) {
            // 假设 q 可能为 projectId 或 date 的一部分，示例使用 date LIKE
            qw.like("date", q);
        }
        Page<Payinfo> res = payinfoService.page(pg, qw);
        Map<String, Object> data = new HashMap<>();
        data.put("total", res.getTotal());
        data.put("records", res.getRecords());
        data.put("page", res.getCurrent());
        data.put("size", res.getSize());
        return ApiResponse.ok(data);
    }

    /**
     * 根据 payinfo id 获取订单详情（前端点击查看或点击预约时调用）
     * 返回结构包含：payinfo, client, card(如果存在), project(detail), project items (selected_ids, selected)
     */
    @GetMapping("/detail")
    public ApiResponse detail(@RequestParam("id") Integer id) {
        if (id == null) return ApiResponse.fail("id 不能为空");
        Payinfo p = payinfoService.getById(id);
        if (p == null || (p.getIsdel() != null && p.getIsdel() == 1)) {
            return ApiResponse.fail("订单不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("payinfo", p);

        // 1. 客户信息（若存在）
        try {
            if (p.getClientId() != null) {
                // clientService 需实现 getById
                Object clientObj = null;
                try {
                    clientObj = clientService.getById(p.getClientId());
                } catch (Exception ex) {
                    clientObj = null;
                }
                result.put("client", clientObj);
            } else {
                result.put("client", null);
            }
        } catch (Exception ex) {
            result.put("client", null);
        }

        // 2. 尝试查找关联的实卡信息（card）
        try {
            Card card = null;
            // 优先使用 card.payinfo_id 关联
            QueryWrapper<Card> cw = new QueryWrapper<>();
            cw.eq("isdel", 0).eq("payinfo_id", p.getId());
            card = cardService.getOne(cw);
            // 备用：如果 payinfo 有 card_id 字段
            if (card == null) {
                try {
                    java.lang.reflect.Method gm = p.getClass().getMethod("getCardId");
                    if (gm != null) {
                        Object cv = gm.invoke(p);
                        if (cv != null) {
                            Integer cardId = Integer.valueOf(cv.toString());
                            Card tmp = cardService.getById(cardId);
                            if (tmp != null && (tmp.getIsdel() == null || tmp.getIsdel() == 0)) {
                                card = tmp;
                            }
                        }
                    }
                } catch (Exception ignore) { }
            }
            result.put("card", card);
        } catch (Exception ex) {
            result.put("card", null);
        }

        // 3. 项目信息与检查项（如果 payinfo.projectId 有有效 id）
        try {
            Integer projectId = p.getProjectId();
            if (projectId != null && projectId > 0) {
                ProjectDetailVO projectDetail = null;
                ProjectItemsResult itemsResult = null;
                try {
                    projectDetail = projectService.getProjectDetailById(projectId);
                } catch (Exception ignore) { projectDetail = null; }
                try {
                    itemsResult = projectService.getProjectItemsByProjectId(projectId);
                } catch (Exception ignore) { itemsResult = null; }

                result.put("project", projectDetail);
                if (itemsResult != null) {
                    Map<String,Object> itemsData = new HashMap<>();
                    itemsData.put("selected_ids", itemsResult.getSelectedIds());
                    itemsData.put("selected", itemsResult.getSelectedItems());
                    result.put("projectItems", itemsData);
                } else {
                    result.put("projectItems", null);
                }
            } else {
                result.put("project", null);
                result.put("projectItems", null);
            }
        } catch (Exception ex) {
            result.put("project", null);
            result.put("projectItems", null);
        }

        return ApiResponse.ok(result);
    }
    /**
     * 为某条支付记录直接创建预约（前端点击卡片上的预约）
     * 也可以直接使用 /create 来创建任意预约。
     */
    @PostMapping("/createFromPay")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse createFromPay(@RequestBody CreateReserverFromPayReq req) {
        if (req == null || req.getPayinfoId() == null) {
            return ApiResponse.fail("缺少 payinfoId");
        }
        Payinfo p = payinfoService.getById(req.getPayinfoId());
        if (p == null || (p.getIsdel() != null && p.getIsdel() == 1)) {
            return ApiResponse.fail("对应订单不存在");
        }

        // 构建预约
        Reserver r = new Reserver();
        OrderMain orderMain = new OrderMain();
        String now = LocalDateTime.now().format(DT_FMT);
        r.setDate(now);
        r.setTodate(req.getTodate());
        r.setTotime(req.getTotime());
        r.setClientId(p.getClientId());
        r.setProjectId(p.getProjectId());
        orderMain.setDate(req.getTodate());
        orderMain.setClientId(p.getClientId());
        orderMain.setProjectId(p.getProjectId());
        r.setStatus(req.getStatus() == null ? 0 : req.getStatus());
        r.setIsdel(0);

        reserverService.save(r);
        orderMainService.save(orderMain);
        Map<String,Object> data = new HashMap<>();
        data.put("reserverId", r.getId());
        data.put("reserver", r);
        return ApiResponse.ok(data);
    }

    /**
     * 通用创建预约接口（前端直接提交预约表单）
     */
    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse create(@RequestBody CreateReserverReq req) {
        if (req == null) return ApiResponse.fail("参数为空");
        if (!StringUtils.hasText(req.getTodate())) {
            return ApiResponse.fail("请选择预约日期");
        }
        if (req.getClientId() == null) return ApiResponse.fail("clientId 必须提供");

        Reserver r = new Reserver();
        String now = LocalDateTime.now().format(DT_FMT);
        r.setDate(now);
        r.setTodate(req.getTodate());
        r.setTotime(req.getTotime());
        r.setClientId(req.getClientId());
        r.setProjectId(req.getProjectId() == null ? 0 : req.getProjectId());
        r.setStatus(req.getStatus() == null ? 0 : req.getStatus());
        r.setIsdel(0);

        reserverService.save(r);
        Map<String,Object> data = new HashMap<>();
        data.put("reserverId", r.getId());
        data.put("reserver", r);
        return ApiResponse.ok(data);
    }

    /**
     * 逻辑删除预约
     */
    @PostMapping("/delete")
    public ApiResponse delete(@RequestParam("id") Integer id) {
        if (id == null) return ApiResponse.fail("id 不能为空");
        Reserver r = reserverService.getById(id);
        if (r == null) return ApiResponse.fail("预约不存在");
        r.setIsdel(1);
        reserverService.updateById(r);
        return ApiResponse.ok();
    }

    // DTOs

    @Data
    public static class CreateReserverFromPayReq {
        private Integer payinfoId;
        private String todate;
        private String totime;
        private Integer status; // 可选
    }

    @Data
    public static class CreateReserverReq {
        private String todate;
        private String totime;
        private Integer clientId;
        private Integer projectId;
        private Integer status;
    }

    // 复用你之前的 ApiResponse 定义；若有重复类则可移动到公共包
    @Data
    public static class ApiResponse {
        private int code; // 0 success, 非0失败
        private String message;
        private Object data;

        public static ApiResponse ok(Object data) {
            ApiResponse r = new ApiResponse();
            r.setCode(0);
            r.setMessage("ok");
            r.setData(data);
            return r;
        }
        public static ApiResponse ok() { return ok(null); }

        public static ApiResponse fail(String msg) {
            ApiResponse r = new ApiResponse();
            r.setCode(-1);
            r.setMessage(msg);
            r.setData(null);
            return r;
        }
    }


}
