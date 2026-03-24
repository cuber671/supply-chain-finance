package com.fisco.app.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 仓库服务 Feign 客户端
 *
 * 供金融服务调用仓单相关接口
 */
@FeignClient(name = "warehouse-service", path = "/api/v1/warehouse", fallback = WarehouseFeignClientFallback.class)
public interface WarehouseFeignClient {

    @GetMapping("/receipt/{id}")
    Object getReceiptById(@PathVariable("id") Long id);

    @GetMapping("/receipt/no/{receiptNo}")
    Object getReceiptByNo(@PathVariable("receiptNo") String receiptNo);

    @GetMapping("/receipt/status")
    Object getReceiptStatus(@RequestParam("id") Long id);

    @PostMapping("/receipt/{receiptId}/lock")
    Map<String, Object> lockReceipt(@PathVariable("receiptId") Long receiptId, @RequestBody Map<String, String> params);

    @PostMapping("/receipt/{receiptId}/unlock")
    Map<String, Object> unlockReceipt(@PathVariable("receiptId") Long receiptId);
}
