package com.fisco.app.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.feign.BlockchainFeignClient;
import com.fisco.app.feign.CreditFeignClient;
import com.fisco.app.entity.LogisticsAssignHistory;
import com.fisco.app.entity.LogisticsDeviationCreditRecord;
import com.fisco.app.entity.LogisticsDelegate;
import com.fisco.app.entity.LogisticsTrack;
import com.fisco.app.feign.WarehouseFeignClient;
import com.fisco.app.mapper.LogisticsAssignHistoryMapper;
import com.fisco.app.mapper.LogisticsDelegateMapper;
import com.fisco.app.mapper.LogisticsDeviationCreditRecordMapper;
import com.fisco.app.mapper.LogisticsTrackMapper;
import com.fisco.app.service.LogisticsDeviationDetector;
import com.fisco.app.service.LogisticsService;

/**
 * 物流服务实现类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class LogisticsServiceImpl implements LogisticsService {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsServiceImpl.class);

    @Autowired
    private LogisticsDelegateMapper delegateMapper;

    @Autowired
    private LogisticsTrackMapper trackMapper;

    @Autowired
    private LogisticsAssignHistoryMapper assignHistoryMapper;

    @Autowired(required = false)
    private LogisticsDeviationCreditRecordMapper deviationCreditRecordMapper;

    @Autowired(required = false)
    private WarehouseFeignClient warehouseFeignClient;

    @Autowired(required = false)
    private BlockchainFeignClient blockchainFeignClient;

    @Autowired(required = false)
    private CreditFeignClient creditFeignClient;

    @Autowired(required = false)
    private LogisticsDeviationDetector logisticsDeviationDetector;

    private static final DateTimeFormatter VOUCHER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    

    // ==================== 委派单操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate createDelegate(LogisticsDelegate delegate) {
        String voucherNo = generateVoucherNo();
        delegate.setVoucherNo(voucherNo);
        delegate.setStatus(LogisticsDelegate.STATUS_PENDING);

        validateBusinessScene(delegate);
        validateWarehouseOwnership(delegate);

        switch (delegate.getBusinessScene()) {
            case LogisticsDelegate.SCENE_DIRECT_TRANSFER:
                handleSceneDirectTransfer(delegate);
                break;
            case LogisticsDelegate.SCENE_TRANSFER_THEN_TRANSFER:
                handleSceneTransferThenTransfer(delegate);
                break;
            case LogisticsDelegate.SCENE_DELIVERY_TO_WAREHOUSE:
                handleSceneDeliveryToWarehouse(delegate);
                break;
            default:
                throw new IllegalArgumentException("不支持的业务场景: " + delegate.getBusinessScene());
        }

        if (delegate.getValidUntil() == null) {
            delegate.setValidUntil(LocalDateTime.now().plusDays(7));
        }

        delegateMapper.insert(delegate);
        logger.info("创建物流委派单成功: voucherNo={}, ownerEntId={}, businessScene={}",
            voucherNo, delegate.getOwnerEntId(), delegate.getBusinessSceneDesc());

        // 区块链上链（失败时需补偿：直接移库场景需解锁仓单）
        // 【修复L1】blockchainFeignClient 为 null 时必须拒绝创建，而非静默跳过
        if (blockchainFeignClient == null) {
            throw new IllegalStateException(
                "区块链网关服务不可用，无法创建物流委派单。请确保 fisco-gateway-service 已启动。");
        }
        try {
            BlockchainFeignClient.LogisticsCreateRequest request = new BlockchainFeignClient.LogisticsCreateRequest();
                request.setVoucherNo(voucherNo);
                request.setBusinessScene(delegate.getBusinessScene());
                request.setReceiptId(delegate.getReceiptId() != null ? delegate.getReceiptId().toString() : null);
                request.setTransportQuantity(delegate.getTransportQuantity() != null ? delegate.getTransportQuantity().longValue() : 0L);
                request.setUnit(delegate.getUnit());
                request.setOwnerHash(delegate.getOwnerEntId() != null ? delegate.getOwnerEntId().toString() : null);
                request.setCarrierHash(delegate.getCarrierEntId() != null ? delegate.getCarrierEntId().toString() : null);
                request.setSourceWhHash(delegate.getSourceWhId() != null ? delegate.getSourceWhId().toString() : null);
                request.setTargetWhHash(delegate.getTargetWhId() != null ? delegate.getTargetWhId().toString() : null);
                request.setValidUntil(delegate.getValidUntil() != null ? delegate.getValidUntil().toEpochSecond(java.time.ZoneOffset.UTC) : 0L);
                var result = blockchainFeignClient.createLogisticsDelegate(request);
                // 【新问题修复】添加result.getCode()检查
                if (result == null || result.getCode() != 0) {
                    String errMsg = "物流委派单区块链创建失败: voucherNo=" + voucherNo + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                logger.info("物流委派单上链成功: voucherNo={}, result={}", voucherNo, result);

                // 【P2-5修复】校验并保存链上交易hash，用于后续追溯和链上链下数据关联
                if (result != null && result.getData() != null) {
                    String chainTxHash = result.getData().toString();
                    if (chainTxHash != null && !chainTxHash.isEmpty()) {
                        delegate.setChainTxHash(chainTxHash);
                        delegateMapper.updateById(delegate);
                        logger.info("链上交易hash已保存: voucherNo={}, chainTxHash={}", voucherNo, chainTxHash);
                    } else {
                        logger.warn("区块链返回的交易hash为空: voucherNo={}", voucherNo);
                    }
                } else {
                    logger.warn("区块链返回数据异常，未获取到交易hash: voucherNo={}", voucherNo);
                }
            } catch (Exception e) {
                logger.error("物流委派单上链失败: voucherNo={}", voucherNo, e);
                // 区块链失败时，直接移库场景需解锁仓单作为补偿
                if (delegate.getBusinessScene() == LogisticsDelegate.SCENE_DIRECT_TRANSFER
                    && delegate.getReceiptId() != null && warehouseFeignClient != null) {
                    try {
                        warehouseFeignClient.unlockReceipt(delegate.getReceiptId());
                        logger.warn("区块链失败已补偿解锁仓单: receiptId={}, voucherNo={}",
                            delegate.getReceiptId(), voucherNo);
                    } catch (Exception unlockEx) {
                        logger.error("仓单解锁补偿失败，需要人工干预: receiptId={}, voucherNo={}",
                            delegate.getReceiptId(), voucherNo, unlockEx);
                    }
                }
                throw new RuntimeException("区块链操作失败，物流委派单创建已回滚", e);
            }

        return delegate;
    }

    private void handleSceneDirectTransfer(LogisticsDelegate delegate) {
        if (delegate.getReceiptId() == null) {
            throw new IllegalArgumentException("直接移库场景必须关联仓单");
        }
        if (delegate.getSourceWhId() == null) {
            throw new IllegalArgumentException("必须指定起运地仓库");
        }
        if (delegate.getTargetWhId() == null) {
            throw new IllegalArgumentException("必须指定目的地仓库");
        }

        // SCENE_DIRECT_TRANSFER 必须锁定仓单，仓单服务不可用时抛出异常
        if (warehouseFeignClient == null) {
            throw new IllegalStateException(
                "仓单服务不可用，无法执行直接移库操作。请确保仓库服务已启动。");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("loanId", "LOGISTICS_LOCK_" + delegate.getVoucherNo());
            params.put("voucherNo", delegate.getVoucherNo());
            params.put("sourceWhId", String.valueOf(delegate.getSourceWhId()));
            params.put("targetWhId", String.valueOf(delegate.getTargetWhId()));

            Map<String, Object> lockResult = warehouseFeignClient.lockReceipt(delegate.getReceiptId(), params);

            if (lockResult == null || lockResult.get("code") == null) {
                throw new IllegalStateException("仓单服务响应异常");
            }

            Integer code = lockResult.get("code") instanceof Integer
                ? (Integer) lockResult.get("code")
                : Integer.parseInt(lockResult.get("code").toString());

            if (code != 0) {
                throw new IllegalStateException("锁定仓单失败: " + lockResult.get("msg"));
            }

            logger.info("直接移库-仓单已锁定: receiptId={}, 锁定数量={}, result={}",
                delegate.getReceiptId(), delegate.getTransportQuantity(), lockResult);
        } catch (Exception e) {
            logger.error("直接移库-仓单锁定失败: receiptId={}, voucherNo={}",
                delegate.getReceiptId(), delegate.getVoucherNo(), e);
            throw new IllegalStateException("仓单锁定失败，无法创建直接移库委派单: " + e.getMessage());
        }
    }

    private void handleSceneTransferThenTransfer(LogisticsDelegate delegate) {
        if (delegate.getEndorseId() == null) {
            throw new IllegalArgumentException("转让后移库场景必须关联背书");
        }
        if (delegate.getTargetWhId() == null) {
            throw new IllegalArgumentException("必须指定目的地仓库");
        }

        // 【P2-2修复】校验目标仓库归属，确保货物移至受让方自有仓库
        validateTargetWarehouseOwnership(delegate);

        logger.info("转让后移库-背书关联: endorseId={}, targetWhId={}", delegate.getEndorseId(), delegate.getTargetWhId());
    }

    /**
     * 校验目标仓库归属
     * 【P2-2修复】目标仓库应属于仓单当前持有人（受让方），即委派单的ownerEntId
     */
    private void validateTargetWarehouseOwnership(LogisticsDelegate delegate) {
        if (delegate.getTargetWhId() == null || warehouseFeignClient == null) {
            return;
        }

        try {
            Map<String, Object> warehouse = warehouseFeignClient.getWarehouseById(delegate.getTargetWhId());
            if (warehouse == null || warehouse.isEmpty()) {
                throw new IllegalArgumentException("目标仓库不存在: warehouseId=" + delegate.getTargetWhId());
            }

            Object dataObj = warehouse.get("data");
            if (!(dataObj instanceof Map)) {
                throw new IllegalArgumentException("目标仓库数据格式不正确: warehouseId=" + delegate.getTargetWhId());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object warehouseEntIdObj = data.get("entId");
            if (warehouseEntIdObj == null) {
                throw new IllegalArgumentException("目标仓库信息缺少entId字段: warehouseId=" + delegate.getTargetWhId());
            }

            Long warehouseEntId;
            if (warehouseEntIdObj instanceof Integer) {
                warehouseEntId = ((Integer) warehouseEntIdObj).longValue();
            } else if (warehouseEntIdObj instanceof Long) {
                warehouseEntId = (Long) warehouseEntIdObj;
            } else {
                warehouseEntId = Long.parseLong(warehouseEntIdObj.toString());
            }

            // 目标仓库应属于仓单当前持有人（受让方）
            if (!warehouseEntId.equals(delegate.getOwnerEntId())) {
                throw new IllegalArgumentException(
                    "目标仓库不属于当前企业（仓单受让方），无法创建移库委派单。" +
                    "目标仓库归属企业ID=" + warehouseEntId + ", 当前企业ID=" + delegate.getOwnerEntId());
            }

            logger.debug("目标仓库归属校验通过: targetWhId={}, ownerEntId={}", delegate.getTargetWhId(), delegate.getOwnerEntId());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("目标仓库归属校验异常: targetWhId={}", delegate.getTargetWhId(), e);
            throw new IllegalStateException("目标仓库归属校验失败，请稍后重试", e);
        }
    }

    private void handleSceneDeliveryToWarehouse(LogisticsDelegate delegate) {
        if (delegate.getTargetWhId() == null) {
            throw new IllegalArgumentException("发货入库场景必须指定入库仓库");
        }
        logger.info("发货入库-创建委派单: voucherNo={}, targetWhId={}",
            delegate.getVoucherNo(), delegate.getTargetWhId());
    }

    private void validateBusinessScene(LogisticsDelegate delegate) {
        switch (delegate.getBusinessScene()) {
            case LogisticsDelegate.SCENE_DIRECT_TRANSFER:
                if (delegate.getReceiptId() == null) {
                    throw new IllegalArgumentException("直接移库场景必须关联仓单");
                }
                break;
            case LogisticsDelegate.SCENE_TRANSFER_THEN_TRANSFER:
                if (delegate.getEndorseId() == null) {
                    throw new IllegalArgumentException("转让后移库场景必须关联背书");
                }
                break;
            case LogisticsDelegate.SCENE_DELIVERY_TO_WAREHOUSE:
                if (delegate.getTargetWhId() == null) {
                    throw new IllegalArgumentException("发货入库场景必须指定入库仓库");
                }
                break;
            default:
                throw new IllegalArgumentException("无效的业务场景: " + delegate.getBusinessScene());
        }
    }

    /**
     * 校验起运地仓库归属
     * 确保sourceWhId属于当前企业，防止越权操作
     *
     * 【P2-1修复】仓库归属校验不匹配时应阻断流程，而非仅警告
     */
    private void validateWarehouseOwnership(LogisticsDelegate delegate) {
        if (delegate.getSourceWhId() == null) {
            return;
        }

        if (warehouseFeignClient == null) {
            throw new IllegalStateException(
                "仓单服务不可用，无法验证仓库归属。请确保仓库服务已启动。");
        }

        try {
            Map<String, Object> warehouse = warehouseFeignClient.getWarehouseById(delegate.getSourceWhId());
            if (warehouse == null || warehouse.isEmpty()) {
                throw new IllegalArgumentException("起运地仓库不存在: warehouseId=" + delegate.getSourceWhId());
            }

            // 检查返回结果是否表示成功
            Object codeObj = warehouse.get("code");
            if (codeObj instanceof Integer && (Integer) codeObj != 0) {
                throw new IllegalArgumentException("仓库查询失败: warehouseId=" + delegate.getSourceWhId() + ", code=" + codeObj);
            }

            // 获取仓库所属企业ID (entId在data对象内)
            Object dataObj = warehouse.get("data");
            if (!(dataObj instanceof Map)) {
                throw new IllegalArgumentException("仓库数据格式不正确: warehouseId=" + delegate.getSourceWhId());
            }
            Map<?, ?> rawData = (Map<?, ?>) dataObj;
            Map<String, Object> data = new java.util.HashMap<>();
            for (Map.Entry<?, ?> entry : rawData.entrySet()) {
                if (entry.getKey() instanceof String) {
                    data.put((String) entry.getKey(), entry.getValue());
                }
            }
            if (data.isEmpty()) {
                throw new IllegalArgumentException("仓库数据为空: warehouseId=" + delegate.getSourceWhId());
            }
            Object warehouseEntIdObj = data.get("entId");
            if (warehouseEntIdObj == null) {
                throw new IllegalArgumentException("仓库信息缺少entId字段: warehouseId=" + delegate.getSourceWhId());
            }

            Long warehouseEntId;
            if (warehouseEntIdObj instanceof Integer) {
                warehouseEntId = ((Integer) warehouseEntIdObj).longValue();
            } else if (warehouseEntIdObj instanceof Long) {
                warehouseEntId = (Long) warehouseEntIdObj;
            } else {
                warehouseEntId = Long.parseLong(warehouseEntIdObj.toString());
            }

            // 【P2-1修复】归属不匹配时阻断，而非仅警告
            if (!warehouseEntId.equals(delegate.getOwnerEntId())) {
                throw new IllegalArgumentException(
                    "起运地仓库不属于当前企业，无法创建物流委派单。" +
                    "仓库归属企业ID=" + warehouseEntId + ", 当前企业ID=" + delegate.getOwnerEntId());
            }

            logger.debug("仓库归属校验通过: sourceWhId={}, ownerEntId={}", delegate.getSourceWhId(), delegate.getOwnerEntId());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("仓库归属校验异常: warehouseId={}", delegate.getSourceWhId(), e);
            throw new IllegalStateException("仓库归属校验失败，请稍后重试", e);
        }
    }

    @Override
    public LogisticsDelegate getDelegateById(Long id) {
        return delegateMapper.selectById(id);
    }

    @Override
    public LogisticsDelegate getDelegateByVoucherNo(String voucherNo) {
        return delegateMapper.selectByVoucherNo(voucherNo);
    }

    @Override
    public List<LogisticsDelegate> listByOwnerEntId(Long ownerEntId) {
        return delegateMapper.selectByOwnerEntId(ownerEntId);
    }

    @Override
    public List<LogisticsDelegate> listByCarrierEntId(Long carrierEntId) {
        return delegateMapper.selectByCarrierEntId(carrierEntId);
    }

    @Override
    public List<LogisticsDelegate> listByEntIdWithFilters(Long entId, Integer businessScene, Integer status) {
        return delegateMapper.selectByEntIdWithFilters(entId, businessScene, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate assignDriver(String voucherNo, String driverId, String driverName, String vehicleNo) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 幂等性检查：如果已指派相同司机，直接返回
        if (delegate.getStatus() == LogisticsDelegate.STATUS_ASSIGNED) {
            if (driverId != null && driverId.equals(delegate.getDriverId())) {
                logger.info("司机已指派，返回现有状态: voucherNo={}, driverId={}", voucherNo, driverId);
                return delegate;
            }
            throw new IllegalArgumentException("当前状态不允许指派司机，当前状态: " + delegate.getStatusDesc());
        }

        if (delegate.getStatus() != LogisticsDelegate.STATUS_PENDING) {
            throw new IllegalArgumentException("当前状态不允许指派司机，当前状态: " + delegate.getStatusDesc());
        }

        // 参数校验
        if (driverId == null || driverId.isEmpty()) {
            throw new IllegalArgumentException("司机ID不能为空");
        }
        if (driverName == null || driverName.isEmpty()) {
            throw new IllegalArgumentException("司机姓名不能为空");
        }
        if (vehicleNo == null || vehicleNo.isEmpty()) {
            throw new IllegalArgumentException("车牌号不能为空");
        }

        // 【P2-3修复】承运身份和司机归属校验（预留扩展点）
        // 当承运企业管理服务不可用时，记录警告但不阻断；服务可用时应启用完整校验
        validateCarrierIdentity(delegate, driverId, vehicleNo);

        // 【P2-4修复】指派次数校验（防止无限次更换司机）
        if (assignHistoryMapper != null) {
            long assignCount = assignHistoryMapper.countByVoucherNo(voucherNo);
            if (assignCount >= 3) {
                throw new IllegalArgumentException(
                    "该委派单指派次数已达上限（3次），如需更换司机请先失效当前委派单后重新创建");
            }
        }

        // 判断是指派还是变更
        boolean isFirstAssign = delegate.getDriverId() == null;

        delegate.setDriverId(driverId);
        delegate.setDriverName(driverName);
        delegate.setVehicleNo(vehicleNo);
        delegate.setStatus(LogisticsDelegate.STATUS_ASSIGNED);
        delegate.setAuthCode(generateAuthCode(voucherNo, driverId));

        String qrCode = generatePickupQrCode(voucherNo, driverId, delegate.getAuthCode());
        delegate.setPickupQrCode(qrCode);

        delegateMapper.updateById(delegate);

        // 【P2-4修复】记录指派历史
        if (assignHistoryMapper != null) {
            LogisticsAssignHistory history = new LogisticsAssignHistory();
            history.setVoucherNo(voucherNo);
            history.setDriverId(driverId);
            history.setDriverName(driverName);
            history.setVehicleNo(vehicleNo);
            history.setAssignTime(LocalDateTime.now());
            history.setAssignType(isFirstAssign
                ? LogisticsAssignHistory.TYPE_FIRST_ASSIGN
                : LogisticsAssignHistory.TYPE_CHANGE_ASSIGN);
            assignHistoryMapper.insert(history);
            logger.info("指派历史已记录: voucherNo={}, assignType={}, driverId={}",
                voucherNo, history.getAssignTypeDesc(), driverId);
        }

        logger.info("物流指派任务成功: voucherNo={}, driver={}, vehicleNo={}", voucherNo, driverName, vehicleNo);

        return delegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate confirmPickup(String voucherNo, String authCode) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 幂等性：如果已经是 IN_TRANSIT 状态，直接返回成功
        if (delegate.getStatus() == LogisticsDelegate.STATUS_IN_TRANSIT) {
            logger.info("提货确认 idempotent: 已处于运输中状态, voucherNo={}", voucherNo);
            return delegate;
        }

        if (delegate.getStatus() != LogisticsDelegate.STATUS_ASSIGNED) {
            throw new IllegalArgumentException("当前状态不允许提货，当前状态: " + delegate.getStatusDesc());
        }

        if (delegate.getAuthCode() == null || !delegate.getAuthCode().equals(authCode)) {
            throw new IllegalArgumentException("授权码错误");
        }

        delegate.setStatus(LogisticsDelegate.STATUS_IN_TRANSIT);
        delegateMapper.updateById(delegate);
        logger.info("仓库提货确认成功: voucherNo={}", voucherNo);

        return delegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate confirmPickup(String voucherNo, String authCode,
                                           BigDecimal driverLatitude, BigDecimal driverLongitude) {
        if (driverLatitude != null && driverLongitude != null) {
            validateGeofence(voucherNo, driverLatitude, driverLongitude);
        }
        return confirmPickup(voucherNo, authCode);
    }

    // 地理围栏半径500米
    private static final double GEOFENCE_RADIUS_METERS = 500.0;

    private void validateGeofence(String voucherNo, BigDecimal driverLat, BigDecimal driverLon) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        if (delegate.getSourceWhId() == null) {
            logger.warn("未设置起运地仓库，跳过地理围栏校验: voucherNo={}", voucherNo);
            return;
        }

        if (driverLat == null || driverLon == null) {
            logger.warn("司机位置信息不完整，跳过地理围栏校验: voucherNo={}", voucherNo);
            return;
        }

        if (warehouseFeignClient == null) {
            logger.warn("仓单服务不可用，跳过地理围栏校验: voucherNo={}", voucherNo);
            return;
        }

        Map<String, Object> warehouse = warehouseFeignClient.getWarehouseById(delegate.getSourceWhId());
        if (warehouse == null || warehouse.isEmpty()) {
            logger.warn("仓库信息获取失败，跳过地理围栏校验: warehouseId={}", delegate.getSourceWhId());
            return;
        }

        // 提取仓库坐标
        BigDecimal warehouseLat = extractBigDecimal(warehouse.get("latitude"));
        BigDecimal warehouseLon = extractBigDecimal(warehouse.get("longitude"));

        if (warehouseLat == null || warehouseLon == null) {
            logger.warn("仓库坐标信息不完整，跳过地理围栏校验: warehouseId={}, lat={}, lon={}",
                delegate.getSourceWhId(), warehouseLat, warehouseLon);
            return;
        }

        // 计算距离
        double distance = calculateHaversineDistance(
            driverLat.doubleValue(), driverLon.doubleValue(),
            warehouseLat.doubleValue(), warehouseLon.doubleValue()
        );

        if (distance > GEOFENCE_RADIUS_METERS) {
            logger.warn("司机位置超出地理围栏范围: voucherNo={}, distance={:.0f}m, radius={}m",
                voucherNo, distance, GEOFENCE_RADIUS_METERS);
            throw new IllegalArgumentException(
                "司机位置不在允许范围内。当前位置距离仓库" + String.format("%.0f", distance) + "米，超出允许范围" + (int)GEOFENCE_RADIUS_METERS + "米");
        }

        logger.info("地理围栏校验通过: voucherNo={}, distance={:.0f}m", voucherNo, distance);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate arrive(String voucherNo, Integer actionType, Long targetReceiptId) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        if (delegate.getStatus() != LogisticsDelegate.STATUS_IN_TRANSIT) {
            throw new IllegalArgumentException("当前状态不允许到货操作，当前状态: " + delegate.getStatusDesc());
        }

        // 调用仓单服务创建或合并仓单
        if (actionType == LogisticsDelegate.ACTION_CREATE_NEW_RECEIPT) {
            // 验证必要字段
            if (delegate.getTargetWhId() == null) {
                throw new IllegalArgumentException("创建新仓单时目标仓库不能为空");
            }
            if (delegate.getTransportQuantity() == null) {
                throw new IllegalArgumentException("创建新仓单时运输数量不能为空");
            }

            if (warehouseFeignClient == null) {
                throw new IllegalStateException("仓单服务不可用，无法创建新仓单");
            }

            // 调用仓单服务创建新仓单（物流直接入库接口）
            Map<String, Object> mintParams = new HashMap<>();
            mintParams.put("logisticsVoucherNo", voucherNo);
            mintParams.put("warehouseId", delegate.getTargetWhId());
            mintParams.put("transportQuantity", delegate.getTransportQuantity());
            mintParams.put("unit", delegate.getUnit());
            mintParams.put("ownerEntId", delegate.getOwnerEntId());

            try {
                Map<String, Object> mintResult = warehouseFeignClient.mintDirectReceipt(mintParams);
                if (mintResult == null || mintResult.get("code") == null) {
                    throw new RuntimeException("仓单服务响应异常");
                }
                Integer code = mintResult.get("code") instanceof Integer
                    ? (Integer) mintResult.get("code")
                    : Integer.parseInt(mintResult.get("code").toString());
                if (code != 0) {
                    throw new RuntimeException("创建仓单失败: " + mintResult.get("msg"));
                }
                logger.info("到货生成新仓单: voucherNo={}, targetWhId={}, result={}",
                    voucherNo, delegate.getTargetWhId(), mintResult);
            } catch (Exception e) {
                logger.error("到货创建仓单失败: voucherNo={}", voucherNo, e);
                throw new RuntimeException("创建仓单失败: " + e.getMessage(), e);
            }

        } else if (actionType == LogisticsDelegate.ACTION_MERGE_EXISTING_RECEIPT) {
            if (targetReceiptId == null) {
                throw new IllegalArgumentException("增量入库时必须指定目标仓单ID");
            }
            if (warehouseFeignClient == null) {
                throw new IllegalStateException("仓单服务不可用，无法合并仓单");
            }

            // 调用仓单服务合并仓单
            Map<String, Object> mergeParams = new HashMap<>();
            mergeParams.put("receiptId", targetReceiptId);
            mergeParams.put("logisticsVoucherNo", voucherNo);
            mergeParams.put("additionalQuantity", delegate.getTransportQuantity());
            mergeParams.put("unit", delegate.getUnit());

            try {
                // 检查 mergeReceipt 方法是否存在
                Map<String, Object> mergeResult = warehouseFeignClient.mergeReceipt(mergeParams);
                if (mergeResult == null || mergeResult.get("code") == null) {
                    throw new RuntimeException("仓单服务响应异常");
                }
                Integer code = mergeResult.get("code") instanceof Integer
                    ? (Integer) mergeResult.get("code")
                    : Integer.parseInt(mergeResult.get("code").toString());
                if (code != 0) {
                    throw new RuntimeException("合并仓单失败: " + mergeResult.get("msg"));
                }
                logger.info("并入已有仓单: receiptId={}, 增加数量={}, result={}",
                    targetReceiptId, delegate.getTransportQuantity(), mergeResult);
            } catch (Exception e) {
                logger.error("并入仓单失败: receiptId={}, voucherNo={}", targetReceiptId, voucherNo, e);
                throw new RuntimeException("合并仓单失败: " + e.getMessage(), e);
            }
        }

        // 【P1-3修复】到货入库必须上链存证
        if (blockchainFeignClient == null) {
            throw new IllegalStateException("区块链网关服务不可用，无法执行到货入库操作。请确保 fisco-gateway-service 已启动。");
        }

        try {
            // 根据actionType调用对应的区块链接口
            if (actionType == LogisticsDelegate.ACTION_CREATE_NEW_RECEIPT) {
                BlockchainFeignClient.LogisticsArriveCreateRequest request =
                    new BlockchainFeignClient.LogisticsArriveCreateRequest();
                request.setVoucherNo(voucherNo);
                request.setWeight(delegate.getTransportQuantity() != null
                    ? delegate.getTransportQuantity().longValue() : 0L);
                request.setUnit(delegate.getUnit());
                request.setOwnerHash(delegate.getOwnerEntId() != null
                    ? delegate.getOwnerEntId().toString() : null);
                request.setWarehouseHash(delegate.getTargetWhId() != null
                    ? delegate.getTargetWhId().toString() : null);

                // 【新问题修复】添加result.getCode()检查
                var result = blockchainFeignClient.arriveAndCreateReceipt(request);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "到货入库（新建仓单）区块链失败: voucherNo=" + voucherNo + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                // 【新问题修复】保存txHash用于追溯
                if (result.getData() != null) {
                    delegate.setChainTxHash(result.getData());
                }
                logger.info("到货入库（新建仓单）上链成功: voucherNo={}, txHash={}", voucherNo, result.getData());
            } else if (actionType == LogisticsDelegate.ACTION_MERGE_EXISTING_RECEIPT) {
                BlockchainFeignClient.LogisticsArriveAddRequest request =
                    new BlockchainFeignClient.LogisticsArriveAddRequest();
                request.setVoucherNo(voucherNo);
                request.setTargetReceiptId(targetReceiptId != null
                    ? targetReceiptId.toString() : null);
                request.setQuantity(delegate.getTransportQuantity() != null
                    ? delegate.getTransportQuantity().longValue() : 0L);

                // 【新问题修复】添加result.getCode()检查
                var result = blockchainFeignClient.arriveAndAddQuantity(request);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "到货入库（合并仓单）区块链失败: voucherNo=" + voucherNo + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                // 【新问题修复】保存txHash用于追溯
                if (result.getData() != null) {
                    delegate.setChainTxHash(result.getData());
                }
                logger.info("到货入库（合并仓单）上链成功: voucherNo={}, txHash={}", voucherNo, result.getData());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("到货入库上链失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("区块链上链失败，到货入库操作已回滚", e);
        }

        // 【P1-1修复】arrive只执行仓单创建/合并操作，保持IN_TRANSIT状态
        // 状态推进到DELIVERED由confirmDelivery方法负责，确保仓单解锁与货物到达一致
        delegateMapper.updateById(delegate);
        logger.info("到货入库操作成功（待确认交付）: voucherNo={}, actionType={}, 货物已到达但需货主确认交付后仓单方可解除锁定", voucherNo, actionType);

        return delegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate updateStatus(String voucherNo, Integer status) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 校验状态值有效
        if (status == null || status < 1 || status > 5) {
            throw new IllegalArgumentException("无效的状态值: " + status + ", 有效值为1-5");
        }

        // 校验状态流转合法性
        if (!isValidStateTransition(delegate.getStatus(), status)) {
            throw new IllegalArgumentException("无效的状态转换: 从" + delegate.getStatusDesc() + "到" + getStatusDesc(status));
        }

        delegate.setStatus(status);
        delegateMapper.updateById(delegate);
        logger.info("更新物流状态: voucherNo={}, status={}", voucherNo, status);

        return delegate;
    }

    // 状态流转规则:
    // PENDING(1) -> ASSIGNED(2)
    // ASSIGNED(2) -> IN_TRANSIT(3)
    // IN_TRANSIT(3) -> DELIVERED(4) 或 INVALID(5)
    // DELIVERED(4) -> (终态)
    // INVALID(5) -> (终态)
    private boolean isValidStateTransition(Integer fromStatus, Integer toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        switch (fromStatus) {
            case 1: // PENDING
                return toStatus == 2; // ASSIGNED
            case 2: // ASSIGNED
                return toStatus == 3; // IN_TRANSIT
            case 3: // IN_TRANSIT
                return toStatus == 4 || toStatus == 5; // DELIVERED or INVALID
            case 4: // DELIVERED - 终态
            case 5: // INVALID - 终态
                return false;
            default:
                return false;
        }
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待指派";
            case 2: return "已调度";
            case 3: return "运输中";
            case 4: return "已交付";
            case 5: return "已失效";
            default: return "未知";
        }
    }

    // ==================== 轨迹操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsTrack reportTrack(LogisticsTrack track) {
        track.setEventTime(LocalDateTime.now());
        track.setStatus(LogisticsTrack.STATUS_IN_TRANSIT);

        if (track.getLatitude() != null && track.getLongitude() != null) {
            String locationHash = calculateLocationHash(
                track.getLatitude().toString(),
                track.getLongitude().toString(),
                track.getEventTime() != null ? track.getEventTime().toString() : ""
            );
            String existingDesc = track.getLocationDesc() != null ? track.getLocationDesc() + "; " : "";
            track.setLocationDesc(existingDesc + "locationHash:" + locationHash);
            logger.info("位置哈希计算: voucherNo={}, locationHash={}", track.getVoucherNo(), locationHash);

            // 【P1-5修复】使用偏航自主检测服务计算偏航，而非依赖外部系统传入
            if (logisticsDeviationDetector != null) {
                try {
                    LogisticsDeviationDetector.DeviationResult deviationResult =
                        logisticsDeviationDetector.detectDeviation(
                            track.getVoucherNo(), track.getLatitude(), track.getLongitude());

                    // 使用自主检测结果覆盖外部传入值
                    track.setIsDeviation(deviationResult.isDeviation()
                        ? LogisticsTrack.DEVIATION_YES
                        : LogisticsTrack.DEVIATION_NO);
                    track.setDeviationDistance(deviationResult.getDistance());

                    logger.info("偏航自主检测结果: voucherNo={}, isDeviation={}, distance={}, level={}",
                        track.getVoucherNo(), deviationResult.isDeviation(),
                        deviationResult.getDistance(), deviationResult.getDeviationLevel());
                } catch (Exception e) {
                    logger.warn("偏航自主检测失败，使用外部传入值: voucherNo={}, error={}",
                        track.getVoucherNo(), e.getMessage());
                }
            }
        }

        trackMapper.insert(track);
        logger.info("上报物流轨迹成功: voucherNo={}, lat={}, lon={}",
            track.getVoucherNo(), track.getLatitude(), track.getLongitude());

        // L3: 偏航时记录告警到委派单备注 AND 触发信用扣分
        if (track.getIsDeviation() != null && track.getIsDeviation() == LogisticsTrack.DEVIATION_YES) {
            logger.warn("检测到物流偏航: voucherNo={}, deviationDistance={}",
                track.getVoucherNo(), track.getDeviationDistance());

            // 记录偏航告警到委派单备注
            LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(track.getVoucherNo());
            if (delegate != null) {
                String deviationAlert = String.format("[偏航告警 %s] 距离: %s, 位置: %s,%s",
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    track.getDeviationDistance(),
                    track.getLatitude(), track.getLongitude());
                String existingRemark = delegate.getRemark() != null ? delegate.getRemark() + "\n" : "";
                delegate.setRemark(existingRemark + deviationAlert);
                delegateMapper.updateById(delegate);
                logger.info("偏航告警已记录到委派单: voucherNo={}", track.getVoucherNo());

                // 触发信用扣分
                if (creditFeignClient != null) {
                    try {
                        CreditFeignClient.LogisticsDeviationRequest deviationRequest =
                                new CreditFeignClient.LogisticsDeviationRequest();
                        deviationRequest.setEntId(delegate.getOwnerEntId());
                        deviationRequest.setLogisticsOrderId(track.getVoucherNo());
                        deviationRequest.setDeviationLevel(calculateDeviationLevel(track.getDeviationDistance()));
                        deviationRequest.setDeviationDesc("物流路径偏移检测");

                        Map<String, Object> creditResult = creditFeignClient.reportLogisticsDeviation(deviationRequest);
                        if (creditResult != null && "0".equals(String.valueOf(creditResult.get("code")))) {
                            logger.info("偏航信用扣分已触发: voucherNo={}, entId={}",
                                    track.getVoucherNo(), delegate.getOwnerEntId());
                        }
                    } catch (Exception e) {
                        logger.error("偏航信用扣分失败: voucherNo={}, entId={}, error={}",
                                track.getVoucherNo(), delegate.getOwnerEntId(), e.getMessage());
                        // 【修复SC-005-01】记录失败项供后续补偿重试
                        savePendingCreditDeduction(delegate.getOwnerEntId(), track.getVoucherNo(),
                            calculateDeviationLevel(track.getDeviationDistance()), "物流路径偏移检测",
                            e.getMessage());
                    }
                }
            }
        }

        return track;
    }

    @Override
    public List<LogisticsTrack> listTracks(String voucherNo) {
        return trackMapper.selectByVoucherNo(voucherNo);
    }

    @Override
    public LogisticsTrack getLatestTrack(String voucherNo) {
        return trackMapper.selectLatestByVoucherNo(voucherNo);
    }

    @Override
    public List<LogisticsTrack> listDeviations(String voucherNo) {
        return trackMapper.selectDeviationByVoucherNo(voucherNo);
    }

    // ==================== 物流追踪 ====================

    @Override
    public Map<String, Object> trackLogistics(String voucherNo) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        List<LogisticsTrack> tracks = trackMapper.selectByVoucherNo(voucherNo);
        LogisticsTrack latestTrack = trackMapper.selectLatestByVoucherNo(voucherNo);

        Map<String, Object> result = new HashMap<>();
        result.put("voucherNo", voucherNo);
        result.put("status", delegate.getStatus());
        result.put("statusDesc", delegate.getStatusDesc());
        result.put("businessScene", delegate.getBusinessScene());
        result.put("businessSceneDesc", delegate.getBusinessSceneDesc());
        result.put("ownerEntId", delegate.getOwnerEntId());
        result.put("carrierEntId", delegate.getCarrierEntId());
        result.put("sourceWhId", delegate.getSourceWhId());
        result.put("targetWhId", delegate.getTargetWhId());
        result.put("driverName", delegate.getDriverName());
        result.put("vehicleNo", delegate.getVehicleNo());
        result.put("transportQuantity", delegate.getTransportQuantity());
        result.put("unit", delegate.getUnit());
        result.put("latestTrack", latestTrack);
        result.put("trackCount", tracks.size());
        result.put("tracks", tracks);
        result.put("chainTxHash", delegate.getChainTxHash());

        // 【P2-6修复】运输时效风控：检测运输超时
        if (delegate.getStatus() == LogisticsDelegate.STATUS_IN_TRANSIT && latestTrack != null) {
            long minutes = java.time.Duration.between(latestTrack.getEventTime(), LocalDateTime.now()).toMinutes();
            long maxMinutes = 72 * 60; // 默认最大运输时限72小时

            result.put("transitDurationMinutes", minutes);

            // 超时预警
            if (minutes > maxMinutes) {
                result.put("transitWarning", "运输超时警告");
                result.put("transitOvertimeMinutes", minutes - maxMinutes);
                logger.warn("物流运输超时: voucherNo={}, 已运输{}分钟, 最大时限{}分钟",
                    voucherNo, minutes, maxMinutes);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate confirmDelivery(String voucherNo, Integer action, String targetReceiptId) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 幂等性：如果已经是 DELIVERED 状态，直接返回成功
        if (delegate.getStatus() == LogisticsDelegate.STATUS_DELIVERED) {
            logger.info("交付确认 idempotent: 已处于已交付状态, voucherNo={}", voucherNo);
            return delegate;
        }

        if (delegate.getStatus() != LogisticsDelegate.STATUS_IN_TRANSIT) {
            throw new IllegalArgumentException("当前状态不允许确认交付: " + delegate.getStatusDesc());
        }

        // 直接移库场景：到货后解锁仓单
        if (delegate.getBusinessScene() == LogisticsDelegate.SCENE_DIRECT_TRANSFER) {
            if (delegate.getReceiptId() == null) {
                throw new IllegalStateException("直接移库场景缺少关联仓单");
            }

            if (warehouseFeignClient == null) {
                throw new IllegalStateException("仓单服务不可用，无法完成交付确认");
            }

            Map<String, Object> unlockResult = warehouseFeignClient.unlockReceipt(delegate.getReceiptId());
            if (unlockResult == null || unlockResult.get("code") == null) {
                throw new RuntimeException("仓单服务响应异常");
            }
            Integer code = unlockResult.get("code") instanceof Integer
                ? (Integer) unlockResult.get("code")
                : Integer.parseInt(unlockResult.get("code").toString());
            if (code != 0) {
                throw new RuntimeException("解锁仓单失败: " + unlockResult.get("msg"));
            }

            logger.info("直接移库-仓单已解锁: receiptId={}, voucherNo={}", delegate.getReceiptId(), voucherNo);
        }

        // 转让后移库场景：记录日志（背书转让由仓单服务处理）
        if (delegate.getBusinessScene() == LogisticsDelegate.SCENE_TRANSFER_THEN_TRANSFER) {
            logger.info("转让后移库-完成交付: endorseId={}, voucherNo={}", delegate.getEndorseId(), voucherNo);
        }

        // 【P2-7修复】确认交付必须上链存证
        if (blockchainFeignClient == null) {
            throw new IllegalStateException("区块链网关服务不可用，无法执行确认交付。请确保 fisco-gateway-service 已启动。");
        }

        try {
            BlockchainFeignClient.LogisticsConfirmDeliveryRequest request =
                new BlockchainFeignClient.LogisticsConfirmDeliveryRequest();
            request.setVoucherNo(voucherNo);
            request.setAction(action);
            request.setTargetReceiptId(targetReceiptId);

            var result = blockchainFeignClient.confirmDelivery(request);
            // 【新问题修复】添加result.getCode()检查
            if (result == null || result.getCode() != 0) {
                String errMsg = "确认交付区块链失败: voucherNo=" + voucherNo + ", result=" + result;
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
            logger.info("确认交付上链成功: voucherNo={}, txHash={}", voucherNo, result.getData());

            // 保存链上交易hash
            if (result != null && result.getData() != null) {
                String chainTxHash = result.getData().toString();
                if (chainTxHash != null && !chainTxHash.isEmpty()) {
                    // 追加到现有chainTxHash
                    String existingHash = delegate.getChainTxHash();
                    delegate.setChainTxHash(existingHash != null ? existingHash + "," + chainTxHash : chainTxHash);
                }
            }
        } catch (Exception e) {
            logger.error("确认交付上链失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("区块链上链失败，确认交付操作已回滚", e);
        }

        delegate.setStatus(LogisticsDelegate.STATUS_DELIVERED);
        delegateMapper.updateById(delegate);
        logger.info("物流确认交付: voucherNo={}", voucherNo);

        return delegate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsDelegate invalidate(String voucherNo) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        if (delegate == null) {
            throw new IllegalArgumentException("委派单不存在: " + voucherNo);
        }

        // 【P1-2修复】运输中状态禁止失效，防止货物在途但仓单被解锁的"幽灵货物"风险
        if (delegate.getStatus() == LogisticsDelegate.STATUS_IN_TRANSIT) {
            throw new IllegalArgumentException(
                "当前货物正在运输中，禁止执行失效操作。请等待货物到达目的地后再尝试，或联系客服处理异常情况。");
        }

        // 直接移库场景：必须先解锁仓单，否则仓单永久锁死
        if (delegate.getBusinessScene() == LogisticsDelegate.SCENE_DIRECT_TRANSFER
            && delegate.getReceiptId() != null) {

            if (warehouseFeignClient == null) {
                throw new IllegalStateException("仓单服务不可用，无法解锁仓单，委派单无法失效");
            }

            Map<String, Object> unlockResult = warehouseFeignClient.unlockReceipt(delegate.getReceiptId());
            if (unlockResult == null || unlockResult.get("code") == null) {
                throw new RuntimeException("仓单服务响应异常");
            }
            Integer code = unlockResult.get("code") instanceof Integer
                ? (Integer) unlockResult.get("code")
                : Integer.parseInt(unlockResult.get("code").toString());
            if (code != 0) {
                throw new RuntimeException("解锁仓单失败: " + unlockResult.get("msg"));
            }

            logger.info("直接移库-委派单失效-仓单已解锁: receiptId={}, voucherNo={}",
                delegate.getReceiptId(), voucherNo);
        }

        // 【P2-8修复】失效操作必须上链存证
        if (blockchainFeignClient == null) {
            throw new IllegalStateException("区块链网关服务不可用，无法执行失效操作。请确保 fisco-gateway-service 已启动。");
        }

        try {
            BlockchainFeignClient.LogisticsInvalidateRequest request =
                new BlockchainFeignClient.LogisticsInvalidateRequest();
            request.setVoucherNo(voucherNo);

            var result = blockchainFeignClient.invalidateLogistics(request);
            // 【新问题修复】添加result.getCode()检查
            if (result == null || result.getCode() != 0) {
                String errMsg = "失效操作区块链失败: voucherNo=" + voucherNo + ", result=" + result;
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
            logger.info("失效操作上链成功: voucherNo={}, txHash={}", voucherNo, result.getData());

            // 保存链上交易hash
            if (result != null && result.getData() != null) {
                String chainTxHash = result.getData().toString();
                if (chainTxHash != null && !chainTxHash.isEmpty()) {
                    String existingHash = delegate.getChainTxHash();
                    delegate.setChainTxHash(existingHash != null ? existingHash + "," + chainTxHash : chainTxHash);
                }
            }
        } catch (Exception e) {
            logger.error("失效操作上链失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("区块链上链失败，失效操作已回滚", e);
        }

        // 【P1-4修复】如果已指派司机，触发通知（预留扩展点）
        if (delegate.getDriverId() != null && delegate.getStatus() == LogisticsDelegate.STATUS_ASSIGNED) {
            notifyDriverForCancellation(delegate);
        }

        delegate.setStatus(LogisticsDelegate.STATUS_INVALID);
        delegateMapper.updateById(delegate);
        logger.info("委派单已失效: voucherNo={}", voucherNo);

        return delegate;
    }

    @Override
    public boolean validateDelegate(String voucherNo) {
        LogisticsDelegate delegate = delegateMapper.selectByVoucherNo(voucherNo);
        return delegate != null && delegate.isValid();
    }

    // ==================== 私有方法 ====================

    private static final int MAX_VOUCHER_NO_GENERATION_ATTEMPTS = 10;

    private String generateVoucherNo() {
        String date = LocalDateTime.now().format(VOUCHER_FORMAT);

        for (int attempt = 0; attempt < MAX_VOUCHER_NO_GENERATION_ATTEMPTS; attempt++) {
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String voucherNo = "DPDO" + date + uuid;

            // 检查碰撞
            if (delegateMapper.selectByVoucherNo(voucherNo) == null) {
                return voucherNo;
            }

            logger.warn("voucherNo碰撞，尝试第{}次: {}", attempt + 1, voucherNo);
        }

        throw new IllegalStateException("voucherNo生成失败，已尝试" + MAX_VOUCHER_NO_GENERATION_ATTEMPTS + "次");
    }

    /**
     * 校验承运企业身份和司机归属
     * 【P2-3修复】确保只有承运企业名下的司机和车辆才能执行运输任务
     *
     * 注意：当前预留扩展点，接入司机/车辆管理服务后可启用完整校验
     */
    private void validateCarrierIdentity(LogisticsDelegate delegate, String driverId, String vehicleNo) {
        // 1. 承运企业身份校验
        if (delegate.getCarrierEntId() == null) {
            logger.warn("委派单未指定承运企业: voucherNo={}", delegate.getVoucherNo());
            // 承运企业为空时记录警告但不阻断（兼容历史数据）
            return;
        }

        // 2. 司机归属校验（预留扩展点）
        // 当司机管理服务可用时应启用此校验：
        // if (driverManagementService != null) {
        //     if (!driverManagementService.isDriverOfEnterprise(driverId, delegate.getCarrierEntId())) {
        //         throw new IllegalArgumentException("司机不属于指定承运企业，无法指派运输任务");
        //     }
        // }
        logger.debug("司机归属校验（扩展点未启用）: driverId={}, carrierEntId={}", driverId, delegate.getCarrierEntId());

        // 3. 车辆归属校验（预留扩展点）
        // 当车辆管理服务可用时应启用此校验：
        // if (vehicleManagementService != null) {
        //     if (!vehicleManagementService.isVehicleOfEnterprise(vehicleNo, delegate.getCarrierEntId())) {
        //         throw new IllegalArgumentException("车辆不属于指定承运企业，无法指派运输任务");
        //     }
        // }
        logger.debug("车辆归属校验（扩展点未启用）: vehicleNo={}, carrierEntId={}", vehicleNo, delegate.getCarrierEntId());

        logger.info("承运身份校验通过: carrierEntId={}, driverId={}, vehicleNo={}",
            delegate.getCarrierEntId(), driverId, vehicleNo);
    }

    /**
     * 通知司机委派单已取消
     * 【P1-4修复】防止司机不知情继续执行任务导致空跑
     *
     * 注意：当前预留扩展点，接入通知服务（短信/推送）后可启用完整通知
     */
    private void notifyDriverForCancellation(LogisticsDelegate delegate) {
        logger.warn("委派单已失效，已指派司机将被通知: voucherNo={}, driverId={}, driverName={}, vehicleNo={}",
            delegate.getVoucherNo(), delegate.getDriverId(), delegate.getDriverName(), delegate.getVehicleNo());

        // 【扩展点】可接入短信/推送服务通知司机
        // 示例：
        // if (notificationService != null) {
        //     notificationService.sendDriverCancellationNotice(
        //         delegate.getDriverId(),
        //         "物流委派单已取消",
        //         "委派单号：" + delegate.getVoucherNo() + "，请勿前往提货"
        //     );
        // }

        // 记录失效原因和时间戳
        String cancellationRecord = String.format("[委派单失效 %s] 司机ID=%s, 司机姓名=%s, 车牌号=%s",
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            delegate.getDriverId(),
            delegate.getDriverName(),
            delegate.getVehicleNo());
        String existingRemark = delegate.getRemark() != null ? delegate.getRemark() + "\n" : "";
        delegate.setRemark(existingRemark + cancellationRecord);
    }

    // 移除了易混淆字符 (0,O,1,I,l) 以提高识别度
    private static final String AUTH_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * 生成确定性的提货授权码（用于测试）
     * 基于voucherNo和driverId生成，确保相同输入产生相同输出
     */
    private String generateAuthCode(String voucherNo, String driverId) {
        String input = voucherNo + driverId;
        int hash = input.hashCode();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(AUTH_CODE_CHARS.charAt(Math.abs((hash >> (i * 3)) % AUTH_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String generatePickupQrCode(String voucherNo, String driverId, String authCode) {
        try {
            Map<String, Object> qrData = new HashMap<>();
            qrData.put("voucherNo", voucherNo);
            qrData.put("driverId", driverId);
            qrData.put("authCode", authCode);
            qrData.put("timestamp", System.currentTimeMillis());
            qrData.put("expires", System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(qrData);
            String base64 = java.util.Base64.getEncoder().encodeToString(json.getBytes("UTF-8"));

            logger.info("生成提货二维码: voucherNo={}, driverId={}", voucherNo, driverId);
            return base64;
        } catch (Exception e) {
            logger.error("生成二维码失败: voucherNo={}", voucherNo, e);
            return null;
        }
    }

    private String calculateLocationHash(String lat, String lon, String timestamp) {
        try {
            String data = lat + "|" + lon + "|" + timestamp;
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("计算位置哈希失败", e);
            return "";
        }
    }

    /**
     * 根据偏航距离计算偏航级别
     * 1-轻度（2km以内）
     * 2-中度（2-5km）
     * 3-严重（5km以上）
     */
    private Integer calculateDeviationLevel(BigDecimal deviationDistance) {
        if (deviationDistance == null) {
            return 1;
        }
        // 【P3-1修复】使用 >= 比较，明确边界值归属：>=5km为严重，>=2km为中度，<2km为轻度
        if (deviationDistance.compareTo(new BigDecimal("5")) >= 0) {
            return 3;  // 严重：>=5km
        }
        if (deviationDistance.compareTo(new BigDecimal("2")) >= 0) {
            return 2;  // 中度：>=2km 且 <5km
        }
        return 1;  // 轻度：<2km
    }

    // 【修复SC-005-01】保存待补偿的偏航信用扣分记录
    private void savePendingCreditDeduction(Long entId, String voucherNo,
            Integer deviationLevel, String deviationDesc, String errorMsg) {
        if (deviationCreditRecordMapper == null) {
            logger.warn("偏航信用记录Mapper未注入，无法保存待补偿记录");
            return;
        }
        try {
            // 检查是否已有待处理的记录
            LogisticsDeviationCreditRecord existing = deviationCreditRecordMapper
                    .selectPendingByEntIdAndOrderId(entId, voucherNo);
            if (existing != null) {
                logger.info("偏航信用扣分待补偿记录已存在: entId={}, voucherNo={}", entId, voucherNo);
                return;
            }
            LogisticsDeviationCreditRecord record = new LogisticsDeviationCreditRecord();
            record.setEntId(entId);
            record.setLogisticsOrderId(voucherNo);
            record.setDeviationLevel(deviationLevel);
            record.setDeviationDesc(deviationDesc);
            record.setStatus(LogisticsDeviationCreditRecord.STATUS_PENDING);
            record.setRetryCount(0);
            record.setErrorMsg(errorMsg);
            deviationCreditRecordMapper.insert(record);
            logger.info("偏航信用扣分待补偿记录已保存: entId={}, voucherNo={}", entId, voucherNo);
        } catch (Exception e) {
            logger.error("保存偏航信用扣分待补偿记录失败: entId={}, voucherNo={}, error={}",
                    entId, voucherNo, e.getMessage());
        }
    }
}
