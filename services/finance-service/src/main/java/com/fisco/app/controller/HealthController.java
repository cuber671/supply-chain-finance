package com.fisco.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 健康检查 Controller
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}
