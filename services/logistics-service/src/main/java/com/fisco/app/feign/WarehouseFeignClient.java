package com.fisco.app.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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

    @PostMapping("/api/v1/warehouse/receipt/{receiptId}/mark-wait-logistics")
    Map<String, Object> markWaitLogistics(@PathVariable("receiptId") Long receiptId, @RequestParam("voucherNo") String voucherNo);

    @PostMapping("/api/v1/warehouse/receipt/{receiptId}/clear-wait-logistics")
    Map<String, Object> clearWaitLogistics(@PathVariable("receiptId") Long receiptId);

    @PostMapping("/api/v1/warehouse/receipt/{receiptId}/set-in-transit")
    Map<String, Object> setInTransit(@PathVariable("receiptId") Long receiptId);

    @PostMapping("/api/v1/warehouse/receipt/{receiptId}/update-remark")
    Map<String, Object> updateReceiptRemark(@PathVariable("receiptId") Long receiptId, @RequestParam("remark") String remark);

    @PostMapping("/api/v1/warehouse/split/apply")
    Map<String, Object> applySplit(@RequestBody Map<String, Object> params);

    @PostMapping("/api/v1/warehouse/split-merge/{opLogId}/execute")
    Map<String, Object> executeSplitMerge(@PathVariable("opLogId") Long opLogId,
            @RequestParam("execute") Boolean execute,
            @RequestParam(value = "authorizedCarrierEntId", required = false) Long authorizedCarrierEntId);

    @PostMapping("/api/v1/warehouse/receipt/{receiptId}/void")
    Map<String, Object> voidReceipt(@PathVariable("receiptId") Long receiptId, @RequestParam("reason") String reason);

    @GetMapping("/api/v1/warehouse/warehouse/{warehouseId}")
    Map<String, Object> getWarehouseById(@PathVariable("warehouseId") Long warehouseId);

    @PostMapping("/api/v1/warehouse/receipt/mint")
    Map<String, Object> mintReceipt(@RequestBody Map<String, Object> params);

    @PostMapping("/api/v1/warehouse/receipt/mint-direct")
    Map<String, Object> mintDirectReceipt(@RequestBody Map<String, Object> params);

    @PostMapping("/api/v1/warehouse/stock-in/apply")
    Map<String, Object> applyStockIn(@RequestBody Map<String, Object> params);

    @PostMapping("/api/v1/warehouse/stock-in/apply-and-confirm")
    Map<String, Object> applyStockInAndConfirm(@RequestBody Map<String, Object> params,
            @RequestParam("actualWarehouseId") Long actualWarehouseId);

    @PostMapping("/api/v1/warehouse/stock-in/create-confirmed")
    Map<String, Object> createStockInConfirmed(@RequestBody Map<String, Object> params,
            @RequestParam("actualWarehouseId") Long actualWarehouseId);

    @PostMapping("/api/v1/warehouse/stock-order/{stockOrderId}/update-status")
    Map<String, Object> updateStockOrderStatus(@PathVariable("stockOrderId") Long stockOrderId,
            @RequestParam("status") Integer status, @RequestParam(value = "remark", required = false) String remark);

}
