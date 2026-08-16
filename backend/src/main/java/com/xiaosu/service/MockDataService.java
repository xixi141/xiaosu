package com.xiaosu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaosu.tool.model.AttendanceRecord;
import com.xiaosu.tool.model.Employee;
import com.xiaosu.tool.model.OrderRecord;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 启动时加载 resources/mock/*.json（模拟内部系统数据），供工具查询 */
@Service
@Slf4j
public class MockDataService {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Employee> employees = List.of();
    private List<AttendanceRecord> attendances = List.of();
    private List<OrderRecord> orders = List.of();

    @PostConstruct
    void load() throws Exception {
        employees = load("mock/employees.json", new TypeReference<>() {
        });
        attendances = load("mock/attendance.json", new TypeReference<>() {
        });
        orders = load("mock/orders.json", new TypeReference<>() {
        });
        log.info("Mock 数据加载完成: 员工 {} 条, 考勤 {} 条, 订单 {} 条",
                employees.size(), attendances.size(), orders.size());
    }

    public Optional<Employee> employee(String id) {
        return employees.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    public List<AttendanceRecord> attendance(String empId, LocalDate from, LocalDate to) {
        return attendances.stream()
                .filter(a -> a.empId().equals(empId))
                .filter(a -> !LocalDate.parse(a.date()).isBefore(from))
                .filter(a -> !LocalDate.parse(a.date()).isAfter(to))
                .toList();
    }

    public List<OrderRecord> orders(LocalDate from, LocalDate to) {
        return orders.stream()
                .filter(o -> !LocalDate.parse(o.date()).isBefore(from))
                .filter(o -> !LocalDate.parse(o.date()).isAfter(to))
                .toList();
    }

    private <T> List<T> load(String path, TypeReference<List<T>> type) throws Exception {
        return mapper.readValue(new ClassPathResource(path).getInputStream(), type);
    }
}
