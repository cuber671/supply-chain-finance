package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 链上交易审计记录实体
 * 用于关联 txHash 与 JWT 登录记录，实现链上行为审计回溯
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("blockchain_transaction_record")
public class BlockchainTransactionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String txHash;

    private String jti;

    private Long userId;

    private Long entId;

    private String blockchainAddress;

    private String operation;

    private String contractName;

    private String chainId;

    private String groupId;

    private LocalDateTime createTime;
}
