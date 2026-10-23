package com.syd.medicalbutler.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syd.medicalbutler.pojo.Card;
import com.syd.medicalbutler.pojo.Payinfo;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.syd.medicalbutler.service.CardService;
import com.syd.medicalbutler.service.PayinfoService;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayinfoService payinfoService;

    @Autowired
    private CardService cardService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/purchase")
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse purchase(@RequestBody PurchaseRequest req) {
        // 基本校验
        if (req == null || req.getCart() == null || req.getCart().isEmpty()) {
            return ApiResponse.fail("购物车为空");
        }
        if (req.getPayMode() == null || (!"online".equals(req.getPayMode()) && !"card".equals(req.getPayMode()))) {
            return ApiResponse.fail("不支持的支付方式");
        }

        double total = req.getCart().stream()
                .mapToDouble(it -> (it.getPrice() == null ? 0d : it.getPrice()) * (it.getQty() == null ? 0 : it.getQty()))
                .sum();

        String now = LocalDateTime.now().format(DT_FMT);


        Integer clientId = req.getClientId();

        // card 支付逻辑：校验卡号与余额并扣款
        if ("card".equals(req.getPayMode())) {
            String cardCode = req.getCardCode();
            if (cardCode == null || cardCode.trim().isEmpty()) {
                return ApiResponse.fail("请提供卡号");
            }
            QueryWrapper<Card> qw = new QueryWrapper<>();
            qw.eq("code", cardCode.trim()).eq("isdel", 0);
            Card card = cardService.getOne(qw);
            if (card == null) {
                return ApiResponse.fail("卡号不存在");
            }
            // 检查卡状态：假设 1 表示可用
            if (card.getStatus() == null || card.getStatus() != 1) {
                return ApiResponse.fail("卡不可用");
            }
            double balance = card.getCount() == null ? 0d : card.getCount();
            if (balance < total) {
                return ApiResponse.fail("卡余额不足");
            }

            // 扣款
            card.setCount(balance - total);
            // 可根据业务设置为已使用或标记完成
            card.setComplte(1);
            cardService.updateById(card);



            List<Payinfo> inserted = new ArrayList<>();
            for (PurchaseItem it : req.getCart()) {
                Payinfo p = new Payinfo();
                p.setDate(now);
                p.setClientId(clientId);
                p.setProjectId(it.getProjectId());
                p.setPrice(it.getPrice() * it.getQty());

                p.setPaystatus(1);
                p.setIsdel(0);
                payinfoService.save(p);
                inserted.add(p);
            }

            // 将最后一个 payinfo 的 id 写入卡记录（如果业务需要关联）
            if (!inserted.isEmpty()) {
                card.setPayinfoId(inserted.get(0).getId());
                cardService.updateById(card);
            }

            Map<String,Object> data = new HashMap<>();
            data.put("total", total);
            data.put("payMode","card");
            data.put("cardCode", cardCode);
            data.put("payRecords", inserted);
            return ApiResponse.ok(data);
        }

        // online 支付：模拟在线支付成功，直接插入 payinfo，paystatus=1
        if ("online".equals(req.getPayMode())) {
            List<Payinfo> inserted = new ArrayList<>();
            for (PurchaseItem it : req.getCart()) {
                Payinfo p = new Payinfo();
                p.setDate(now);
                p.setClientId(clientId);
                p.setProjectId(it.getProjectId());
                p.setPrice(it.getPrice() * it.getQty());
                // paystatus: 1=已支付
                p.setPaystatus(1);
                p.setIsdel(0);
                payinfoService.save(p);
                inserted.add(p);
            }
            Map<String,Object> data = new HashMap<>();
            data.put("total", total);
            data.put("payMode","online");
            data.put("payRecords", inserted);
            return ApiResponse.ok(data);
        }

        return ApiResponse.fail("未知错误");
    }

    // ---------- DTOs & 辅助类 ----------

    @Data
    public static class PurchaseRequest {
        private List<PurchaseItem> cart;
        private String buyType; // 固定 'self'（前端传）
        private Integer clientId;
        private String clientTel;
        private String payMode; // online | card
        private String cardCode;
        private String comments;
    }

    @Data
    public static class PurchaseItem {
        private Integer projectId;
        private String name;
        private Double price;
        private Integer qty;
    }

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