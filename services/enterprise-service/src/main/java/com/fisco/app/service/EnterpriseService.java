package com.fisco.app.service;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fisco.app.entity.Enterprise;
import com.fisco.app.entity.InvitationCode;

/**
 * 企业业务服务接口
 *
 * 提供企业注册、登录、状态管理、邀请码等业务功能
 * 集成区块链上链服务完成企业身份存证
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface EnterpriseService {

    // ==================== 企业注册与登录 ====================

    /**
     * 企业注册
     *
     * @param username 用户名
     * @param password 登录密码
     * @param payPassword 交易密码
     * @param enterpriseName 企业名称
     * @param orgCode 统一社会信用代码
     * @param entRole 企业角色
     * @param localAddress 企业地址
     * @param contactPhone 联系电话
     * @return 注册成功的企业ID
     */
    Long registerEnterprise(String username, String password, String payPassword,
            String enterpriseName, String orgCode, Integer entRole,
            String localAddress, String contactPhone);

    /**
     * 企业登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回企业信息，失败返回null
     */
    Enterprise login(String username, String password);

    // ==================== 企业查询 ====================

    /**
     * 根据ID查询企业
     *
     * @param entId 企业ID
     * @return 企业信息
     */
    Enterprise getEnterpriseById(Long entId);

    /**
     * 验证企业是否为金融机构
     *
     * @param entId 企业ID
     * @return true=是金融机构(entRole=6), false=否
     */
    boolean isFinancialInstitution(Long entId);

    /**
     * 根据用户名查询企业
     *
     * @param username 用户名
     * @return 企业信息
     */
    Enterprise getEnterpriseByUsername(String username);

    /**
     * 根据统一社会信用代码查询企业
     *
     * @param orgCode 统一社会信用代码
     * @return 企业信息
     */
    Enterprise getEnterpriseByOrgCode(String orgCode);

    /**
     * 根据区块链地址查询企业
     *
     * @param blockchainAddress 区块链地址
     * @return 企业信息
     */
    Enterprise getEnterpriseByBlockchainAddress(String blockchainAddress);

    /**
     * 查询企业列表
     *
     * @param status 状态过滤（可选）
     * @param entRole 角色过滤（可选）
     * @return 企业列表
     */
    List<Enterprise> listEnterprises(Integer status, Integer entRole);

    /**
     * 分页获取企业列表
     *
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页数量
     * @param status 状态过滤（可选）
     * @param entRole 角色过滤（可选）
     * @return 分页企业列表
     */
    IPage<Enterprise> listEnterprisesPaginated(int pageNum, int pageSize, Integer status, Integer entRole);

    // ==================== 企业状态管理 ====================

    /**
     * 更新企业状态
     *
     * @param entId 企业ID
     * @param newStatus 新状态
     * @return 是否成功
     */
    boolean updateEnterpriseStatus(Long entId, Integer newStatus);

    /**
     * 冻结企业
     *
     * @param entId 企业ID
     * @return 是否成功
     */
    boolean freezeEnterprise(Long entId);

    /**
     * 解冻企业
     *
     * @param entId 企业ID
     * @return 是否成功
     */
    boolean unfreezeEnterprise(Long entId);

    /**
     * 修改登录密码
     *
     * @param entId 企业ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean updateLoginPassword(Long entId, String oldPassword, String newPassword);

    /**
     * 修改交易密码
     *
     * @param entId 企业ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean updatePayPassword(Long entId, String oldPassword, String newPassword);

    // ==================== 邀请码管理 ====================

    /**
     * 生成邀请码
     *
     * @param entId 企业ID
     * @param maxUses 最大使用次数
     * @param expireDays 过期天数
     * @param remark 备注
     * @return 生成的邀请码
     */
    String generateInvitationCode(Long entId, Integer maxUses, Integer expireDays, String remark);

    /**
     * 使用邀请码
     *
     * @param code 邀请码
     * @return 邀请企业ID
     */
    Long useInvitationCode(String code);

    /**
     * 删除邀请码
     *
     * @param codeId 邀请码ID
     * @return 是否成功
     */
    boolean deleteInvitationCode(Long codeId);

    /**
     * 查询企业的邀请码列表
     *
     * @param entId 企业ID
     * @return 邀请码列表
     */
    List<InvitationCode> listInvitationCodes(Long entId);

    /**
     * 验证邀请码是否有效
     *
     * @param code 邀请码
     * @return 是否有效
     */
    boolean validateInvitationCode(String code);

    /**
     * 根据邀请码获取邀请码详情
     *
     * @param code 邀请码
     * @return 邀请码对象，不存在则返回null
     */
    InvitationCode getInvitationCodeByCode(String code);

    // ==================== 企业注销管理 ====================

    /**
     * 发起注销申请
     * 校验链上资产余额是否清零，若有未结清资产则禁止申请
     *
     * @param entId 企业ID
     * @param reason 注销原因
     * @return 注销申请结果
     */
    CancellationResult applyCancellation(Long entId, String reason);

    /**
     * 撤回注销申请
     * 仅在管理员审批前有效，状态回滚至正常
     *
     * @param entId 企业ID
     * @return 是否成功
     */
    boolean revokeCancellation(Long entId);

    /**
     * 获取待审核注销企业列表
     *
     * @return 待审核注销企业列表
     */
    List<Enterprise> getPendingCancellationEnterprises();

    /**
     * 审核企业注销申请
     *
     * @param entId 企业ID
     * @param approved 审核结果：true-通过(设为已注销), false-拒绝(恢复正常)
     * @return 审核结果
     */
    boolean auditCancellation(Long entId, boolean approved);

    /**
     * 管理员强制注销企业（用于资产数据异常等特殊场景）
     *
     * @param entId 企业ID
     * @param reason 强制注销原因
     * @return 操作结果
     */
    boolean forceCancellation(Long entId, String reason);

    /**
     * 查询企业链上资产余额
     * 用于注销前校验
     *
     * @param entId 企业ID
     * @return 资产余额信息
     */
    AssetBalance checkAssetBalance(Long entId);

    // ==================== 区块链操作 ====================

    /**
     * 根据区块链地址查询链上企业信息
     *
     * @param blockchainAddress 区块链地址
     * @return 链上企业信息
     */
    EnterpriseInfo getEnterpriseFromChain(String blockchainAddress);

    /**
     * 根据统一社会信用代码查询链上企业地址
     *
     * @param orgCode 统一社会信用代码
     * @return 区块链地址
     */
    String getEnterpriseAddressByOrgCode(String orgCode);

    /**
     * 查询链上企业列表
     *
     * @return 区块链地址列表
     */
    List<String> getEnterpriseListFromChain();

    /**
     * 更新链上企业状态
     *
     * @param entId 企业ID
     * @param status 新状态
     * @return 交易哈希
     */
    String updateEnterpriseStatusOnChain(Long entId, Integer status);

    /**
     * 注册企业上链（审核通过后调用）
     *
     * @param entId 企业ID
     * @return 交易哈希
     */
    String registerEnterpriseOnChain(Long entId);

    /**
     * 更新链上企业信用评级
     *
     * @param entId 企业ID
     * @param rating 信用评级
     * @return 交易哈希
     */
    String updateCreditRatingOnChain(Long entId, String rating);

    /**
     * 设置链上企业授信额度
     *
     * @param entId 企业ID
     * @param creditLimit 授信额度
     * @return 交易哈希
     */
    String setCreditLimitOnChain(Long entId, Long creditLimit);

    // ==================== 内部类 ====================

    /**
     * 注销申请结果
     */
    class CancellationResult {
        private boolean success;
        private String message;
        private Long entId;
        private String reason;
        private java.time.LocalDateTime applyTime;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public java.time.LocalDateTime getApplyTime() { return applyTime; }
        public void setApplyTime(java.time.LocalDateTime applyTime) { this.applyTime = applyTime; }
    }

    /**
     * 资产余额信息
     */
    class AssetBalance {
        private Long entId;
        private String blockchainAddress;
        private long warehouseReceiptCount;  // 仓单数量
        private long billCount;              // 票据数量
        private long receivableCount;        // 应收款数量
        private long totalAssets;            // 资产总数

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getBlockchainAddress() { return blockchainAddress; }
        public void setBlockchainAddress(String blockchainAddress) { this.blockchainAddress = blockchainAddress; }
        public long getWarehouseReceiptCount() { return warehouseReceiptCount; }
        public void setWarehouseReceiptCount(long warehouseReceiptCount) { this.warehouseReceiptCount = warehouseReceiptCount; }
        public long getBillCount() { return billCount; }
        public void setBillCount(long billCount) { this.billCount = billCount; }
        public long getReceivableCount() { return receivableCount; }
        public void setReceivableCount(long receivableCount) { this.receivableCount = receivableCount; }
        public long getTotalAssets() { return totalAssets; }
        public void setTotalAssets(long totalAssets) { this.totalAssets = totalAssets; }

        public boolean hasAssets() {
            return totalAssets > 0;
        }
    }

    /**
     * 区块链企业信息
     */
    class EnterpriseInfo {
        private String address;
        private String creditCode;
        private BigInteger role;
        private BigInteger status;
        private BigInteger creditLimit;
        private BigInteger creditRating;

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getCreditCode() { return creditCode; }
        public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
        public BigInteger getRole() { return role; }
        public void setRole(BigInteger role) { this.role = role; }
        public BigInteger getStatus() { return status; }
        public void setStatus(BigInteger status) { this.status = status; }
        public BigInteger getCreditLimit() { return creditLimit; }
        public void setCreditLimit(BigInteger creditLimit) { this.creditLimit = creditLimit; }
        public BigInteger getCreditRating() { return creditRating; }
        public void setCreditRating(BigInteger creditRating) { this.creditRating = creditRating; }
    }
}
