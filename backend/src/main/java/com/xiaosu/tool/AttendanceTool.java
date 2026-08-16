package com.xiaosu.tool;

import com.xiaosu.service.MockDataService;
import com.xiaosu.tool.model.AttendanceRecord;
import com.xiaosu.util.JsonUtil;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class AttendanceTool {

    private final MockDataService data;

    public AttendanceTool(MockDataService data) {
        this.data = data;
    }

    @Tool(name = "attendance_query",
            description = "查询指定员工在日期范围内的考勤记录并统计出勤天数（迟到仍算出勤，请假不算）。" +
                    "参数 empId 员工工号；from 开始日期 yyyy-MM-dd；to 结束日期 yyyy-MM-dd")
    public String getAttendance(@ToolParam(description = "员工工号") String empId,
                                @ToolParam(description = "开始日期 yyyy-MM-dd") String from,
                                @ToolParam(description = "结束日期 yyyy-MM-dd") String to,
                                ToolContext toolContext) {
        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);
        List<AttendanceRecord> records = data.attendance(empId, f, t);
        long workedDays = records.stream().filter(r -> !"请假".equals(r.status())).count();
        String result = "员工 " + empId + " 在 " + from + " 至 " + to + " 共出勤 " + workedDays
                + " 天（共 " + records.size() + " 条考勤记录）。记录: " + JsonUtil.toJson(records);
        ToolRecorder.record(toolContext, "attendance_query",
                JsonUtil.toJson(Map.of("empId", empId, "from", from, "to", to)), result);
        return result;
    }
}
