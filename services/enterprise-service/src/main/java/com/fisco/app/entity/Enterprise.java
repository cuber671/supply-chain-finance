package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

/**
 * 企业实体类
 *
 * 对应数据库表: t_enterprise
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_enterprise")
public class Enterprise {

    /**
     * 企业ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long entId;

    /**
     * 企业法定全称
     */
    private String enterpriseName;

    /**
     * 统一社会信用代码
     */
    private String orgCode;

    /**
     * 企业实际地址
     */
    @TableField("local_address")
    private String localAddress;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 登录密码 (BCrypt加密)
     */
    private String password;

    /**
     * 交易密码 (BCrypt加密)
     */
    @TableField("pay_password")
    private String payPassword;

    /**
     * 业务角色: 1-核心企业, 2-现货交易平台, 3-供应商, 6-金融机构, 9-仓储方, 12-物流方
     */
    @TableField("ent_role")
    private Integer entRole;

    /**
     * 区块链公开地址 (0x开头)
     */
    @TableField("blockchain_address")
    private String blockchainAddress;

    /**
     * 加密存储的私钥 (AES加密)
     */
    @TableField("encrypted_private_key")
    private String encryptedPrivateKey;

    /**
     * 账户状态: 0-待审核, 1-正常, 2-冻结, 3-注销中, 4-已注销
     */
    private Integer status;

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

    /**
     * 状态常量
     */
    public static final int STATUS_PENDING = 0;      // 待审核
    public static final int STATUS_NORMAL = 1;       // 正常
    public static final int STATUS_FROZEN = 2;       // 冻结
    public static final int STATUS_CANCELLING = 3;   // 注销中
    public static final int STATUS_PENDING_CANCEL = 5; // 注销待审核
    public static final int STATUS_CANCELLED = 4;    // 已注销

    /**
     * 角色常量
     */
    public static final int ROLE_CORE_ENTERPRISE = 1;      // 核心企业
    public static final int ROLE_TRADING_PLATFORM = 2;    // 现货交易平台
    public static final int ROLE_SUPPLIER = 3;            // 供应商
    public static final int ROLE_FINANCIAL_INSTITUTION = 6; // 金融机构
    public static final int ROLE_WAREHOUSE = 9;           // 仓储方
    public static final int ROLE_LOGISTICS = 12;          // 物流方
}
