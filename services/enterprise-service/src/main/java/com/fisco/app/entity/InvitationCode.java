package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 企业邀请码实体类
 *
 * 对应数据库表: t_invitation_code
 *
 * 支持"一企多码"，可以追踪谁邀请了谁、码是否过期、被使用了多少次
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_invitation_code")
public class InvitationCode {

    /**
     * 邀请码ID
     */
    @TableId(type = IdType.AUTO)
    private Long codeId;

    /**
     * 邀请企业ID
     */
    @TableField("inviter_ent_id")
    private Long inviterEntId;

    /**
     * 邀请码字符串 (6-10位大写字母+数字)
     */
    private String code;

    /**
     * 最大使用次数
     */
    @TableField("max_uses")
    private Integer maxUses;

    /**
     * 已使用次数
     */
    @TableField("used_count")
    private Integer usedCount;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 状态: 0-禁用, 1-启用, 2-已用罄
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 状态常量
     */
    public static final int STATUS_DISABLED = 0;   // 禁用
    public static final int STATUS_ENABLED = 1;     // 启用
    public static final int STATUS_EXHAUSTED = 2;  // 已用罄

    /**
     * 检查邀请码是否有效
     */
    public boolean isValid() {
        if (status == null || status != STATUS_ENABLED) {
            return false;
        }
        if (usedCount != null && usedCount >= maxUses) {
            return false;
        }
        if (expireTime != null && LocalDateTime.now().isAfter(expireTime)) {
            return false;
        }
        return true;
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 检查是否已用罄
     */
    public boolean isExhausted() {
        return usedCount != null && usedCount >= maxUses;
    }
}
