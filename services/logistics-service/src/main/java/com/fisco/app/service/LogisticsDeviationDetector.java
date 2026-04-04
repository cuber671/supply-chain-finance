package com.fisco.app.service;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fisco.app.entity.LogisticsDelegate;
import com.fisco.app.feign.WarehouseFeignClient;
import com.fisco.app.mapper.LogisticsDelegateMapper;

/**
 * 物流偏航检测服务
 *
 * 【P1-5修复】建立平台级物流路线知识库，实现自主偏航检测
 * 摆脱对外部GPS系统传入isDeviation标志的依赖
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class LogisticsDeviationDetector {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsDeviationDetector.class);

    // 偏航判定阈值（米）- 偏离预定路线超过此距离判定为偏航
    private static final double DEVIATION_THRESHOLD_METERS = 2000.0;

    @Autowired
    private LogisticsDelegateMapper delegateMapper;

    @Autowired(required = false)
    private WarehouseFeignClient warehouseFeignClient;

    /**
     * 偏航检测结果
     */
    public static class DeviationResult {
        private final boolean isDeviation;
        private final BigDecimal distance;
        private final int deviationLevel;

        public DeviationResult(boolean isDeviation, BigDecimal distance, int deviationLevel) {
            this.isDeviation = isDeviation;
            this.distance = distance;
            this.deviationLevel = deviationLevel;
        }

        public boolean isDeviation() {
            return isDeviation;
        }

        public BigDecimal getDistance() {
            return distance;
        }

        public int getDeviationLevel() {
            return deviationLevel;
        }
    }

    /**
     * 自主检测偏航
     * 策略：当前位置距离起运地和目的地连线的垂直距离超过阈值
     */
    public DeviationResult detectDeviation(String voucherNo, BigDecimal currentLat, BigDecimal currentLon) {
        // 1. 获取委派单信息
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            logger.warn("偏航检测失败：委派单不存在: voucherNo={}", voucherNo);
            return new DeviationResult(false, BigDecimal.ZERO, 0);
        }

        // 2. 获取起运地和目的地坐标
        BigDecimal sourceLat = null, sourceLon = null;
        BigDecimal targetLat = null, targetLon = null;

        if (delegate.getSourceWhId() != null && warehouseFeignClient != null) {
            try {
                Map<String, Object> sourceWh = warehouseFeignClient.getWarehouseById(delegate.getSourceWhId());
                if (sourceWh != null && sourceWh.get("data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) sourceWh.get("data");
                    sourceLat = extractBigDecimal(data.get("latitude"));
                    sourceLon = extractBigDecimal(data.get("longitude"));
                }
            } catch (Exception e) {
                logger.warn("获取起运地仓库坐标失败: sourceWhId={}", delegate.getSourceWhId(), e);
            }
        }

        if (delegate.getTargetWhId() != null && warehouseFeignClient != null) {
            try {
                Map<String, Object> targetWh = warehouseFeignClient.getWarehouseById(delegate.getTargetWhId());
                if (targetWh != null && targetWh.get("data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) targetWh.get("data");
                    targetLat = extractBigDecimal(data.get("latitude"));
                    targetLon = extractBigDecimal(data.get("longitude"));
                }
            } catch (Exception e) {
                logger.warn("获取目的地仓库坐标失败: targetWhId={}", delegate.getTargetWhId(), e);
            }
        }

        // 坐标不完整时，无法计算偏航
        if (sourceLat == null || sourceLon == null || targetLat == null || targetLon == null) {
            logger.warn("偏航检测跳过：仓库坐标信息不完整: voucherNo={}", voucherNo);
            return new DeviationResult(false, BigDecimal.ZERO, 0);
        }

        // 3. 计算当前位置到预定路线的垂直距离
        double distance = calculateDistanceToRoute(
            sourceLat.doubleValue(), sourceLon.doubleValue(),
            targetLat.doubleValue(), targetLon.doubleValue(),
            currentLat.doubleValue(), currentLon.doubleValue()
        );

        // 4. 判断是否偏航
        boolean isDeviation = distance > DEVIATION_THRESHOLD_METERS;
        int deviationLevel = calculateDeviationLevel(distance);

        logger.info("偏航检测结果: voucherNo={}, distance={}m, isDeviation={}, level={}",
            voucherNo, distance, isDeviation, deviationLevel);

        return new DeviationResult(isDeviation, BigDecimal.valueOf(distance), deviationLevel);
    }

    /**
     * 计算点到线段的最短距离（Haversine公式 + 向量投影）
     */
    private double calculateDistanceToRoute(double lat1, double lon1, double lat2, double lon2,
                                           double lat3, double lon3) {
        // lat1,lon1: 起运地
        // lat2,lon2: 目的地
        // lat3,lon3: 当前位置

        // 如果起运地和目的地重合，使用简单距离计算
        if (Math.abs(lat1 - lat2) < 0.0001 && Math.abs(lon1 - lon2) < 0.0001) {
            return calculateHaversineDistance(lat1, lon1, lat3, lon3);
        }

        // 计算经纬度差值（用于向量投影计算）
        double deltaLat12 = Math.toRadians(lat2 - lat1);
        double deltaLon12 = Math.toRadians(lon2 - lon1);
        double deltaLat13 = Math.toRadians(lat3 - lat1);
        double deltaLon13 = Math.toRadians(lon3 - lon1);

        // 向量A = P2 - P1
        // 向量B = P3 - P1
        // 投影比例 t = (A·B) / (A·A)
        double dotProduct = deltaLat12 * deltaLat13 + deltaLon12 * deltaLon13;
        double lengthSquared = deltaLat12 * deltaLat12 + deltaLon12 * deltaLon12;

        // 限制t在[0,1]范围内（如果是负数或大于1，取最近的端点）
        double t = Math.max(0, Math.min(1, dotProduct / lengthSquared));

        // 最近点坐标
        double closestLat = lat1 + t * (lat2 - lat1);
        double closestLon = lon1 + t * (lon2 - lon1);

        // 计算当前位置到最近点的距离
        return calculateHaversineDistance(lat3, lon3, closestLat, closestLon);
    }

    /**
     * 计算两点之间的Haversine距离
     * @return 距离（米）
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球半径（米）

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 根据偏航距离计算偏航级别
     * 1-轻度（2km以内）
     * 2-中度（2-5km）
     * 3-严重（5km以上）
     */
    private int calculateDeviationLevel(double distanceMeters) {
        if (distanceMeters >= 5000) {
            return 3;  // 严重
        }
        if (distanceMeters >= 2000) {
            return 2;  // 中度
        }
        return 1;  // 轻度
    }

    private BigDecimal extractBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}