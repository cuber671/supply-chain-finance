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

    // 根据凭证号查询委派单
    @GetMapping("/api/v1/logistics/delegate/{voucherNo}")
    Result<Object> getDelegateByVoucherNo(@PathVariable("voucherNo") String voucherNo);

    // 根据物流单ID查询委派单（解决 FinanceServiceImpl 传入数字 ID 的问题）
    @GetMapping("/api/v1/logistics/delegate/by-id/{id}")
    Result<Object> getDelegateById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/logistics/delegate/by-receipt/{receiptId}")
    Result<Object> getDelegateByReceiptId(@PathVariable("receiptId") Long receiptId);
}
