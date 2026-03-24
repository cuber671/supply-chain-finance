package com.fisco.app.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fisco.app.entity.LogisticsDelegate;
import com.fisco.app.entity.LogisticsTrack;

/**
 * 物流服务接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface LogisticsService {

    // ==================== 委派单操作 ====================

    LogisticsDelegate createDelegate(LogisticsDelegate delegate);

    LogisticsDelegate getDelegateById(Long id);

    LogisticsDelegate getDelegateByVoucherNo(String voucherNo);

    List<LogisticsDelegate> listByOwnerEntId(Long ownerEntId);

    List<LogisticsDelegate> listByCarrierEntId(Long carrierEntId);

    // 优化：单次查询，支持可选过滤条件
    List<LogisticsDelegate> listByEntIdWithFilters(Long entId, Integer businessScene, Integer status);

    LogisticsDelegate assignDriver(String voucherNo, String driverId, String driverName, String vehicleNo);

    LogisticsDelegate confirmPickup(String voucherNo, String authCode);

    LogisticsDelegate confirmPickup(String voucherNo, String authCode,
                                   BigDecimal driverLatitude, BigDecimal driverLongitude);

    LogisticsDelegate arrive(String voucherNo, Integer actionType, Long targetReceiptId);

    LogisticsDelegate updateStatus(String voucherNo, Integer status);

    // ==================== 轨迹操作 ====================

    LogisticsTrack reportTrack(LogisticsTrack track);

    List<LogisticsTrack> listTracks(String voucherNo);

    LogisticsTrack getLatestTrack(String voucherNo);

    List<LogisticsTrack> listDeviations(String voucherNo);

    // ==================== 物流追踪 ====================

    Map<String, Object> trackLogistics(String voucherNo);

    LogisticsDelegate confirmDelivery(String voucherNo, Integer action, String targetReceiptId);

    LogisticsDelegate invalidate(String voucherNo);

    boolean validateDelegate(String voucherNo);
}
