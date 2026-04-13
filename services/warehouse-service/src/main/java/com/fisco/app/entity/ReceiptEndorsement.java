package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 背书记录实体类
 *
 * 对应数据库表: t_receipt_endorsement
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_receipt_endorsement")
public class ReceiptEndorsement {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("receipt_id")
    private Long receiptId;

    @TableField("transferor_ent_id")
    private Long transferorEntId;

    @TableField("transferor_user_id")
    private Long transferorUserId;

    @TableField("transferee_ent_id")
    private Long transfereeEntId;

    @TableField("transferee_user_id")
    private Long transfereeUserId;

    @TableField("signature_hash")
    private String signatureHash;

    @TableField("tx_hash")
    private String txHash;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;

    // 状态常量
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_ACCEPTED = 2;
    public static final int STATUS_REJECTED = 3;
    public static final int STATUS_REVOKED = 4;
}
