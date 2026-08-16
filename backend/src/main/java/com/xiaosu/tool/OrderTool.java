package com.xiaosu.tool;

import com.xiaosu.service.MockDataService;
import com.xiaosu.tool.model.OrderRecord;
import com.xiaosu.util.JsonUtil;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class OrderTool {

    private final MockDataService data;

    public OrderTool(MockDataService data) {
        this.data = data;
    }

    @Tool(name = "order_stats",
            description = "查询日期范围内的订单数量与销售金额汇总（已退款订单不计入有效订单）。" +
                    "参数 from 开始日期 yyyy-MM-dd；to 结束日期 yyyy-MM-dd。返回订单数与总金额")
    public String getOrders(@ToolParam(description = "开始日期 yyyy-MM-dd") String from,
                            @ToolParam(description = "结束日期 yyyy-MM-dd") String to,
                            ToolContext toolContext) {
        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);
        List<OrderRecord> orders = data.orders(f, t);
        List<OrderRecord> valid = orders.stream().filter(o -> !"已退款".equals(o.status())).toList();
        long refunds = orders.size() - valid.size();
        double total = valid.stream().mapToDouble(OrderRecord::amount).sum();
        String result = from + " 至 " + to + " 共 " + valid.size() + " 笔有效订单，销售总额 "
                + String.format("%.2f", total) + " 元" + (refunds > 0 ? "（另有 " + refunds + " 笔退款未计入）" : "")
                + "。订单: " + JsonUtil.toJson(orders);
        ToolRecorder.record(toolContext, "order_stats",
                JsonUtil.toJson(Map.of("from", from, "to", to)), result);
        return result;
    }
}
