package com.xiaosu.tool;

import com.xiaosu.service.MockDataService;
import com.xiaosu.tool.model.Employee;
import com.xiaosu.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class EmployeeTool {

    private final MockDataService data;

    public EmployeeTool(MockDataService data) {
        this.data = data;
    }

    @Tool(name = "employee_info", description = "根据员工工号查询员工姓名、部门、职级、职位。参数 id 是员工工号（如 001）")
    public String getEmployee(@ToolParam(description = "员工工号，如 001") String id, ToolContext toolContext) {
        Optional<Employee> emp = data.employee(id);
        String result = emp.map(JsonUtil::toJson).orElse("未找到工号为 " + id + " 的员工");
        ToolRecorder.record(toolContext, "employee_info", JsonUtil.toJson(Map.of("id", id)), result);
        return result;
    }
}
