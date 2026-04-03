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
import com.fisco.app.entity.LogisticsDelegate;
import com.fisco.app.entity.LogisticsTrack;
import com.fisco.app.feign.WarehouseFeignClient;
import com.fisco.app.mapper.LogisticsDelegateMapper;
import com.fisco.app.mapper.LogisticsTrackMapper;
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

    @Autowired(required = false)
    private WarehouseFeignClient warehouseFeignClient;

    @Autowired(required = false)
    private BlockchainFeignClient blockchainFeignClient;

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
                logger.info("物流委派单上链成功: voucherNo={}, result={}", voucherNo, result);
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
        logger.info("转让后移库-背书关联: endorseId={}", delegate.getEndorseId());
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
                logger.warn("仓库不存在: warehouseId={}", delegate.getSourceWhId());
                return;
            }

            // 检查返回结果是否表示成功
            Object codeObj = warehouse.get("code");
            if (codeObj instanceof Integer && (Integer) codeObj != 0) {
                logger.warn("仓库查询返回错误码: warehouseId={}, code={}", delegate.getSourceWhId(), codeObj);
                return;
            }

            // 获取仓库所属企业ID (entId在data对象内)
            Object dataObj = warehouse.get("data");
            if (!(dataObj instanceof Map)) {
                logger.warn("仓库数据为空或格式不正确: warehouseId={}", delegate.getSourceWhId());
                return;
            }
            Map<?, ?> rawData = (Map<?, ?>) dataObj;
            Map<String, Object> data = new java.util.HashMap<>();
            for (Map.Entry<?, ?> entry : rawData.entrySet()) {
                if (entry.getKey() instanceof String) {
                    data.put((String) entry.getKey(), entry.getValue());
                }
            }
            if (data.isEmpty()) {
                logger.warn("仓库数据为空: warehouseId={}", delegate.getSourceWhId());
                return;
            }
            Object warehouseEntIdObj = data.get("entId");
            if (warehouseEntIdObj == null) {
                logger.warn("仓库信息缺少entId字段: warehouseId={}", delegate.getSourceWhId());
                return;
            }

            Long warehouseEntId;
            if (warehouseEntIdObj instanceof Integer) {
                warehouseEntId = ((Integer) warehouseEntIdObj).longValue();
            } else if (warehouseEntIdObj instanceof Long) {
                warehouseEntId = (Long) warehouseEntIdObj;
            } else {
                warehouseEntId = Long.parseLong(warehouseEntIdObj.toString());
            }

            // 校验仓库是否属于当前企业（仅作警告，不阻断流程）
            if (!warehouseEntId.equals(delegate.getOwnerEntId())) {
                logger.warn("仓库归属与当前企业不匹配（仅警告）: sourceWhId={}, warehouseEntId={}, ownerEntId={}",
                        delegate.getSourceWhId(), warehouseEntId, delegate.getOwnerEntId());
            } else {
                logger.debug("仓库归属校验通过: sourceWhId={}, ownerEntId={}", delegate.getSourceWhId(), delegate.getOwnerEntId());
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("仓库归属校验异常: warehouseId={}, error={}", delegate.getSourceWhId(), e.getMessage());
            // 校验异常不应阻止业务流程，但记录警告
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

        delegate.setDriverId(driverId);
        delegate.setDriverName(driverName);
        delegate.setVehicleNo(vehicleNo);
        delegate.setStatus(LogisticsDelegate.STATUS_ASSIGNED);
        delegate.setAuthCode(generateAuthCode(voucherNo, driverId));

        String qrCode = generatePickupQrCode(voucherNo, driverId, delegate.getAuthCode());
        delegate.setPickupQrCode(qrCode);

        delegateMapper.updateById(delegate);
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

        delegate.setStatus(LogisticsDelegate.STATUS_DELIVERED);
        delegateMapper.updateById(delegate);
        logger.info("到货入库申请成功: voucherNo={}, actionType={}", voucherNo, actionType);

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
        }

        trackMapper.insert(track);
        logger.info("上报物流轨迹成功: voucherNo={}, lat={}, lon={}",
            track.getVoucherNo(), track.getLatitude(), track.getLongitude());

        // L3: 偏航时记录告警到委派单备注
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

        if (delegate.getStatus() == LogisticsDelegate.STATUS_IN_TRANSIT && latestTrack != null) {
            long minutes = java.time.Duration.between(latestTrack.getEventTime(), LocalDateTime.now()).toMinutes();
            result.put("transitDurationMinutes", minutes);
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
}
