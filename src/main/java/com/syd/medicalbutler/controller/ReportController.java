package com.syd.medicalbutler.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syd.medicalbutler.mapper.ClientMapper;
import com.syd.medicalbutler.mapper.OrderDetailsMapper;
import com.syd.medicalbutler.mapper.OrderMainMapper;
import com.syd.medicalbutler.mapper.ProjectItemMapper;
import com.syd.medicalbutler.mapper.ProjectMapper;
import com.syd.medicalbutler.pojo.Client;
import com.syd.medicalbutler.pojo.OrderDetails;
import com.syd.medicalbutler.pojo.OrderMain;
import com.syd.medicalbutler.pojo.Project;
import com.syd.medicalbutler.pojo.ProjectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private OrderMainMapper orderMainMapper;

    @Autowired
    private OrderDetailsMapper orderDetailsMapper;

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectItemMapper projectItemMapper;

    @GetMapping("/order-detail")
    public Map<String, Object> orderDetailList(@RequestParam("userId") Integer userId,
                                               @RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "size", required = false) Integer size) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (userId == null) {
                resp.put("code", 400);
                resp.put("msg", "缺少 userId 参数");
                resp.put("data", Collections.emptyMap());
                return resp;
            }

            // 计算总数（count）
            QueryWrapper<OrderMain> countQw = new QueryWrapper<>();
            countQw.eq("client_id", userId).eq("isdel", 0);
            int totalCount = Math.toIntExact(orderMainMapper.selectCount(countQw));

            // 构造主查询（按 id 降序）
            QueryWrapper<OrderMain> qw = new QueryWrapper<>();
            qw.eq("client_id", userId).eq("isdel", 0).orderByDesc("id");

            List<OrderMain> mains;
            int respPage = 1;
            int respSize;
            if (page != null && size != null && page > 0 && size > 0) {
                respPage = page;
                respSize = size;
                int offset = (page - 1) * size;
                // 若 totalCount 为 0，可提前返回
                if (totalCount == 0) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("total", 0);
                    data.put("page", respPage);
                    data.put("size", respSize);
                    data.put("orders", Collections.emptyList());

                    resp.put("code", 0);
                    resp.put("msg", "ok");
                    resp.put("data", data);
                    return resp;
                }

                // 使用 last 拼接 LIMIT offset,size
                qw.last(" LIMIT " + offset + "," + size);
                mains = orderMainMapper.selectList(qw);
            } else {
                // 不分页，返回全部（注意：若数据量大，建议使用分页）
                mains = orderMainMapper.selectList(qw);
                respSize = mains == null ? 0 : mains.size();
            }

            if (mains == null || mains.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("total", totalCount);
                data.put("page", respPage);
                data.put("size", respSize);
                data.put("orders", Collections.emptyList());

                resp.put("code", 0);
                resp.put("msg", "ok");
                resp.put("data", data);
                return resp;
            }

            // 收集主订单 id 列表 & projectId 列表 & clientId 列表
            List<Integer> mainIds = mains.stream().map(OrderMain::getId).collect(Collectors.toList());
            Set<Integer> projectIds = mains.stream().map(OrderMain::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Integer> clientIds = mains.stream().map(OrderMain::getClientId).filter(Objects::nonNull).collect(Collectors.toSet());

            // 1) 批量查询所有 details for these mains
            List<OrderDetails> allDetails = new ArrayList<>();
            if (!mainIds.isEmpty()) {
                allDetails = orderDetailsMapper.selectList(new QueryWrapper<OrderDetails>()
                        .in("main_id", mainIds)
                        .eq("isdel", 0));
            }

            // 将 details 按 mainId 分组
            Map<Integer, List<OrderDetails>> detailsByMain = new HashMap<>();
            if (allDetails != null) {
                detailsByMain = allDetails.stream().collect(Collectors.groupingBy(OrderDetails::getMainId));
            }

            // 2) 批量查询 projects（按 projectIds）并解析 itemIds，收集需要查询的 project_item ids
            Set<Integer> needItemIds = new LinkedHashSet<>();
            Map<Integer, Project> projectById = new HashMap<>();
            if (!projectIds.isEmpty()) {
                for (Integer pid : projectIds) {
                    if (pid == null) continue;
                    Project p = projectMapper.selectById(pid);
                    if (p != null) {
                        projectById.put(pid, p);
                        String itemIds = p.getItemIds();
                        if (itemIds != null && !itemIds.trim().isEmpty()) {
                            Arrays.stream(itemIds.split(","))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .map(Integer::valueOf)
                                    .forEach(needItemIds::add);
                        }
                    }
                }
            }

            // 3) 批量查询所有需要的 ProjectItem
            List<ProjectItem> allItems = new ArrayList<>();
            Map<Integer, ProjectItem> itemById = new HashMap<>();
            if (!needItemIds.isEmpty()) {
                allItems = projectItemMapper.selectBatchIds(new ArrayList<>(needItemIds));
                if (allItems != null) {
                    itemById = allItems.stream().collect(Collectors.toMap(ProjectItem::getId, it -> it));
                }
            }

            // 4) 查询 client(s) 信息（通常只有一个 clientId）
            Map<Integer, Client> clientById = new HashMap<>();
            if (!clientIds.isEmpty()) {
                for (Integer cid : clientIds) {
                    if (cid == null) continue;
                    Client cl = clientMapper.selectById(cid);
                    if (cl != null) clientById.put(cid, cl);
                }
            }

            // 5) 组装返回：每个主订单对应 items 列表（按项目中的 item_ids 顺序）和 details（转换为 Map 结构）
            List<Map<String, Object>> ordersOut = new ArrayList<>();
            for (OrderMain m : mains) {
                Map<String, Object> o = new HashMap<>();
                o.put("id", m.getId());
                o.put("main_id", m.getId());
                o.put("date", m.getDate());
                o.put("info", m.getInfo());
                o.put("project_id", m.getProjectId());
                o.put("client_id", m.getClientId());

                // clientName
                String clientName = "";
                if (m.getClientId() != null && clientById.containsKey(m.getClientId())) {
                    Client c = clientById.get(m.getClientId());
                    clientName = (c != null) ? c.getNike() : "";
                }
                o.put("clientName", clientName);

                // project name & items (按项目字段顺序)
                String projectName = null;
                List<Map<String, Object>> itemOut = new ArrayList<>();
                if (m.getProjectId() != null && projectById.containsKey(m.getProjectId())) {
                    Project p = projectById.get(m.getProjectId());
                    projectName = p != null ? p.getName() : null;
                    String itemIds = p.getItemIds();
                    if (itemIds != null && !itemIds.trim().isEmpty()) {
                        List<Integer> ids = Arrays.stream(itemIds.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(Integer::valueOf)
                                .collect(Collectors.toList());
                        for (Integer iid : ids) {
                            ProjectItem pit = itemById.get(iid);
                            if (pit != null) {
                                Map<String, Object> mm = new HashMap<>();
                                mm.put("id", pit.getId());
                                mm.put("name", pit.getName());
                                mm.put("info", pit.getInfo());
                                mm.put("pic", pit.getPic());
                                itemOut.add(mm);
                            }
                        }
                    }
                }
                o.put("project_name", projectName);
                o.put("items", itemOut);

                // details for this main
                List<OrderDetails> ds = detailsByMain.getOrDefault(m.getId(), Collections.emptyList());
                List<Map<String, Object>> detailOut = ds.stream().map(d -> {
                    Map<String, Object> mm = new HashMap<>();
                    mm.put("id", d.getId());
                    mm.put("mainId", d.getMainId());
                    mm.put("date", d.getDate());
                    mm.put("info", d.getInfo());
                    mm.put("itemId", d.getItemId());
                    mm.put("status", d.getStatus());
                    mm.put("comment", d.getComment());
                    return mm;
                }).collect(Collectors.toList());
                o.put("details", detailOut);

                ordersOut.add(o);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", totalCount);
            data.put("page", respPage);
            data.put("size", respSize);
            data.put("orders", ordersOut);

            resp.put("code", 0);
            resp.put("msg", "ok");
            resp.put("data", data);
            return resp;

        } catch (Exception e) {
            e.printStackTrace();
            resp.put("code", 500);
            resp.put("msg", "服务器错误：" + e.getMessage());
            return resp;
        }
    }

    /**
     * GET /report/order-detail/{mainId}
     * 返回指定主订单的详情（向后兼容之前的单条详情需求）
     */
    @GetMapping("/order-detail/{mainId}")
    public Map<String, Object> orderDetailByMain(@PathVariable("mainId") Integer mainId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (mainId == null) {
                resp.put("code", 400);
                resp.put("msg", "缺少 mainId");
                resp.put("data", Collections.emptyMap());
                return resp;
            }

            OrderMain main = orderMainMapper.selectById(mainId);
            if (main == null) {
                resp.put("code", 0);
                resp.put("msg", "未找到主订单");
                resp.put("data", Collections.emptyMap());
                return resp;
            }

            Client client = null;
            if (main.getClientId() != null) client = clientMapper.selectById(main.getClientId());
            String clientName = (client != null) ? client.getNike() : "";

            String projectName = null;
            List<ProjectItem> items = new ArrayList<>();
            if (main.getProjectId() != null) {
                Project p = projectMapper.selectById(main.getProjectId());
                if (p != null) {
                    projectName = p.getName();
                    String itemIds = p.getItemIds();
                    if (itemIds != null && !itemIds.trim().isEmpty()) {
                        List<Integer> ids = Arrays.stream(itemIds.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(Integer::valueOf)
                                .collect(Collectors.toList());
                        if (!ids.isEmpty()) {
                            items = projectItemMapper.selectBatchIds(ids);
                        }
                    }
                }
            }

            List<OrderDetails> details = orderDetailsMapper.selectList(new QueryWrapper<OrderDetails>()
                    .eq("main_id", main.getId())
                    .eq("isdel", 0));

            List<Map<String, Object>> detailOut = details.stream().map(d -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getId());
                m.put("mainId", d.getMainId());
                m.put("date", d.getDate());
                m.put("info", d.getInfo());
                m.put("itemId", d.getItemId());
                m.put("status", d.getStatus());
                m.put("comment", d.getComment());
                return m;
            }).collect(Collectors.toList());

            List<Map<String, Object>> itemOut = items.stream().map(it -> {
                Map<String, Object> mm = new HashMap<>();
                mm.put("id", it.getId());
                mm.put("name", it.getName());
                mm.put("info", it.getInfo());
                mm.put("pic", it.getPic());
                return mm;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("id", main.getId());
            data.put("main_id", main.getId());
            data.put("date", main.getDate());
            data.put("info", main.getInfo());
            data.put("project_id", main.getProjectId());
            data.put("clientName", clientName);
            data.put("project_name", projectName);
            data.put("items", itemOut);
            data.put("details", detailOut);
            data.put("total", null);
            data.put("comment", main.getInfo());

            resp.put("code", 0);
            resp.put("msg", "ok");
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("code", 500);
            resp.put("msg", "服务器错误：" + e.getMessage());
            return resp;
        }
    }

}