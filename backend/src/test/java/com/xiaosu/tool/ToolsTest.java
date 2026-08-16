package com.xiaosu.tool;

import com.xiaosu.dto.ChatResponseDto.ToolCallInfo;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ToolsTest {

    @Autowired EmployeeTool employeeTool;
    @Autowired AttendanceTool attendanceTool;
    @Autowired OrderTool orderTool;
    @Autowired DateTimeTool dateTimeTool;

    @Test
    void employeeToolFinds001() {
        String result = employeeTool.getEmployee("001", null);
        assertThat(result).contains("张三").contains("研发部");
    }

    @Test
    void attendanceCountsWorkingDays() {
        // 001 上周（8-10 至 8-14）出勤 5 天（1 天迟到仍算出勤）
        String result = attendanceTool.getAttendance("001", "2026-08-10", "2026-08-14", null);
        assertThat(result).contains("出勤 5 天");
    }

    @Test
    void orderStatsExcludesRefunds() {
        // 上周订单：已支付 17 笔、退款 3 笔（数据见 orders.json）
        String result = orderTool.getOrders("2026-08-10", "2026-08-14", null);
        assertThat(result).contains("17 笔有效订单").contains("3 笔退款未计入");
    }

    @Test
    void dateTimeToolReturnsToday() {
        String result = dateTimeTool.now(null);
        assertThat(result).contains(String.valueOf(LocalDate.now().getYear()));
    }

    @Test
    void toolsRecordToRecorderViaToolContext() {
        // ToolRecorder 通过 ToolContext 传递（模拟 Spring AI 工具执行时注入）
        ToolRecorder recorder = new ToolRecorder();
        ToolContext toolContext = new ToolContext(Map.of(ToolRecorder.TOOL_CONTEXT_KEY, recorder));
        employeeTool.getEmployee("001", toolContext);
        List<ToolCallInfo> calls = recorder.calls();
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).name()).isEqualTo("employee_info");
    }
}
