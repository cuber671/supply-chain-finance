package com.fisco.app.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fisco.app.entity.LogisticsDelegate;
import com.fisco.app.entity.LogisticsTrack;

/**
 * 物流服务接口
 *
 * 提供物流委派单的全生命周期管理，包括：
 * - 委派单创建、状态流转
 * - 司机指派、提货确认、物流轨迹追踪
 * - 交货确认、作废等操作
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface LogisticsService {

    // ==================== 委派单操作 ====================

    /**
     * 创建物流委派单
     *
     * 支持三种业务场景：
     * - SCENE_DIRECT_TRANSFER: 直接移库（仓单从A仓库移到B仓库）
     * - SCENE_TRANSFER_THEN_TRANSFER: 先仓单入库再移库
     * - SCENE_DELIVERY_TO_WAREHOUSE: 送货到仓库
     *
     * @param delegate 委派单信息
     * @return 创建后的委派单（含生成的voucherNo）
     * @throws IllegalArgumentException 参数不合法或仓库不属于当前企业
     */
    LogisticsDelegate createDelegate(LogisticsDelegate delegate);

    /**
     * 根据ID查询委派单
     *
     * @param id 委派单ID
     * @return 委派单记录，不存在返回null
     */
    LogisticsDelegate getDelegateById(Long id);

    /**
     * 根据凭证号查询委派单
     *
     * @param voucherNo 委派单凭证号
     * @return 委派单记录，不存在返回null
     */
    LogisticsDelegate getDelegateByVoucherNo(String voucherNo);

    /**
     * 查询货主企业的所有委派单
     *
     * @param ownerEntId 货主企业ID
     * @return 委派单列表
     */
    List<LogisticsDelegate> listByOwnerEntId(Long ownerEntId);

    /**
     * 查询承运企业的所有委派单
     *
     * @param carrierEntId 承运企业ID
     * @return 委派单列表
     */
    List<LogisticsDelegate> listByCarrierEntId(Long carrierEntId);

    /**
     * 单次查询，支持可选过滤条件
     *
     * 根据企业ID查询该企业作为货主或承运人的委派单，
     * 支持按业务场景和状态筛选。
     *
     * @param entId 企业ID
     * @param businessScene 业务场景筛选，可为空
     * @param status 状态筛选，可为空
     * @return 符合条件的委派单列表
     */
    List<LogisticsDelegate> listByEntIdWithFilters(Long entId, Integer businessScene, Integer status);

    /**
     * 指派司机
     *
     * @param voucherNo 委派单凭证号
     * @param driverId 司机ID
     * @param driverName 司机姓名
     * @param vehicleNo 车牌号
     * @return 更新后的委派单
     * @throws IllegalArgumentException 委派单不存在或状态不允许指派
     */
    LogisticsDelegate assignDriver(String voucherNo, String driverId, String driverName, String vehicleNo);

    /**
     * 确认提货（基础版）
     *
     * 司机到达起运地仓库，凭提货码确认提货。
     *
     * @param voucherNo 委派单凭证号
     * @param authCode 提货授权码
     * @return 更新后的委派单
     * @throws IllegalArgumentException 授权码不匹配或委派单状态不允许提货
     */
    LogisticsDelegate confirmPickup(String voucherNo, String authCode);

    /**
     * 确认提货（带GPS位置验证）
     *
     * 司机确认提货时上传GPS坐标，系统验证是否在仓库地理围栏内（500米半径）。
     * 位置验证可有效防止司机伪造提货记录。
     *
     * @param voucherNo 委派单凭证号
     * @param authCode 提货授权码
     * @param driverLatitude 司机所在纬度
     * @param driverLongitude 司机所在经度
     * @return 更新后的委派单
     * @throws IllegalArgumentException 授权码不匹配、位置不在围栏内或委派单状态不允许
     */
    LogisticsDelegate confirmPickup(String voucherNo, String authCode,
                                   BigDecimal driverLatitude, BigDecimal driverLongitude);

    /**
     * 物流到达确认
     *
     * 司机到达目的地仓库或下一节点时调用。
     *
     * @param voucherNo 委派单凭证号
     * @param actionType 动作类型（如：到达起运地、到达中转地、到达目的地）
     * @param targetReceiptId 目标仓单ID（仓单入库场景）
     * @return 更新后的委派单
     * @throws IllegalArgumentException 委派单不存在或状态不允许该操作
     */
    LogisticsDelegate arrive(String voucherNo, Integer actionType, Long targetReceiptId);

    /**
     * 更新委派单状态
     *
     * @param voucherNo 委派单凭证号
     * @param status 新状态
     * @return 更新后的委派单
     * @throws IllegalArgumentException 委派单不存在或状态转换不合法
     */
    LogisticsDelegate updateStatus(String voucherNo, Integer status);

    // ==================== 轨迹操作 ====================

    /**
     * 上报物流轨迹节点
     *
     * 司机或物流系统定时上报当前位置和时间。
     *
     * @param track 轨迹记录
     * @return 创建的轨迹记录
     */
    LogisticsTrack reportTrack(LogisticsTrack track);

    /**
     * 查询委派单的全部轨迹记录
     *
     * @param voucherNo 委派单凭证号
     * @return 轨迹列表（按时间正序）
     */
    List<LogisticsTrack> listTracks(String voucherNo);

    /**
     * 获取委派单的最新轨迹
     *
     * @param voucherNo 委派单凭证号
     * @return 最新轨迹记录，不存在返回null
     */
    LogisticsTrack getLatestTrack(String voucherNo);

    /**
     * 查询委派单的轨迹偏离记录
     *
     * @param voucherNo 委派单凭证号
     * @return 偏离轨迹列表
     */
    List<LogisticsTrack> listDeviations(String voucherNo);

    // ==================== 物流追踪 ====================

    /**
     * 追踪物流完整信息
     *
     * 返回委派单的完整状态信息和轨迹数据。
     *
     * @param voucherNo 委派单凭证号
     * @return 包含委派单信息、轨迹列表、当前位置等
     */
    Map<String, Object> trackLogistics(String voucherNo);

    /**
     * 确认交货
     *
     * 货物到达目的地后，仓单入库或直接移库完成交货。
     *
     * @param voucherNo 委派单凭证号
     * @param action 交货动作（如：仓单入库、直接移库）
     * @param targetReceiptId 目标仓单ID
     * @return 更新后的委派单
     * @throws IllegalArgumentException 委派单不存在或状态不允许交货
     */
    LogisticsDelegate confirmDelivery(String voucherNo, Integer action, String targetReceiptId);

    /**
     * 作废委派单
     *
     * 在特定条件下（如货物丢失、订单取消）作废委派单。
     *
     * @param voucherNo 委派单凭证号
     * @return 更新后的委派单
     * @throws IllegalArgumentException 委派单不存在或状态不允许作废
     */
    LogisticsDelegate invalidate(String voucherNo);

    /**
     * 验证委派单有效性
     *
     * @param voucherNo 委派单凭证号
     * @return 是否有效
     */
    boolean validateDelegate(String voucherNo);
}