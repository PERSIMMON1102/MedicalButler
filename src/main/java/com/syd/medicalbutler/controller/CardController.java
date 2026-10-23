package com.syd.medicalbutler.controller;

import com.syd.medicalbutler.pojo.Card;
import com.syd.medicalbutler.pojo.Client;
import com.syd.medicalbutler.service.CardService;
import com.syd.medicalbutler.service.ClientService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/card")
public class CardController {

    @Autowired
    private CardService cardService;

    @Autowired
    private ClientService clientService;

    @PostMapping("/recharge")
    @Transactional
    public Map<String, Object> recharge(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        if (payload == null) {
            result.put("code", 400);
            result.put("msg", "请求体不能为空");
            return result;
        }

        // 解析 clientId
        Integer clientId = null;
        try {
            Object c = payload.get("clientId");
            if (c != null) {
                if (c instanceof Number) clientId = ((Number) c).intValue();
                else clientId = Integer.valueOf(String.valueOf(c));
            }
        } catch (Exception e) {
            result.put("code", 400);
            result.put("msg", "clientId 格式错误");
            return result;
        }

        // 解析 amount
        Double amount = null;
        try {
            Object a = payload.get("amount");
            if (a != null) {
                if (a instanceof Number) amount = ((Number) a).doubleValue();
                else amount = Double.valueOf(String.valueOf(a));
            }
        } catch (Exception e) {
            result.put("code", 400);
            result.put("msg", "amount 格式错误");
            return result;
        }

        if (clientId == null || amount == null || amount <= 0) {
            result.put("code", 400);
            result.put("msg", "参数错误：clientId 与 amount 必须提供且 amount > 0");
            return result;
        }

        // 获取客户信息
        Client client = clientService.getById(clientId);
        if (client == null) {
            result.put("code", 404);
            result.put("msg", "找不到客户信息");
            return result;
        }

        Card card;
        boolean isNewCard = false;

        // 检查客户是否已有卡
        if (client.getCardId() != null) {
            // 已有卡，获取现有卡信息
            card = cardService.getById(client.getCardId());
            if (card == null) {
                // 卡记录不存在，创建新卡
                card = createNewCard(clientId, amount);
                isNewCard = true;
            } else {
                // 更新现有卡的余额
                card.setCount(card.getCount() + amount);
                card.setTocount(card.getTocount() + amount);

                // 更新卡信息
                boolean updated = cardService.updateById(card);
                if (!updated) {
                    result.put("code", 500);
                    result.put("msg", "更新卡信息失败");
                    return result;
                }
            }
        } else {
            // 首次充值，创建新卡
            card = createNewCard(clientId, amount);
            isNewCard = true;
        }

        // 如果是新卡，保存卡信息并更新客户关联
        if (isNewCard) {
            boolean saved = cardService.save(card);
            if (!saved) {
                result.put("code", 500);
                result.put("msg", "保存卡信息失败");
                return result;
            }

            // 更新客户的卡ID
            client.setCardId(card.getId());
        }

        // 更新客户积分和等级
        Integer oldPoint = client.getPoint() == null ? 0 : client.getPoint();
        client.setPoint(oldPoint + amount.intValue());

        Integer newLevelId = determineLevelByAccumulated(client.getPoint());
        client.setLevelId(newLevelId);

        // 更新客户信息
        boolean updatedClient = clientService.updateById(client);
        if (!updatedClient) {
            throw new RuntimeException("更新 client 失败");
        }

        // 返回结果
        result.put("code", 0);
        result.put("msg", "充值成功");
        Map<String, Object> data = new HashMap<>();
        data.put("card", card);
        data.put("client", client);
        data.put("isNewCard", isNewCard);
        result.put("data", data);
        return result;
    }

    // 创建新卡
    private Card createNewCard(Integer clientId, Double amount) {
        String code = generate8DigitCode();

        Card card = new Card();
        card.setCode(code);
        card.setCount(amount);
        card.setTocount(amount);
        card.setPrice(amount);
        card.setClientId(clientId);
        card.setStatus(1);
        card.setIsdel(0);
        card.setComplte(1);
        card.setCreatedate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return card;
    }

    private String generate8DigitCode() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(r.nextInt(10));
        }

        // 检查卡号是否已存在（极小概率但需要处理）
        String code = sb.toString();
        QueryWrapper<Card> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        long count = cardService.count(queryWrapper);

        // 如果卡号已存在，重新生成
        while (count > 0) {
            sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(r.nextInt(10));
            }
            code = sb.toString();
            queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("code", code);
            count = cardService.count(queryWrapper);
        }

        return code;
    }

    @GetMapping("/info")
    public Map<String, Object> getCardInfo(@RequestParam Integer clientId) {
        Map<String, Object> result = new HashMap<>();

        if (clientId == null) {
            result.put("code", 400);
            result.put("msg", "clientId不能为空");
            return result;
        }

        // 获取客户信息
        Client client = clientService.getById(clientId);
        if (client == null) {
            result.put("code", 404);
            result.put("msg", "找不到客户信息");
            return result;
        }

        // 获取卡信息
        Card card = null;
        if (client.getCardId() != null) {
            card = cardService.getById(client.getCardId());
        }

        // 构建返回数据
        result.put("code", 0);
        result.put("msg", "获取成功");

        Map<String, Object> data = new HashMap<>();
        data.put("client", client);
        data.put("card", card);

        result.put("data", data);
        return result;
    }


    private Integer determineLevelByAccumulated(Integer accumulated) {
        if (accumulated == null) return 1;
        if (accumulated >= 20000) return 5;
        if (accumulated >= 10000) return 4;
        if (accumulated >= 5000) return 3;
        if (accumulated >= 2000) return 2;
        return 1;
    }
}