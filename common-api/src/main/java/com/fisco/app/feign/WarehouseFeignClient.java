package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 仓单服务 Feign 客户端
 */
@FeignClient(name = "warehouse-service", contextId = "warehouseFeignClient")
public interface WarehouseFeignClient {

    @GetMapping("/api/v1/warehouse/receipts/{id}")
    Result<Object> getReceiptById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/warehouse/receipts/no/{receiptNo}")
    Result<Object> getReceiptByNo(@PathVariable("receiptNo") String receiptNo);

    @GetMapping("/api/v1/warehouse/receipts/exists/{id}")
    Result<Boolean> receiptExists(@PathVariable("id") Long id);
}
