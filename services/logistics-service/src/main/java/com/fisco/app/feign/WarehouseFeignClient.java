package com.fisco.app.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 仓单服务 Feign 客户端
 *
 * 物流服务通过此接口调用仓单服务进行仓单操作
 */
@FeignClient(name = "warehouse-service", contextId = "logisticsWarehouseClient", fallback = WarehouseFeignClientFallback.class)
public interface WarehouseFeignClient {

    @GetMapping("/api/v1/warehouse/receipt/{receiptId}")
    Map<String, Object> getReceiptById(@PathVariable("receiptId") Long receiptId);

    @GetMapping("/api/v1/warehouse/receipt/by-chain/{onChainId}")
    Map<String, Object> getReceiptByOnChainId(@PathVariable("onChainId") String onChainId);

    @PostMapping("/api/v1/warehouse/receipt/{receiptId}/lock")
    Map<String, Object> lockReceipt(@PathVariable("receiptId") Long receiptId, @RequestBody Map<String, String> params);

    @PostMapping("/api/v1/warehouse/receipt/{receiptId}/unlock")
    Map<String, Object> unlockReceipt(@PathVariable("receiptId") Long receiptId);

    @GetMapping("/api/v1/warehouse/warehouse/{warehouseId}")
    Map<String, Object> getWarehouseById(@PathVariable("warehouseId") Long warehouseId);

    @PostMapping("/api/v1/warehouse/receipt/mint")
    Map<String, Object> mintReceipt(@RequestBody Map<String, Object> params);

    @PostMapping("/api/v1/warehouse/receipt/merge")
    Map<String, Object> mergeReceipt(@RequestBody Map<String, Object> params);
}
