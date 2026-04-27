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

    // 状态常量
    public static final int STATUS_PENDING = 0;   // 待确认
    public static final int STATUS_SUCCESS = 1;  // 成功
    public static final int STATUS_FAILED = 2;   // 失败（可重试）
    public static final int STATUS_RETRY_EXHAUSTED = 3;  // 重试次数耗尽

    // 最大重试次数
    public static final int MAX_RETRY_COUNT = 3;

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long id;

    private String txHash;

    private String contractName;

    private String methodName;

    private String fromAddress;

    private String toAddress;

    private String inputData;

    private Long blockNumber;

    private Integer status;

    private LocalDateTime createTime;

    // 新增字段：关联用户/企业/JTI
    private Long userId;

    private Long entId;

    private String jti;

    // 重试次数
    private Integer retryCount;

    // 最后错误信息
    private String errorMsg;

    // 最后重试时间
    private LocalDateTime lastRetryTime;

    // 幂等键，用于防止重复上链操作
    private String idempotencyKey;
}
