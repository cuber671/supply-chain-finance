package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 信用事件实体类
 *
 * 对应数据库表: t_credit_event
 *
 * 记录企业的信用相关事件，包括加分事件（准时还款、货物无损等）
 * 和扣分事件（逾期还款、物流偏航、仓单数据异常等）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_credit_event")
public class CreditEvent {

    /**
     * 信用事件ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 企业ID - 关联t_enterprise.ent_id
     */
    @TableField("ent_id")
    private Long entId;

    /**
     * 事件类型
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件等级
     */
    @TableField("event_level")
    private String eventLevel;

    /**
     * 事件描述
     */
    @TableField("event_desc")
    private String eventDesc;

    /**
     * 分值变化 - 正数为加分，负数为扣分
     */
    @TableField("score_change")
    private Integer scoreChange;

    /**
     * 关联模块 - WAREHOUSE-仓单, LOGISTICS-物流, FINANCE-金融
     */
    @TableField("related_module")
    private String relatedModule;

    /**
     * 关联业务ID
     */
    @TableField("related_id")
    private String relatedId;

    /**
     * 区块链交易哈希
     */
    @TableField("chain_tx_hash")
    private String chainTxHash;

    /**
     * 状态 - 0-已失效, 1-有效
     */
    private Integer status;

    /**
     * 事件上报时间
     */
    @TableField("report_time")
    private LocalDateTime reportTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 事件类型常量 ====================

    /**
     * 事件类型 - 逾期
     */
    public static final String EVENT_TYPE_OVERDUE = "OVERDUE";

    /**
     * 事件类型 - 违约
     */
    public static final String EVENT_TYPE_DEFAULTER = "DEFAULTER";

    /**
     * 事件类型 - 提前还款
     */
    public static final String EVENT_TYPE_EARLY_REPAY = "EARLY_REPAY";

    /**
     * 事件类型 - 准时还款
     */
    public static final String EVENT_TYPE_ON_TIME_REPAY = "ON_TIME_REPAY";

    /**
     * 事件类型 - 货物无损
     */
    public static final String EVENT_TYPE_GOODS_UNDAMAGED = "GOODS_UNDAMAGED";

    /**
     * 事件类型 - 入库稳定
     */
    public static final String EVENT_TYPE_STABLE_STORAGE = "STABLE_STORAGE";

    /**
     * 事件类型 - 物流偏航
     */
    public static final String EVENT_TYPE_LOGISTICS_DEVIATION = "LOGISTICS_DEVIATION";

    /**
     * 事件类型 - 仓单数据异常
     */
    public static final String EVENT_TYPE_RECEIPT_ABNORMAL = "RECEIPT_ABNORMAL";

    /**
     * 事件类型 - 频繁撤单
     */
    public static final String EVENT_TYPE_FREQUENT_CANCEL = "FREQUENT_CANCEL";

    // ==================== 事件等级常量 ====================

    /**
     * 事件等级 - 低
     */
    public static final String EVENT_LEVEL_LOW = "LOW";

    /**
     * 事件等级 - 中
     */
    public static final String EVENT_LEVEL_MEDIUM = "MEDIUM";

    /**
     * 事件等级 - 高
     */
    public static final String EVENT_LEVEL_HIGH = "HIGH";

    /**
     * 事件等级 - 严重
     */
    public static final String EVENT_LEVEL_SEVERE = "SEVERE";

    // ==================== 状态常量 ====================

    /**
     * 状态 - 已失效
     */
    public static final int STATUS_INVALID = 0;

    /**
     * 状态 - 有效
     */
    public static final int STATUS_VALID = 1;

    // ==================== 关联模块常量 ====================

    /**
     * 关联模块 - 仓单
     */
    public static final String MODULE_WAREHOUSE = "WAREHOUSE";

    /**
     * 关联模块 - 物流
     */
    public static final String MODULE_LOGISTICS = "LOGISTICS";

    /**
     * 关联模块 - 金融
     */
    public static final String MODULE_FINANCE = "FINANCE";

    // ==================== 便捷方法 ====================

    /**
     * 判断是否为扣分事件
     */
    public boolean isDeductionEvent() {
        return scoreChange != null && scoreChange < 0;
    }

    /**
     * 判断是否为加分事件
     */
    public boolean isBonusEvent() {
        return scoreChange != null && scoreChange > 0;
    }
}
