package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 物流服务 Feign 客户端
 */
@FeignClient(name = "logistics-service", contextId = "logisticsFeignClient")
public interface LogisticsFeignClient {

    // FIX: Path changed from /{id} to /{voucherNo}, parameter from Long to String to match LogisticsController
    @GetMapping("/api/v1/logistics/delegate/{voucherNo}")
    Result<Object> getDelegateById(@PathVariable("voucherNo") String voucherNo);

    // NEW: Typed alias following WarehouseFeignClient pattern
    @GetMapping("/api/v1/logistics/delegate/{voucherNo}")
    Result<Object> getDelegateByVoucherNo(@PathVariable("voucherNo") String voucherNo);

    @GetMapping("/api/v1/logistics/delegate/by-receipt/{receiptId}")
    Result<Object> getDelegateByReceiptId(@PathVariable("receiptId") Long receiptId);
}
