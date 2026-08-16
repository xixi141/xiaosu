package com.xiaosu.controller;

import com.xiaosu.service.SettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public Map<String, Object> settings() {
        return settingService.settings();
    }

    /** 连通测试：实际调一次 embedding 服务，验证 key 有效性 */
    @PostMapping("/test-connection")
    public Map<String, Object> testConnection() {
        return settingService.testConnection();
    }
}
