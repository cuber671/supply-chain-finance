package com.fisco.app.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 企业信用档案实体类
 *
 * 对应数据库表: t_enterprise_credit_profile
 *
 * 记录企业的实时信用状态，包括信用分、信用等级、授信额度等
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_enterprise_credit_profile")
public class EnterpriseCreditProfile {

    /**
     * 信用记录ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 企业ID - 关联t_enterprise.ent_id
     */
    @TableField("ent_id")
    private Long entId;

    /**
     * 信用评分 - 初始分800，范围300-1000
     */
    @TableField("credit_score")
    private Integer creditScore;

    /**
     * 信用等级 - AAA, AA, A, B, C, D
     */
    @TableField("credit_level")
    private String creditLevel;

    /**
     * 授信额度 - 平台允许该企业同时存在的未结清账款总额
     */
    @TableField("available_limit")
    private BigDecimal availableLimit;

    /**
     * 已用额度 - 当前未结清的应收款总和
     */
    @TableField("used_limit")
    private BigDecimal usedLimit;

    /**
     * 逾期次数 - 累计历史违约次数
     */
    @TableField("overdue_count")
    private Integer overdueCount;

    /**
     * 上次评估时间 - 系统自动跑分的日期
     */
    @TableField("last_eval_time")
    private LocalDateTime lastEvalTime;

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

    // ==================== 信用等级常量 ====================

    /**
     * 信用等级常量
     */
    public static final String LEVEL_AAA = "AAA";
    public static final String LEVEL_AA = "AA";
    public static final String LEVEL_A = "A";
    public static final String LEVEL_B = "B";
    public static final String LEVEL_C = "C";
    public static final String LEVEL_D = "D";

    /**
     * 初始信用分
     */
    public static final int DEFAULT_SCORE = 800;

    /**
     * 信用分范围
     */
    public static final int MIN_SCORE = 300;
    public static final int MAX_SCORE = 1000;

    /**
     * 信用分等级阈值
     */
    public static final int THRESHOLD_AAA = 900;
    public static final int THRESHOLD_AA = 800;
    public static final int THRESHOLD_A = 700;
    public static final int THRESHOLD_B = 600;
    public static final int THRESHOLD_C = 500;
    // 500分以下为D级

    /**
     * 根据信用分计算信用等级
     *
     * @param score 信用分
     * @return 信用等级
     */
    public static String calculateLevel(int score) {
        if (score >= THRESHOLD_AAA) {
            return LEVEL_AAA;
        } else if (score >= THRESHOLD_AA) {
            return LEVEL_AA;
        } else if (score >= THRESHOLD_A) {
            return LEVEL_A;
        } else if (score >= THRESHOLD_B) {
            return LEVEL_B;
        } else if (score >= THRESHOLD_C) {
            return LEVEL_C;
        } else {
            return LEVEL_D;
        }
    }
}
