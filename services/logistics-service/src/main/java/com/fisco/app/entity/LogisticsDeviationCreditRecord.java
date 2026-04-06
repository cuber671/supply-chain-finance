package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 物流偏航信用扣分记录实体类
 *
 * 用于记录偏航检测时信用扣分失败的待补偿记录
 * 对应数据库表: t_logistics_deviation_credit_record
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_logistics_deviation_credit_record")
public class LogisticsDeviationCreditRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 企业ID
     */
    @TableField("ent_id")
    private Long entId;

    /**
     * 物流订单号
     */
    @TableField("logistics_order_id")
    private String logisticsOrderId;

    /**
     * 偏航级别
     */
    @TableField("deviation_level")
    private Integer deviationLevel;

    /**
     * 偏航描述
     */
    @TableField("deviation_desc")
    private String deviationDesc;

    /**
     * 状态: 0=PENDING, 1=SUCCESS, 2=FAILED
     */
    @TableField("status")
    private Integer status;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 最后重试时间
     */
    @TableField("last_retry_time")
    private LocalDateTime lastRetryTime;

    // ==================== 状态常量 ====================

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;
}