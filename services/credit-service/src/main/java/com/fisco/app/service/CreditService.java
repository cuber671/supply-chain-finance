package com.fisco.app.service;

import java.math.BigDecimal;
import java.util.List;

import com.fisco.app.entity.CreditEvent;
import com.fisco.app.entity.EnterpriseCreditProfile;

/**
 * 信用管理业务服务接口
 *
 * 提供企业信用档案管理、信用事件上报、信用评分计算等服务
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface CreditService {

    // ==================== 信用档案管理 ====================

    /**
     * 获取企业信用档案
     *
     * @param entId 企业ID
     * @return 信用档案信息
     */
    EnterpriseCreditProfile getCreditProfile(Long entId);

    /**
     * 创建企业信用档案（注册企业时自动创建）
     *
     * @param entId 企业ID
     * @return 创建的信用档案
     */
    EnterpriseCreditProfile createCreditProfile(Long entId);

    /**
     * 更新信用档案
     *
     * @param profile 信用档案
     * @return 是否成功
     */
    boolean updateCreditProfile(EnterpriseCreditProfile profile);

    /**
     * 设置授信额度
     *
     * @param entId 企业ID
     * @param availableLimit 授信额度
     * @return 是否成功
     */
    boolean setCreditLimit(Long entId, BigDecimal availableLimit);

    /**
     * 使用信用额度（融资时调用）
     *
     * @param entId 企业ID
     * @param amount 使用金额
     * @return 是否成功
     */
    boolean useCreditLimit(Long entId, BigDecimal amount);

    /**
     * 释放信用额度（还款时调用）
     *
     * @param entId 企业ID
     * @param amount 释放金额
     * @return 是否成功
     */
    boolean releaseCreditLimit(Long entId, BigDecimal amount);

    /**
     * 调整信用额度
     *
     * @param entId 企业ID
     * @param newLimit 新额度
     * @return 是否成功
     */
    boolean adjustCreditLimit(Long entId, BigDecimal newLimit);

    /**
     * 查询可用信用额度
     *
     * @param entId 企业ID
     * @return 可用额度
     */
    BigDecimal getAvailableCreditLimit(Long entId);

    // ==================== 信用事件管理 ====================

    /**
     * 上报信用事件
     *
     * @param entId 企业ID
     * @param eventType 事件类型
     * @param eventLevel 事件等级
     * @param eventDesc 事件描述
     * @param scoreChange 分值变化
     * @param relatedModule 关联模块
     * @param relatedId 关联业务ID
     * @return 事件记录ID
     */
    Long reportCreditEvent(Long entId, String eventType, String eventLevel,
            String eventDesc, Integer scoreChange, String relatedModule, String relatedId);

    /**
     * 查询企业信用事件列表
     *
     * @param entId 企业ID
     * @return 信用事件列表
     */
    List<CreditEvent> listCreditEvents(Long entId);

    /**
     * 查询企业信用事件列表（按类型过滤）
     *
     * @param entId 企业ID
     * @param eventType 事件类型
     * @return 信用事件列表
     */
    List<CreditEvent> listCreditEventsByType(Long entId, String eventType);

    /**
     * 统计企业逾期次数
     *
     * @param entId 企业ID
     * @return 逾期次数
     */
    int countOverdueEvents(Long entId);

    // ==================== 信用评分计算 ====================

    /**
     * 计算并更新信用分
     * 根据企业历史事件计算新的信用分
     *
     * @param entId 企业ID
     * @return 新的信用分
     */
    int calculateCreditScore(Long entId);

    /**
     * 触发信用等级重算
     * 系统每月自动调用，或手动触发
     *
     * @param entId 企业ID
     * @return 新的信用等级
     */
    String recalculateCreditLevel(Long entId);

    /**
     * 信用评分（对外服务接口）
     *
     * @param entId 企业ID
     * @return 信用评分结果
     */
    CreditScoreResult getCreditScore(Long entId);

    /**
     * 获取信用画像（综合信息）
     *
     * @param entId 企业ID
     * @return 信用画像
     */
    CreditPortrait getCreditPortrait(Long entId);

    // ==================== 额度锁死校验 ====================

    /**
     * 校验额度是否充足（物流委派单生成前调用）
     *
     * @param entId 企业ID
     * @param requiredAmount 需求金额
     * @return 校验结果
     */
    LimitCheckResult checkCreditLimit(Long entId, BigDecimal requiredAmount);

    /**
     * 额度锁死（当已用额度超过授信总额时）
     *
     * @param entId 企业ID
     * @return 是否成功
     */
    boolean lockCreditLimit(Long entId);

    // ==================== 信用黑名单 ====================

    /**
     * 检查是否触发信用黑名单
     * 当信用分低于阈值时触发
     *
     * @param entId 企业ID
     * @return 是否触发黑名单
     */
    boolean checkBlacklist(Long entId);

    /**
     * 触发信用黑名单
     * 触发全局禁言、资产锁定、银行预警
     *
     * @param entId 企业ID
     * @return 是否成功
     */
    boolean triggerBlacklist(Long entId);

    /**
     * 移除信用黑名单
     *
     * @param entId 企业ID
     * @return 是否成功
     */
    boolean removeBlacklist(Long entId);

    // ==================== 信用等级差异化服务 ====================

    /**
     * 获取质押率
     * 根据企业信用等级返回对应的质押率
     *
     * @param entId 企业ID
     * @return 质押率（0.0-1.0）
     */
    BigDecimal getPledgeRate(Long entId);

    /**
     * 计算可融资额度
     * 根据货物价值和质押率计算可融资额度
     *
     * @param entId 企业ID
     * @param goodsValue 货物价值
     * @return 可融资额度
     */
    BigDecimal calculateFinancingAmount(Long entId, BigDecimal goodsValue);

    /**
     * 检查是否需要强制监控
     * 低信用企业需要强制摄像头监控
     *
     * @param entId 企业ID
     * @return 是否需要强制监控
     */
    boolean requireMandatoryMonitoring(Long entId);

    /**
     * 获取监控频率
     * 根据企业信用等级返回对应的监控频率
     *
     * @param entId 企业ID
     * @return 监控频率（小时）
     */
    Integer getMonitoringFrequency(Long entId);

    /**
     * 获取差异化服务配置
     * 返回企业当前信用等级对应的所有差异化服务配置
     *
     * @param entId 企业ID
     * @return 差异化服务配置
     */
    DifferentiatedServiceConfig getDifferentiatedServiceConfig(Long entId);

    // ==================== 内部类 ====================

    /**
     * 信用评分结果
     */
    class CreditScoreResult {
        private Long entId;
        private Integer creditScore;
        private String creditLevel;
        private String lastEvalTime;
        private BigDecimal availableLimit;
        private BigDecimal usedLimit;
        private Integer overdueCount;

        // getters and setters
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public String getCreditLevel() { return creditLevel; }
        public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
        public String getLastEvalTime() { return lastEvalTime; }
        public void setLastEvalTime(String lastEvalTime) { this.lastEvalTime = lastEvalTime; }
        public BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public BigDecimal getUsedLimit() { return usedLimit; }
        public void setUsedLimit(BigDecimal usedLimit) { this.usedLimit = usedLimit; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
    }

    /**
     * 信用画像
     */
    class CreditPortrait {
        private Long entId;
        private String enterpriseName;
        private Integer creditScore;
        private String creditLevel;
        private BigDecimal availableLimit;
        private BigDecimal usedLimit;
        private BigDecimal availableBalance;
        private Integer overdueCount;
        private String lastEvalTime;
        private Boolean isBlacklisted;
        private List<CreditEvent> recentEvents;

        // getters and setters
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public String getCreditLevel() { return creditLevel; }
        public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
        public BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public BigDecimal getUsedLimit() { return usedLimit; }
        public void setUsedLimit(BigDecimal usedLimit) { this.usedLimit = usedLimit; }
        public BigDecimal getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
        public String getLastEvalTime() { return lastEvalTime; }
        public void setLastEvalTime(String lastEvalTime) { this.lastEvalTime = lastEvalTime; }
        public Boolean getIsBlacklisted() { return isBlacklisted; }
        public void setIsBlacklisted(Boolean isBlacklisted) { this.isBlacklisted = isBlacklisted; }
        public List<CreditEvent> getRecentEvents() { return recentEvents; }
        public void setRecentEvents(List<CreditEvent> recentEvents) { this.recentEvents = recentEvents; }
    }

    /**
     * 额度校验结果
     */
    class LimitCheckResult {
        private boolean passed;
        private String message;
        private BigDecimal availableLimit;
        private BigDecimal requiredAmount;
        private Long entId;

        // getters and setters
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public BigDecimal getRequiredAmount() { return requiredAmount; }
        public void setRequiredAmount(BigDecimal requiredAmount) { this.requiredAmount = requiredAmount; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
    }

    /**
     * 信用等级差异化服务配置
     */
    class DifferentiatedServiceConfig {
        private Long entId;
        private String creditLevel;
        private BigDecimal pledgeRate;          // 质押率
        private boolean mandatoryMonitoring;    // 是否强制监控
        private Integer monitoringFrequency;    // 监控频率（小时）
        private String serviceLevel;            // 服务等级：HIGH, MEDIUM, LOW
        private String description;             // 描述

        // getters and setters
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getCreditLevel() { return creditLevel; }
        public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
        public BigDecimal getPledgeRate() { return pledgeRate; }
        public void setPledgeRate(BigDecimal pledgeRate) { this.pledgeRate = pledgeRate; }
        public boolean isMandatoryMonitoring() { return mandatoryMonitoring; }
        public void setMandatoryMonitoring(boolean mandatoryMonitoring) { this.mandatoryMonitoring = mandatoryMonitoring; }
        public Integer getMonitoringFrequency() { return monitoringFrequency; }
        public void setMonitoringFrequency(Integer monitoringFrequency) { this.monitoringFrequency = monitoringFrequency; }
        public String getServiceLevel() { return serviceLevel; }
        public void setServiceLevel(String serviceLevel) { this.serviceLevel = serviceLevel; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
