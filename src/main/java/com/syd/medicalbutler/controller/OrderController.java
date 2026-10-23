package com.syd.medicalbutler.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syd.medicalbutler.mapper.ClientMapper;
import com.syd.medicalbutler.mapper.OrderDetailsMapper;
import com.syd.medicalbutler.mapper.OrderMainMapper;
import com.syd.medicalbutler.mapper.ProjectItemMapper;
import com.syd.medicalbutler.mapper.ProjectMapper;
import com.syd.medicalbutler.mapper.ReserverMapper;
import com.syd.medicalbutler.pojo.Client;
import com.syd.medicalbutler.pojo.OrderDetails;
import com.syd.medicalbutler.pojo.OrderMain;
import com.syd.medicalbutler.pojo.Project;
import com.syd.medicalbutler.pojo.ProjectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderMainMapper orderMainMapper;

    @Autowired
    private OrderDetailsMapper orderDetailsMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectItemMapper projectItemMapper;

    @Autowired(required = false)
    private ReserverMapper reserverMapper;

    @Autowired
    private ClientMapper clientMapper;

    // 列表接口（同之前实现）
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "12") Integer size,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "range", required = false) String range
    ) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Page<OrderMain> pager = new Page<>(page, size);
            QueryWrapper<OrderMain> qw = new QueryWrapper<>();
            qw.eq("isdel", 0);
            if (!StringUtils.isEmpty(q)) {
                qw.and(w -> w.like("info", q).or().like("date", q).or().eq("user_id", q));
            }
            IPage<OrderMain> pageResult = orderMainMapper.selectPage(pager, qw);

            List<Map<String, Object>> records = new ArrayList<>();
            for (OrderMain m : pageResult.getRecords()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", m.getId());
                row.put("main_id", m.getId());
                Client client = clientMapper.selectById(m.getClientId());
                row.put("clientName", client.getNike());
                row.put("date", m.getDate());
                row.put("info", m.getInfo());
                row.put("comment", m.getInfo());
                row.put("total", null);
                row.put("status", "paid"); // 占位
                String itemName = "";
                if (m.getProjectId() != null) {
                    Project p = projectMapper.selectById(m.getProjectId());
                    if (p != null) itemName = p.getName();
                }
                row.put("item_name", itemName);
                records.add(row);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", pageResult.getTotal());
            data.put("records", records);

            resp.put("code", 0);
            resp.put("msg", "ok");
            resp.put("data", data);
            return resp;
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.put("code", 500);
            resp.put("msg", "服务器错误：" + ex.getMessage());
            return resp;
        }
    }


    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable("id") Integer id) {
        Map<String, Object> resp = new HashMap<>();
        try {
            OrderMain main = orderMainMapper.selectById(id);
            if (main == null) {
                resp.put("code", 1);
                resp.put("msg", "订单不存在");
                return resp;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", main.getId());
            data.put("main_id", main.getId());
            data.put("date", main.getDate());
            data.put("info", main.getInfo());
            data.put("project_id", main.getProjectId());

            // 查询 client 并返回姓名
            if (main.getClientId() != null) {
                Client client = clientMapper.selectById(main.getClientId());
                if (client != null) {
                    data.put("clientId", client.getId());
                    data.put("clientName", client.getNike());
                    data.put("clientStatus", client.getStatus());
                }
            }

            Integer projectId = main.getProjectId();
            List<ProjectItem> items = new ArrayList<>();
            if (projectId != null) {
                Project project = projectMapper.selectById(projectId);
                if (project != null) {
                    data.put("project_name", project.getName());
                    String itemIdsStr = project.getItemIds();
                    if (!StringUtils.isEmpty(itemIdsStr)) {
                        String normalized = itemIdsStr.trim().replaceAll("；", ",").replaceAll("\\s+", "");
                        String[] parts = normalized.split(",");
                        List<Integer> ids = Arrays.stream(parts)
                                .filter(s -> s != null && !s.isEmpty())
                                .map(s -> {
                                    try { return Integer.parseInt(s); } catch (Exception ex) { return null; }
                                })
                                .filter(Objects::nonNull).collect(Collectors.toList());
                        if (!ids.isEmpty()) {
                            items = projectItemMapper.selectBatchIds(ids);
                            Map<Integer, ProjectItem> map = items.stream().collect(Collectors.toMap(ProjectItem::getId, pi -> pi));
                            List<ProjectItem> ordered = new ArrayList<>();
                            for (Integer iid : ids) {
                                if (map.containsKey(iid)) ordered.add(map.get(iid));
                            }
                            items = ordered;
                        }
                    }
                }
            }

            List<Map<String, Object>> itemDtos = items.stream().map(pi -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", pi.getId());
                m.put("name", pi.getName());
                m.put("info", pi.getInfo());
                m.put("point", pi.getPoint());
                m.put("pic", pi.getPic());
                return m;
            }).collect(Collectors.toList());
            data.put("items", itemDtos);

            // order_details
            List<OrderDetails> details = orderDetailsMapper.selectList(new QueryWrapper<OrderDetails>().eq("main_id", main.getId()).eq("isdel", 0));
            List<Map<String, Object>> detailsOut = details.stream().map(d -> {
                Map<String, Object> mm = new HashMap<>();
                mm.put("id", d.getId());
                mm.put("mainId", d.getMainId());
                mm.put("date", d.getDate());
                mm.put("info", d.getInfo());
                mm.put("itemId", d.getItemId());
                mm.put("status", d.getStatus());
                mm.put("userId", d.getUserId());
                mm.put("isdel", d.getIsdel());
                mm.put("comment", d.getComment());
                return mm;
            }).collect(Collectors.toList());
            data.put("details", detailsOut);

            data.put("item_name", (projectMapper.selectById(main.getProjectId()) != null) ? projectMapper.selectById(main.getProjectId()).getName() : "");
            data.put("total", null);
            data.put("status", "paid");
            data.put("comment", main.getInfo());

            resp.put("code", 0);
            resp.put("msg", "ok");
            resp.put("data", data);
            return resp;
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.put("code", 500);
            resp.put("msg", "服务器错误：" + ex.getMessage());
            return resp;
        }
    }

    // 保存诊断（同之前实现）
    public static class SaveOrderRequest {
        public Integer mainId;
        public String info;
        public List<DetailUpdate> details;
        public static class DetailUpdate {
            public Integer id;
            public Integer itemId;
            public String comment;
        }
    }

    @PostMapping("/{id}/save")
    @Transactional
    public Map<String, Object> saveOrder(@PathVariable("id") Integer id, @RequestBody SaveOrderRequest body) {
        Map<String, Object> resp = new HashMap<>();
        if (body == null || body.mainId == null) {
            resp.put("code", 1);
            resp.put("msg", "参数错误：缺少 mainId");
            return resp;
        }
        try {
            OrderMain main = orderMainMapper.selectById(body.mainId);
            if (main == null) {
                resp.put("code", 2);
                resp.put("msg", "主订单不存在");
                return resp;
            }
            if (body.info != null) {
                main.setInfo(body.info);
                orderMainMapper.updateById(main);
            }

            if (body.details != null && !body.details.isEmpty()) {
                List<OrderDetails> existList = orderDetailsMapper.selectList(new QueryWrapper<OrderDetails>().eq("main_id", main.getId()).eq("isdel", 0));
                Map<Integer, OrderDetails> existById = existList.stream().filter(od -> od.getId() != null).collect(Collectors.toMap(OrderDetails::getId, od -> od));
                Map<Integer, OrderDetails> existByItem = existList.stream().filter(od -> od.getItemId() != null).collect(Collectors.toMap(OrderDetails::getItemId, od -> od, (a,b)->a));

                for (SaveOrderRequest.DetailUpdate du : body.details) {
                    if (du == null) continue;
                    String comment = du.comment;
                    if (du.id != null) {
                        OrderDetails od = existById.get(du.id);
                        if (od != null) {
                            od.setComment(comment);
                            orderDetailsMapper.updateById(od);
                            continue;
                        }
                    }
                    if (du.itemId != null) {
                        OrderDetails odByItem = existByItem.get(du.itemId);
                        if (odByItem != null) {
                            odByItem.setComment(comment);
                            orderDetailsMapper.updateById(odByItem);
                        } else {
                            OrderDetails newOd = new OrderDetails();
                            newOd.setMainId(main.getId());
                            newOd.setItemId(du.itemId);
                            newOd.setComment(comment);
                            newOd.setDate(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                            newOd.setIsdel(0);
                            orderDetailsMapper.insert(newOd);
                        }
                    }
                }
            }

            resp.put("code", 0);
            resp.put("msg", "保存成功");
            return resp;
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.put("code", 500);
            resp.put("msg", "保存失败：" + ex.getMessage());
            return resp;
        }
    }

    // 修改状态：从 order_main 读取 client_id，然后修改 client.status（0..3）
    @PostMapping("/{id}/status")
    @Transactional
    public Map<String, Object> changeStatus(@PathVariable("id") Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (body == null || !body.containsKey("status")) {
                resp.put("code", 1);
                resp.put("msg", "缺少 status");
                return resp;
            }
            Integer statusVal;
            try {
                statusVal = Integer.parseInt(String.valueOf(body.get("status")));
            } catch (Exception e) {
                resp.put("code", 2);
                resp.put("msg", "status 必须为整数（0..3）");
                return resp;
            }
            if (statusVal < 0 || statusVal > 3) {
                resp.put("code", 3);
                resp.put("msg", "status 值超出范围（0..3）");
                return resp;
            }

            OrderMain main = orderMainMapper.selectById(id);
            if (main == null) {
                resp.put("code", 4);
                resp.put("msg", "订单不存在");
                return resp;
            }
            Integer clientId = main.getClientId();
            if (clientId == null) {
                resp.put("code", 5);
                resp.put("msg", "订单未绑定用户（client_id 为空）");
                return resp;
            }
            Client client = clientMapper.selectById(clientId);
            if (client == null) {
                resp.put("code", 6);
                resp.put("msg", "对应用户不存在");
                return resp;
            }

            client.setStatus(statusVal);
            clientMapper.updateById(client);

            // 如果你同时需要在 order_main 表写入状态字段，请在这里设置并 update orderMainMapper.updateById(main);
            // 例如： main.setStatus(statusVal); orderMainMapper.updateById(main);

            resp.put("code", 0);
            resp.put("msg", "修改用户状态成功");
            resp.put("data", Collections.singletonMap("clientStatus", statusVal));
            return resp;
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.put("code", 500);
            resp.put("msg", "修改失败：" + ex.getMessage());
            return resp;
        }
    }

    // 删除（逻辑删除）
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable("id") Integer id) {
        Map<String, Object> resp = new HashMap<>();
        try {
            OrderMain main = orderMainMapper.selectById(id);
            if (main == null) {
                resp.put("code", 1);
                resp.put("msg", "订单不存在");
                return resp;
            }
            main.setIsdel(1);
            orderMainMapper.updateById(main);
            resp.put("code", 0);
            resp.put("msg", "删除成功");
            return resp;
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.put("code", 500);
            resp.put("msg", "删除失败：" + ex.getMessage());
            return resp;
        }
    }

    // 导出占位
    @PostMapping("/export")
    public Map<String, Object> export(@RequestParam Map<String, String> params) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "导出任务已创建（后端需实现实际导出逻辑）");
        resp.put("data", null);
        return resp;
    }
}