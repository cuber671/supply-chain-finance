package com.fisco.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 健康检查 Controller
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "健康检查", description = "服务健康状态检查接口")
@RestController
public class HealthController {

    @Operation(summary = "健康检查", description = "检查服务是否正常运行，返回服务健康状态。")
    @ApiResponse(responseCode = "200", description = "服务正常", content = @Content)
    @GetMapping("/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}
