package com.fisco.app.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fisco.app.entity.CreditEvent;
import com.fisco.app.entity.EnterpriseCreditProfile;
import com.fisco.app.feign.BlockchainFeignClient;
import com.fisco.app.feign.EnterpriseFeignClient;
import com.fisco.app.mapper.CreditEventMapper;
import com.fisco.app.mapper.CreditProfileMapper;
import com.fisco.app.service.CreditService;


/**
 * 信用管理业务服务实现类
 *
 * 实现企业信用档案管理、信用事件上报、信用评分计算等服务
 * 集成区块链上链服务完成信用数据存证
 *
 * 信用评分计算引擎说明：
 * - 加分事件：准时还款(+10~+30)、货物无损(+5~+20)、入库稳定(+5~+15)
 * - 扣分事件：逾期还款(-5~-50/天)、物流偏航(-10~-30)、仓单异常(-20~-50)、频繁撤单(-5~-20)
 * - 时间衰减：近90天内事件100%权重，90-180天50%权重，180天以上25%权重
 * - 事件等级：LOW=1x, MEDIUM=1.5x, HIGH=2x, SEVERE=3x
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class CreditServiceImpl implements CreditService {

    private static final Logger logger = LoggerFactory.getLogger(CreditServiceImpl.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 额度配置常量 ====================

    /** 最大授信额度（1万亿） */
    private static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("1000000000000");
    /** 最小授信额度 */
    private static final BigDecimal MIN_CREDIT_LIMIT = BigDecimal.ZERO;
    /** 单次最大使用额度 */
    private static final BigDecimal MAX_USE_AMOUNT = new BigDecimal("100000000000");

    // ==================== 评分计算引擎配置 ====================

    /** 评分事件基础分值配置 */
    private static final Map<String, Integer> EVENT_BASE_SCORES = new HashMap<>();
    static {
        // 加分事件（正向）
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_ON_TIME_REPAY, 15);      // 准时还款 +15
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_EARLY_REPAY, 20);       // 提前还款 +20
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_GOODS_UNDAMAGED, 10);   // 货物无损 +10
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_STABLE_STORAGE, 8);     // 入库稳定 +8

        // 扣分事件（负向）
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_OVERDUE, -20);         // 逾期 -20
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_DEFAULTER, -50);        // 违约 -50
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_LOGISTICS_DEVIATION, -15); // 物流偏航 -15
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_RECEIPT_ABNORMAL, -25); // 仓单异常 -25
        EVENT_BASE_SCORES.put(CreditEvent.EVENT_TYPE_FREQUENT_CANCEL, -10);  // 频繁撤单 -10
    }

    /** 事件等级权重 */
    private static final Map<String, Double> EVENT_LEVEL_WEIGHTS = new HashMap<>();
    static {
        EVENT_LEVEL_WEIGHTS.put(CreditEvent.EVENT_LEVEL_LOW, 1.0);
        EVENT_LEVEL_WEIGHTS.put(CreditEvent.EVENT_LEVEL_MEDIUM, 1.5);
        EVENT_LEVEL_WEIGHTS.put(CreditEvent.EVENT_LEVEL_HIGH, 2.0);
        EVENT_LEVEL_WEIGHTS.put(CreditEvent.EVENT_LEVEL_SEVERE, 3.0);
    }

    /** 时间衰减天数阈值 */
    private static final int RECENT_DAYS = 90;
    private static final int MEDIUM_DAYS = 180;

    @Autowired
    private CreditProfileMapper creditProfileMapper;

    @Autowired
    private CreditEventMapper creditEventMapper;

    @Autowired
    private BlockchainFeignClient blockchainFeignClient;

    @Autowired(required = false)
    private EnterpriseFeignClient enterpriseFeignClient;

    // ==================== 信用档案管理 ====================

    @Override
    public EnterpriseCreditProfile getCreditProfile(Long entId) {
        if (entId == null) {
            return null;
        }
        return creditProfileMapper.selectByEntId(entId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EnterpriseCreditProfile createCreditProfile(Long entId) {
        // 检查是否已存在
        EnterpriseCreditProfile exist = getCreditProfile(entId);
        if (exist != null) {
            logger.warn("企业信用档案已存在，entId: {}", entId);
            return exist;
        }

        EnterpriseCreditProfile profile = new EnterpriseCreditProfile();
        profile.setEntId(entId);
        profile.setCreditScore(EnterpriseCreditProfile.DEFAULT_SCORE);
        profile.setCreditLevel(EnterpriseCreditProfile.calculateLevel(EnterpriseCreditProfile.DEFAULT_SCORE));
        profile.setAvailableLimit(BigDecimal.ZERO);
        profile.setUsedLimit(BigDecimal.ZERO);
        profile.setOverdueCount(0);
        profile.setLastEvalTime(LocalDateTime.now());

        creditProfileMapper.insert(profile);
        logger.info("创建企业信用档案成功，entId: {}", entId);

        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCreditProfile(EnterpriseCreditProfile profile) {
        if (profile == null || profile.getEntId() == null) {
            return false;
        }
        return creditProfileMapper.updateByEntId(profile) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setCreditLimit(Long entId, BigDecimal availableLimit) {
        // 参数校验
        if (entId == null) {
            throw new IllegalArgumentException("企业ID不能为空");
        }
        if (availableLimit == null || availableLimit.compareTo(MIN_CREDIT_LIMIT) <= 0) {
            throw new IllegalArgumentException("授信额度必须大于0");
        }
        if (availableLimit.compareTo(MAX_CREDIT_LIMIT) > 0) {
            throw new IllegalArgumentException("授信额度不能超过" + MAX_CREDIT_LIMIT);
        }

        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            profile = createCreditProfile(entId);
        }

        // 获取企业区块链地址
        String blockchainAddress = getEnterpriseBlockchainAddress(entId);
        if (blockchainAddress != null) {
            try {
                // 上链设置授信额度
                BigInteger limitWei = toWei(availableLimit);
                BlockchainFeignClient.EnterpriseCreditLimitRequest request =
                    new BlockchainFeignClient.EnterpriseCreditLimitRequest();
                request.setEnterpriseAddress(blockchainAddress);
                request.setNewLimit(limitWei.longValue());
                blockchainFeignClient.setCreditLimit(request);
                logger.info("授信额度上链成功，entId: {}, limit: {}", entId, availableLimit);
            } catch (Exception e) {
                logger.error("授信额度上链失败，entId: {}", entId, e);
                // C5: 乐观模式 - 区块链失败时信任本地 DB，链上链下短期不一致风险
                // 建议：后续添加重试队列或人工处理机制
            }
        }

        // 更新本地数据库
        profile.setAvailableLimit(availableLimit);
        profile.setLastEvalTime(LocalDateTime.now());

        return updateCreditProfile(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useCreditLimit(Long entId, BigDecimal amount) {
        // 参数校验
        if (entId == null) {
            throw new IllegalArgumentException("企业ID不能为空");
        }
        if (amount == null || amount.compareTo(MIN_CREDIT_LIMIT) <= 0) {
            throw new IllegalArgumentException("使用金额必须大于0");
        }
        if (amount.compareTo(MAX_USE_AMOUNT) > 0) {
            throw new IllegalArgumentException("单次使用金额不能超过" + MAX_USE_AMOUNT);
        }

        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            logger.error("企业信用档案不存在，entId: {}", entId);
            return false;
        }

        // 检查链上可用额度（作为第二道防线）
        String blockchainAddress = getEnterpriseBlockchainAddress(entId);
        if (blockchainAddress != null) {
            try {
                BigInteger amountWei = toWei(amount);
                BlockchainFeignClient.CreditCheckLimitRequest checkRequest =
                    new BlockchainFeignClient.CreditCheckLimitRequest();
                checkRequest.setEnterpriseAddress(blockchainAddress);
                checkRequest.setAmount(amountWei.longValue());
                var chainCheckResult = blockchainFeignClient.checkCreditLimit(checkRequest);
                boolean chainCheck = chainCheckResult.getCode() != null && chainCheckResult.getCode() == 200 && Boolean.TRUE.equals(chainCheckResult.getData());
                if (!chainCheck) {
                    logger.warn("链上额度校验不通过，entId: {}, amount: {}", entId, amount);
                    return false;
                }
            } catch (Exception e) {
                logger.warn("链上额度校验失败，使用链下结果，entId: {}", entId, e);
            }
        }

        // 检查链下可用额度
        BigDecimal available = profile.getAvailableLimit().subtract(profile.getUsedLimit());
        if (available.compareTo(amount) < 0) {
            logger.warn("可用额度不足，entId: {}, available: {}, required: {}", entId, available, amount);
            return false;
        }

        // 获取企业区块链地址
        if (blockchainAddress != null) {
            try {
                // 上链使用信用额度
                BigInteger amountWei = toWei(amount);
                BlockchainFeignClient.CreditUseRequest useRequest =
                    new BlockchainFeignClient.CreditUseRequest();
                useRequest.setEnterpriseAddress(blockchainAddress);
                useRequest.setAmount(amountWei.longValue());
                useRequest.setOperationType("USE_FOR_FINANCING");
                blockchainFeignClient.useCredit(useRequest);
                logger.info("信用额度使用上链成功，entId: {}, amount: {}", entId, amount);
            } catch (Exception e) {
                logger.error("信用额度使用上链失败，entId: {}, amount: {}", entId, amount, e);
                // C5: 乐观模式 - 区块链失败时信任本地 DB，链上链下短期不一致风险
            }
        }

        // 更新本地数据库
        profile.setUsedLimit(profile.getUsedLimit().add(amount));
        profile.setLastEvalTime(LocalDateTime.now());

        return updateCreditProfile(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseCreditLimit(Long entId, BigDecimal amount) {
        // 参数校验
        if (entId == null) {
            throw new IllegalArgumentException("企业ID不能为空");
        }
        if (amount == null || amount.compareTo(MIN_CREDIT_LIMIT) <= 0) {
            throw new IllegalArgumentException("释放金额必须大于0");
        }
        if (amount.compareTo(MAX_USE_AMOUNT) > 0) {
            throw new IllegalArgumentException("单次释放金额不能超过" + MAX_USE_AMOUNT);
        }

        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            logger.error("企业信用档案不存在，entId: {}", entId);
            return false;
        }

        // 确保释放金额不超过已用额度
        BigDecimal releaseAmount = amount;
        if (profile.getUsedLimit().compareTo(amount) < 0) {
            releaseAmount = profile.getUsedLimit();
        }

        // 获取企业区块链地址
        String blockchainAddress = getEnterpriseBlockchainAddress(entId);
        if (blockchainAddress != null) {
            try {
                // 上链释放信用额度
                BigInteger amountWei = toWei(releaseAmount);
                BlockchainFeignClient.CreditReleaseRequest releaseRequest =
                    new BlockchainFeignClient.CreditReleaseRequest();
                releaseRequest.setEnterpriseAddress(blockchainAddress);
                releaseRequest.setAmount(amountWei.longValue());
                releaseRequest.setOperationType("REPAYMENT");
                blockchainFeignClient.releaseCredit(releaseRequest);
                logger.info("信用额度释放上链成功，entId: {}, amount: {}", entId, releaseAmount);
            } catch (Exception e) {
                logger.error("信用额度释放上链失败，entId: {}", entId, e);
            }
        }

        // 更新本地数据库
        profile.setUsedLimit(profile.getUsedLimit().subtract(releaseAmount));
        profile.setLastEvalTime(LocalDateTime.now());

        return updateCreditProfile(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustCreditLimit(Long entId, BigDecimal newLimit) {
        // 参数校验
        if (entId == null) {
            throw new IllegalArgumentException("企业ID不能为空");
        }
        if (newLimit == null || newLimit.compareTo(MIN_CREDIT_LIMIT) <= 0) {
            throw new IllegalArgumentException("授信额度不能小于0");
        }
        if (newLimit.compareTo(MAX_CREDIT_LIMIT) > 0) {
            throw new IllegalArgumentException("授信额度不能超过" + MAX_CREDIT_LIMIT);
        }

        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            logger.error("企业信用档案不存在，entId: {}", entId);
            return false;
        }

        // 计算调整金额
        BigDecimal oldLimit = profile.getAvailableLimit();
        BigDecimal adjustment = newLimit.subtract(oldLimit);

        // 获取企业区块链地址
        String blockchainAddress = getEnterpriseBlockchainAddress(entId);
        if (blockchainAddress != null) {
            try {
                if (adjustment.compareTo(BigDecimal.ZERO) > 0) {
                    // 额度增加
                    BigInteger adjustmentWei = toWei(adjustment);
                    BlockchainFeignClient.EnterpriseCreditLimitRequest limitRequest =
                        new BlockchainFeignClient.EnterpriseCreditLimitRequest();
                    limitRequest.setEnterpriseAddress(blockchainAddress);
                    limitRequest.setNewLimit(adjustmentWei.longValue());
                    blockchainFeignClient.setCreditLimit(limitRequest);
                } else if (adjustment.compareTo(BigDecimal.ZERO) < 0) {
                    // 额度减少 - 使用adjustUsedCredit
                    BigInteger adjustmentWei = toWei(adjustment.abs());
                    BlockchainFeignClient.CreditAdjustUsedRequest adjustRequest =
                        new BlockchainFeignClient.CreditAdjustUsedRequest();
                    adjustRequest.setEnterpriseAddress(blockchainAddress);
                    adjustRequest.setAdjustment(adjustmentWei.negate().longValue());
                    blockchainFeignClient.adjustUsedCredit(adjustRequest);
                }
                logger.info("信用额度调整上链成功，entId: {}, oldLimit: {}, newLimit: {}", entId, oldLimit, newLimit);
            } catch (Exception e) {
                logger.error("信用额度调整上链失败，entId: {}", entId, e);
            }
        }

        // 更新本地数据库
        profile.setAvailableLimit(newLimit);
        profile.setLastEvalTime(LocalDateTime.now());

        return updateCreditProfile(profile);
    }

    @Override
    public BigDecimal getAvailableCreditLimit(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            return BigDecimal.ZERO;
        }
        return profile.getAvailableLimit().subtract(profile.getUsedLimit());
    }

    // ==================== 信用事件管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportCreditEvent(Long entId, String eventType, String eventLevel,
            String eventDesc, Integer scoreChange, String relatedModule, String relatedId) {
        // 参数校验
        if (entId == null) {
            throw new IllegalArgumentException("企业ID不能为空");
        }
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("事件类型不能为空");
        }
        if (eventLevel == null || eventLevel.isEmpty()) {
            throw new IllegalArgumentException("事件等级不能为空");
        }
        // 校验分数变化范围 (-500 到 +500)
        if (scoreChange != null && (scoreChange < -500 || scoreChange > 500)) {
            throw new IllegalArgumentException("分数变化范围必须在-500到500之间");
        }

        // 创建事件记录
        CreditEvent event = new CreditEvent();
        event.setEntId(entId);
        event.setEventType(eventType);
        event.setEventLevel(eventLevel);
        event.setEventDesc(eventDesc);
        event.setScoreChange(scoreChange);
        event.setRelatedModule(relatedModule);
        event.setRelatedId(relatedId);
        event.setStatus(CreditEvent.STATUS_VALID);
        event.setReportTime(LocalDateTime.now());

        // 获取企业区块链地址并上链
        String blockchainAddress = getEnterpriseBlockchainAddress(entId);
        if (blockchainAddress != null) {
            try {
                // 获取事件类型对应的链上枚举值
                BigInteger eventTypeValue = getEventTypeValue(eventType);
                BigInteger impact = scoreChange != null ? BigInteger.valueOf(scoreChange) : BigInteger.ZERO;

                // 计算事件数据哈希
                String eventDataHash = calculateEventDataHash(entId, eventType, eventDesc);

                // 上链上报事件
                BlockchainFeignClient.CreditReportEventRequest eventRequest =
                    new BlockchainFeignClient.CreditReportEventRequest();
                eventRequest.setEnterpriseAddress(blockchainAddress);
                eventRequest.setEventType(eventTypeValue.longValue());
                eventRequest.setImpact(impact.longValue());
                eventRequest.setEventDataHash(eventDataHash);
                blockchainFeignClient.reportCreditEvent(eventRequest);
                logger.info("信用事件上链成功，entId: {}, eventType: {}", entId, eventType);
            } catch (Exception e) {
                logger.error("信用事件上链失败，entId: {}, eventType: {}", entId, eventType, e);
                // C5: 乐观模式 - 区块链失败时信任本地 DB，事件已记录，链上链下不一致
            }
        }

        // 保存到数据库
        creditEventMapper.insert(event);
        logger.info("信用事件上报成功，entId: {}, eventId: {}, type: {}", entId, event.getId(), eventType);

        // 更新企业信用分
        if (scoreChange != null && scoreChange != 0) {
            updateCreditScoreByEvent(entId, scoreChange);
        }

        return event.getId();
    }

    @Override
    public List<CreditEvent> listCreditEvents(Long entId) {
        if (entId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<CreditEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditEvent::getEntId, entId)
               .eq(CreditEvent::getStatus, CreditEvent.STATUS_VALID)
               .orderByDesc(CreditEvent::getReportTime);
        return creditEventMapper.selectList(wrapper);
    }

    @Override
    public List<CreditEvent> listCreditEventsByType(Long entId, String eventType) {
        if (entId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<CreditEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditEvent::getEntId, entId)
               .eq(CreditEvent::getEventType, eventType)
               .eq(CreditEvent::getStatus, CreditEvent.STATUS_VALID)
               .orderByDesc(CreditEvent::getReportTime);
        return creditEventMapper.selectList(wrapper);
    }

    @Override
    public int countOverdueEvents(Long entId) {
        if (entId == null) {
            return 0;
        }
        LambdaQueryWrapper<CreditEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CreditEvent::getEntId, entId)
               .eq(CreditEvent::getEventType, CreditEvent.EVENT_TYPE_OVERDUE)
               .eq(CreditEvent::getStatus, CreditEvent.STATUS_VALID);
        return creditEventMapper.selectCount(wrapper).intValue();
    }

    // ==================== 信用评分计算 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateCreditScore(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            profile = createCreditProfile(entId);
        }

        // 使用增强的评分计算引擎
        int totalChange = calculateScoreByEngine(entId);

        // 计算新评分
        int newScore = EnterpriseCreditProfile.DEFAULT_SCORE + totalChange;
        // 确保评分在有效范围内
        if (newScore > EnterpriseCreditProfile.MAX_SCORE) {
            newScore = EnterpriseCreditProfile.MAX_SCORE;
        }
        if (newScore < EnterpriseCreditProfile.MIN_SCORE) {
            newScore = EnterpriseCreditProfile.MIN_SCORE;
        }

        // 更新信用档案
        profile.setCreditScore(newScore);
        profile.setCreditLevel(EnterpriseCreditProfile.calculateLevel(newScore));
        profile.setLastEvalTime(LocalDateTime.now());

        // 统计逾期次数
        profile.setOverdueCount(countOverdueEvents(entId));

        updateCreditProfile(profile);

        // 尝试链上更新评分
        String blockchainAddress = getEnterpriseBlockchainAddress(entId);
        if (blockchainAddress != null) {
            try {
                BlockchainFeignClient.CreditCalculateScoreRequest scoreRequest =
                    new BlockchainFeignClient.CreditCalculateScoreRequest();
                scoreRequest.setEnterpriseAddress(blockchainAddress);
                blockchainFeignClient.calculateCreditScore(scoreRequest);
                logger.info("信用评分计算上链成功，entId: {}, score: {}", entId, newScore);
            } catch (Exception e) {
                logger.error("信用评分计算上链失败，entId: {}", entId, e);
            }
        }

        logger.info("信用评分计算完成，entId: {}, score: {}", entId, newScore);
        return newScore;
    }

    /**
     * 信用评分计算引擎
     *
     * 根据事件类型、等级和时间衰减计算信用分变化
     *
     * @param entId 企业ID
     * @return 信用分变化值
     */
    private int calculateScoreByEngine(Long entId) {
        List<CreditEvent> events = listCreditEvents(entId);
        int totalWeightedChange = 0;

        LocalDateTime now = LocalDateTime.now();

        for (CreditEvent event : events) {
            if (event.getScoreChange() == null || event.getReportTime() == null) {
                continue;
            }

            // 1. 获取基础分值
            // C4: 防御性处理 - 确保 scoreChange 不为 null 再 auto-unboxing
            Integer scoreChange = event.getScoreChange();
            int baseScore = EVENT_BASE_SCORES.getOrDefault(event.getEventType(), scoreChange != null ? scoreChange : 0);

            // 2. 应用事件等级权重
            double levelWeight = EVENT_LEVEL_WEIGHTS.getOrDefault(event.getEventLevel(), 1.0);

            // 3. 计算时间衰减权重
            long daysAgo = ChronoUnit.DAYS.between(event.getReportTime(), now);
            double timeWeight = calculateTimeWeight(daysAgo);

            // 4. 计算加权分值
            int weightedScore = (int) (baseScore * levelWeight * timeWeight);
            totalWeightedChange += weightedScore;

            logger.debug("事件计算: type={}, baseScore={}, levelWeight={}, daysAgo={}, timeWeight={}, weightedScore={}",
                    event.getEventType(), baseScore, levelWeight, daysAgo, timeWeight, weightedScore);
        }

        return totalWeightedChange;
    }

    /**
     * 计算时间衰减权重
     *
     * @param daysAgo 事件发生距今天数
     * @return 权重值
     */
    private double calculateTimeWeight(long daysAgo) {
        if (daysAgo <= RECENT_DAYS) {
            // 90天内：100%权重
            return 1.0;
        } else if (daysAgo <= MEDIUM_DAYS) {
            // 90-180天：50%权重
            return 0.5;
        } else {
            // 180天以上：25%权重
            return 0.25;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String recalculateCreditLevel(Long entId) {
        int newScore = calculateCreditScore(entId);
        return EnterpriseCreditProfile.calculateLevel(newScore);
    }

    @Override
    public CreditScoreResult getCreditScore(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        CreditScoreResult result = new CreditScoreResult();
        result.setEntId(entId);

        if (profile != null) {
            result.setCreditScore(profile.getCreditScore());
            result.setCreditLevel(profile.getCreditLevel());
            result.setAvailableLimit(profile.getAvailableLimit().subtract(profile.getUsedLimit()));
            result.setUsedLimit(profile.getUsedLimit());
            result.setOverdueCount(profile.getOverdueCount());
            if (profile.getLastEvalTime() != null) {
                result.setLastEvalTime(profile.getLastEvalTime().format(DATE_TIME_FORMATTER));
            }
        }

        return result;
    }

    @Override
    public CreditPortrait getCreditPortrait(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);

        CreditPortrait portrait = new CreditPortrait();
        portrait.setEntId(entId);

        // 获取企业名称
        String enterpriseName = getEnterpriseName(entId);
        portrait.setEnterpriseName(enterpriseName);

        if (profile != null) {
            portrait.setCreditScore(profile.getCreditScore());
            portrait.setCreditLevel(profile.getCreditLevel());
            portrait.setAvailableLimit(profile.getAvailableLimit());
            portrait.setUsedLimit(profile.getUsedLimit());
            portrait.setAvailableBalance(profile.getAvailableLimit().subtract(profile.getUsedLimit()));
            portrait.setOverdueCount(profile.getOverdueCount());
            if (profile.getLastEvalTime() != null) {
                portrait.setLastEvalTime(profile.getLastEvalTime().format(DATE_TIME_FORMATTER));
            }
        }

        // 获取最近事件
        List<CreditEvent> recentEvents = listCreditEvents(entId);
        if (recentEvents.size() > 10) {
            portrait.setRecentEvents(recentEvents.subList(0, 10));
        } else {
            portrait.setRecentEvents(recentEvents);
        }

        // 检查是否在黑名单
        portrait.setIsBlacklisted(checkBlacklist(entId));

        return portrait;
    }

    // ==================== 额度锁死校验 ====================

    @Override
    public LimitCheckResult checkCreditLimit(Long entId, BigDecimal requiredAmount) {
        LimitCheckResult result = new LimitCheckResult();
        result.setEntId(entId);
        result.setRequiredAmount(requiredAmount);

        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            result.setPassed(false);
            result.setMessage("企业信用档案不存在");
            return result;
        }

        // 链下数据库校验
        BigDecimal availableLimit = profile.getAvailableLimit().subtract(profile.getUsedLimit());
        result.setAvailableLimit(availableLimit);

        boolean offChainPassed = availableLimit.compareTo(requiredAmount) >= 0;

        // 链上合约校验（作为第二道防线）
        boolean onChainPassed = true;
        String blockchainAddress = getEnterpriseBlockchainAddress(entId);
        if (blockchainAddress != null) {
            try {
                BigInteger requiredAmountWei = toWei(requiredAmount);
                BlockchainFeignClient.CreditCheckLimitRequest checkRequest =
                    new BlockchainFeignClient.CreditCheckLimitRequest();
                checkRequest.setEnterpriseAddress(blockchainAddress);
                checkRequest.setAmount(requiredAmountWei.longValue());
                var chainCheckResult = blockchainFeignClient.checkCreditLimit(checkRequest);
                onChainPassed = chainCheckResult.getCode() != null && chainCheckResult.getCode() == 200 && Boolean.TRUE.equals(chainCheckResult.getData());
                logger.info("额度链上校验，entId: {}, onChainPassed: {}", entId, onChainPassed);
            } catch (Exception e) {
                logger.warn("额度链上校验失败，使用链下结果，entId: {}", entId, e);
                // 链上校验失败时，默认信任链下结果
            }
        }

        // 双重校验：链上和链下都必须通过
        if (offChainPassed && onChainPassed) {
            result.setPassed(true);
            result.setMessage("额度校验通过（链下+链上）");
        } else if (!offChainPassed) {
            result.setPassed(false);
            result.setMessage("可用额度不足（链下），当前可用: " + availableLimit + "，需求: " + requiredAmount);
        } else {
            result.setPassed(false);
            result.setMessage("链上额度校验不通过，存在数据不一致风险");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockCreditLimit(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            return false;
        }

        // C3: 正确的锁死逻辑：将可用额度设为0（已用额度不变）
        // 效果：availableLimit - usedLimit = 0，无法再申请新额度
        profile.setAvailableLimit(BigDecimal.ZERO);
        profile.setLastEvalTime(LocalDateTime.now());

        boolean updated = updateCreditProfile(profile);
        if (updated) {
            logger.info("信用额度已锁死，entId: {}, usedLimit: {}", entId, profile.getUsedLimit());
        }
        return updated;
    }

    // ==================== 信用黑名单 ====================

    @Override
    public boolean checkBlacklist(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null || profile.getCreditScore() == null) {
            return false;
        }
        // 信用分低于400分触发黑名单
        return profile.getCreditScore() < 400;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean triggerBlacklist(Long entId) {
        // C1: 触发黑名单 - 将信用分强制降至黑名单阈值以下
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            return false;
        }

        // 黑名单阈值 400，设置 score = 399 确保 checkBlacklist() 返回 true
        profile.setCreditScore(399);
        profile.setCreditLevel(EnterpriseCreditProfile.LEVEL_D);
        profile.setLastEvalTime(LocalDateTime.now());

        boolean updated = updateCreditProfile(profile);
        if (updated) {
            logger.warn("触发信用黑名单，entId: {}, score set to 399", entId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBlacklist(Long entId) {
        // C2: 移除黑名单 - 仅当信用分恢复到阈值以上时有效
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            return false;
        }

        // 检查是否在黑名单中（score < 400）
        if (profile.getCreditScore() != null && profile.getCreditScore() < 400) {
            // 当前 score < 400，无法移除，需先修复 score
            logger.warn("信用分仍低于阈值，无法移除黑名单，entId: {}, score: {}",
                        entId, profile.getCreditScore());
            return false;
        }

        // score >= 400，本就不在黑名单中，无需移除
        logger.info("企业信用分已在阈值以上，不在黑名单中，entId: {}, score: {}",
                    entId, profile.getCreditScore());
        return true;
    }

    // ==================== 信用等级差异化服务 ====================

    // 差异化服务配置常量
    private static final BigDecimal PLEDGE_RATE_AAA = new BigDecimal("0.90");  // 90%
    private static final BigDecimal PLEDGE_RATE_AA = new BigDecimal("0.90");   // 90%
    private static final BigDecimal PLEDGE_RATE_A = new BigDecimal("0.80");    // 80%
    private static final BigDecimal PLEDGE_RATE_B = new BigDecimal("0.65");    // 65%
    private static final BigDecimal PLEDGE_RATE_C = new BigDecimal("0.50");    // 50%
    private static final BigDecimal PLEDGE_RATE_D = new BigDecimal("0.30");    // 30%

    // 监控频率（小时）
    private static final int MONITOR_FREQ_AAA = 24;   // 24小时一次
    private static final int MONITOR_FREQ_AA = 24;    // 24小时一次
    private static final int MONITOR_FREQ_A = 12;    // 12小时一次
    private static final int MONITOR_FREQ_B = 6;      // 6小时一次
    private static final int MONITOR_FREQ_C = 2;      // 2小时一次
    private static final int MONITOR_FREQ_D = 1;      // 1小时一次

    @Override
    public BigDecimal getPledgeRate(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null || profile.getCreditLevel() == null) {
            return PLEDGE_RATE_B; // 默认
        }

        String level = profile.getCreditLevel();
        switch (level) {
            case EnterpriseCreditProfile.LEVEL_AAA:
                return PLEDGE_RATE_AAA;
            case EnterpriseCreditProfile.LEVEL_AA:
                return PLEDGE_RATE_AA;
            case EnterpriseCreditProfile.LEVEL_A:
                return PLEDGE_RATE_A;
            case EnterpriseCreditProfile.LEVEL_B:
                return PLEDGE_RATE_B;
            case EnterpriseCreditProfile.LEVEL_C:
                return PLEDGE_RATE_C;
            case EnterpriseCreditProfile.LEVEL_D:
                return PLEDGE_RATE_D;
            default:
                return PLEDGE_RATE_B;
        }
    }

    @Override
    public BigDecimal calculateFinancingAmount(Long entId, BigDecimal goodsValue) {
        if (goodsValue == null || goodsValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal pledgeRate = getPledgeRate(entId);
        return goodsValue.multiply(pledgeRate).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean requireMandatoryMonitoring(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null || profile.getCreditLevel() == null) {
            return false;
        }

        // C级及以下需要强制监控
        String level = profile.getCreditLevel();
        return EnterpriseCreditProfile.LEVEL_C.equals(level)
            || EnterpriseCreditProfile.LEVEL_D.equals(level);
    }

    @Override
    public Integer getMonitoringFrequency(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null || profile.getCreditLevel() == null) {
            return MONITOR_FREQ_B; // 默认
        }

        String level = profile.getCreditLevel();
        switch (level) {
            case EnterpriseCreditProfile.LEVEL_AAA:
                return MONITOR_FREQ_AAA;
            case EnterpriseCreditProfile.LEVEL_AA:
                return MONITOR_FREQ_AA;
            case EnterpriseCreditProfile.LEVEL_A:
                return MONITOR_FREQ_A;
            case EnterpriseCreditProfile.LEVEL_B:
                return MONITOR_FREQ_B;
            case EnterpriseCreditProfile.LEVEL_C:
                return MONITOR_FREQ_C;
            case EnterpriseCreditProfile.LEVEL_D:
                return MONITOR_FREQ_D;
            default:
                return MONITOR_FREQ_B;
        }
    }

    @Override
    public DifferentiatedServiceConfig getDifferentiatedServiceConfig(Long entId) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        DifferentiatedServiceConfig config = new DifferentiatedServiceConfig();
        config.setEntId(entId);

        if (profile != null && profile.getCreditLevel() != null) {
            config.setCreditLevel(profile.getCreditLevel());
            config.setPledgeRate(getPledgeRate(entId));
            config.setMandatoryMonitoring(requireMandatoryMonitoring(entId));
            config.setMonitoringFrequency(getMonitoringFrequency(entId));

            // 设置服务等级和描述
            String level = profile.getCreditLevel();
            if (EnterpriseCreditProfile.LEVEL_AAA.equals(level) || EnterpriseCreditProfile.LEVEL_AA.equals(level)) {
                config.setServiceLevel("HIGH");
                config.setDescription("高信用企业，享受优质金融服务：质押率" +
                    getPledgeRate(entId).multiply(new BigDecimal("100")).intValue() +
                    "%，低监控频率" + getMonitoringFrequency(entId) + "小时/次");
            } else if (EnterpriseCreditProfile.LEVEL_A.equals(level) || EnterpriseCreditProfile.LEVEL_B.equals(level)) {
                config.setServiceLevel("MEDIUM");
                config.setDescription("中等信用企业：质押率" +
                    getPledgeRate(entId).multiply(new BigDecimal("100")).intValue() +
                    "%，监控频率" + getMonitoringFrequency(entId) + "小时/次");
            } else {
                config.setServiceLevel("LOW");
                config.setDescription("低信用企业，需加强监控：质押率" +
                    getPledgeRate(entId).multiply(new BigDecimal("100")).intValue() +
                    "%，强制" + (requireMandatoryMonitoring(entId) ? "摄像头监控" : "定期监控") +
                    "，" + getMonitoringFrequency(entId) + "小时/次");
            }
        } else {
            config.setCreditLevel("UNKNOWN");
            config.setPledgeRate(PLEDGE_RATE_B);
            config.setMandatoryMonitoring(false);
            config.setMonitoringFrequency(MONITOR_FREQ_B);
            config.setServiceLevel("MEDIUM");
            config.setDescription("默认中等信用服务");
        }

        return config;
    }

    // ==================== 私有方法 ====================

    /**
     * 获取企业区块链地址
     */
    private String getEnterpriseBlockchainAddress(Long entId) {
        if (enterpriseFeignClient != null) {
            try {
                return enterpriseFeignClient.getBlockchainAddress(entId);
            } catch (Exception e) {
                logger.warn("获取企业区块链地址失败，entId: {}", entId, e);
            }
        }
        return null;
    }

    /**
     * 获取企业名称
     */
    private String getEnterpriseName(Long entId) {
        if (entId == null) {
            return null;
        }
        try {
            var result = enterpriseFeignClient.getEnterpriseById(entId);
            if (result != null && result.getCode() == 0 && result.getData() != null) {
                // The result is an Enterprise object, try to get enterpriseName
                if (result.getData() instanceof Map) {
                    Map<?, ?> enterprise = (Map<?, ?>) result.getData();
                    Object name = enterprise.get("enterpriseName");
                    return name != null ? name.toString() : null;
                }
            }
        } catch (Exception e) {
            logger.error("获取企业名称失败: entId={}", entId, e);
        }
        return null;
    }

    /**
     * 将金额转换为Wei单位
     */
    private BigInteger toWei(BigDecimal amount) {
        if (amount == null) {
            return BigInteger.ZERO;
        }
        return amount.multiply(new BigDecimal("1000000000000000000")).toBigInteger();
    }

    /**
     * 根据事件类型获取链上枚举值
     */
    private BigInteger getEventTypeValue(String eventType) {
        if (eventType == null) {
            return BigInteger.ZERO;
        }
        switch (eventType) {
            case CreditEvent.EVENT_TYPE_OVERDUE:
                return BigInteger.valueOf(0);
            case CreditEvent.EVENT_TYPE_DEFAULTER:
                return BigInteger.valueOf(1);
            case CreditEvent.EVENT_TYPE_EARLY_REPAY:
                return BigInteger.valueOf(2);
            case CreditEvent.EVENT_TYPE_ON_TIME_REPAY:
                return BigInteger.valueOf(3);
            case CreditEvent.EVENT_TYPE_GOODS_UNDAMAGED:
                return BigInteger.valueOf(4);
            case CreditEvent.EVENT_TYPE_STABLE_STORAGE:
                return BigInteger.valueOf(5);
            case CreditEvent.EVENT_TYPE_LOGISTICS_DEVIATION:
                return BigInteger.valueOf(6);
            case CreditEvent.EVENT_TYPE_RECEIPT_ABNORMAL:
                return BigInteger.valueOf(7);
            case CreditEvent.EVENT_TYPE_FREQUENT_CANCEL:
                return BigInteger.valueOf(8);
            default:
                return BigInteger.ZERO;
        }
    }

    /**
     * 计算事件数据哈希
     */
    private String calculateEventDataHash(Long entId, String eventType, String eventDesc) {
        String data = entId + "|" + eventType + "|" + eventDesc + "|" + System.currentTimeMillis();
        return data;
    }

    /**
     * 根据事件更新信用分
     */
    @Transactional(rollbackFor = Exception.class)
    private void updateCreditScoreByEvent(Long entId, Integer scoreChange) {
        EnterpriseCreditProfile profile = getCreditProfile(entId);
        if (profile == null) {
            return;
        }

        int newScore = profile.getCreditScore() + scoreChange;
        // 确保评分在有效范围内
        if (newScore > EnterpriseCreditProfile.MAX_SCORE) {
            newScore = EnterpriseCreditProfile.MAX_SCORE;
        }
        if (newScore < EnterpriseCreditProfile.MIN_SCORE) {
            newScore = EnterpriseCreditProfile.MIN_SCORE;
        }

        profile.setCreditScore(newScore);
        profile.setCreditLevel(EnterpriseCreditProfile.calculateLevel(newScore));
        profile.setLastEvalTime(LocalDateTime.now());

        // 更新逾期次数
        profile.setOverdueCount(countOverdueEvents(entId));

        updateCreditProfile(profile);

        // 检查是否触发黑名单
        if (checkBlacklist(entId)) {
            triggerBlacklist(entId);
        }
    }
}
