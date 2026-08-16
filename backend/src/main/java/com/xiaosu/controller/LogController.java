package com.xiaosu.controller;

import com.xiaosu.dto.LogDto;
import com.xiaosu.service.LogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public LogDto.Page list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String userId,
                            @RequestParam(required = false) String status) {
        return logService.list(page, size, userId, status);
    }

    @GetMapping("/{id}")
    public LogDto detail(@PathVariable Long id) {
        return logService.detail(id);
    }
}
