package com.fisco.app.service.impl;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fisco.app.entity.Enterprise;
import com.fisco.app.entity.InvitationCode;
import com.fisco.app.feign.BlockchainFeignClient;
import com.fisco.app.feign.CreditFeignClient;
import com.fisco.app.mapper.EnterpriseMapper;
import com.fisco.app.mapper.InvitationCodeMapper;
import com.fisco.app.service.EnterpriseService;
import com.fisco.app.util.Result;

/**
 * 企业业务服务实现类
 *
 * 实现企业注册、登录、状态管理等业务功能
 * 集成区块链上链服务完成企业身份存证
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class EnterpriseServiceImpl implements EnterpriseService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ==================== Java-Solidity 枚举转换 ====================

    // ==================== 注入 ====================

    @Autowired
    private EnterpriseMapper enterpriseMapper;

    @Autowired
    private InvitationCodeMapper invitationCodeMapper;

    @Autowired(required = false)
    private BlockchainFeignClient blockchainFeignClient;

    @Autowired(required = false)
    private CreditFeignClient creditFeignClient;

    @Value("${encrypt.aes.key:}")
    private String aesKey;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 企业注册与登录 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerEnterprise(String username, String password, String payPassword,
            String enterpriseName, String orgCode, Integer entRole,
            String localAddress, String contactPhone) {

        // 参数校验
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("登录密码不能为空");
        }
        validatePassword(password);
        if (payPassword == null || payPassword.isEmpty()) {
            throw new IllegalArgumentException("交易密码不能为空");
        }
        if (enterpriseName == null || enterpriseName.isEmpty()) {
            throw new IllegalArgumentException("企业名称不能为空");
        }
        if (orgCode == null || orgCode.isEmpty()) {
            throw new IllegalArgumentException("统一社会信用代码不能为空");
        }

        // 检查用户名是否已存在（已注销的企业可重新注册）
        Enterprise existUser = getEnterpriseByUsername(username);
        if (existUser != null && existUser.getStatus() != Enterprise.STATUS_CANCELLED) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 检查信用代码是否已存在（已注销的企业可重新注册）
        Enterprise existOrg = getEnterpriseByOrgCode(orgCode);
        if (existOrg != null && existOrg.getStatus() != Enterprise.STATUS_CANCELLED) {
            throw new IllegalArgumentException("统一社会信用代码已被注册");
        }

        // 生成企业ID（雪花算法由MyBatis-Plus自动处理）
        Enterprise enterprise = new Enterprise();
        enterprise.setEnterpriseName(enterpriseName);
        enterprise.setOrgCode(orgCode);
        enterprise.setLocalAddress(localAddress);
        enterprise.setContactPhone(contactPhone);
        enterprise.setUsername(username);
        // 密码加密存储
        enterprise.setPassword(passwordEncoder.encode(password));
        enterprise.setPayPassword(passwordEncoder.encode(payPassword));
        enterprise.setEntRole(entRole != null ? entRole : Enterprise.ROLE_SUPPLIER);
        enterprise.setStatus(Enterprise.STATUS_PENDING); // 待审核状态

        try {
            // 通过 Feign 调用 fisco-gateway-service 生成密钥对
            if (blockchainFeignClient == null) {
                logger.warn("区块链网关服务不可用，使用空地址");
                enterprise.setBlockchainAddress(null);
                enterprise.setEncryptedPrivateKey(null);
            } else {
                var result = blockchainFeignClient.generateKeyPair();
                if (result == null || result.getCode() != 0 || result.getData() == null) {
                    logger.warn("密钥对生成失败，使用空地址: result={}", result);
                    enterprise.setBlockchainAddress(null);
                    enterprise.setEncryptedPrivateKey(null);
                } else {
                    String blockchainAddress = result.getData().getAddress();
                    String privateKey = result.getData().getPrivateKey();

                    // 加密存储私钥
                    String encryptedPrivateKey = privateKey != null
                        ? encryptWithAes(privateKey)
                        : null;

                    enterprise.setBlockchainAddress(blockchainAddress);
                    enterprise.setEncryptedPrivateKey(encryptedPrivateKey);

                    logger.info("为企业生成区块链地址: {}", blockchainAddress);
                }
            }
        } catch (Exception e) {
            logger.warn("生成区块链地址失败，使用空地址: {}", e.getMessage());
            // 区块链地址可后续补充
            enterprise.setBlockchainAddress(null);
            enterprise.setEncryptedPrivateKey(null);
        }

        // 保存到数据库
        enterpriseMapper.insert(enterprise);

        return enterprise.getEntId();
    }

    @Override
    public Enterprise login(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        Enterprise enterprise = getEnterpriseByUsername(username);
        if (enterprise == null) {
            return null;
        }

        // 检查企业状态
        if (enterprise.getStatus() == Enterprise.STATUS_PENDING) {
            throw new IllegalStateException("账户待审核，请等待管理员审核通过后登录");
        }
        if (enterprise.getStatus() == Enterprise.STATUS_FROZEN) {
            throw new IllegalStateException("账户已被冻结");
        }
        if (enterprise.getStatus() == Enterprise.STATUS_CANCELLED) {
            throw new IllegalStateException("账户已注销");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, enterprise.getPassword())) {
            return null;
        }

        return enterprise;
    }

    // ==================== 企业查询 ====================

    @Override
    public Enterprise getEnterpriseById(Long entId) {
        if (entId == null) {
            return null;
        }
        return enterpriseMapper.selectById(entId);
    }

    @Override
    public boolean isFinancialInstitution(Long entId) {
        if (entId == null) {
            return false;
        }
        Enterprise ent = enterpriseMapper.selectById(entId);
        return ent != null && ent.getEntRole() == 6;
    }

    @Override
    public boolean isLogisticsEnterprise(Long entId) {
        if (entId == null) {
            return false;
        }
        Enterprise ent = enterpriseMapper.selectById(entId);
        return ent != null && ent.getEntRole() == Enterprise.ROLE_LOGISTICS;
    }

    @Override
    public Enterprise getEnterpriseByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getUsername, username);
        return enterpriseMapper.selectOne(wrapper);
    }

    @Override
    public Enterprise getEnterpriseByOrgCode(String orgCode) {
        if (orgCode == null || orgCode.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getOrgCode, orgCode);
        return enterpriseMapper.selectOne(wrapper);
    }

    @Override
    public Enterprise getEnterpriseByBlockchainAddress(String blockchainAddress) {
        if (blockchainAddress == null || blockchainAddress.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getBlockchainAddress, blockchainAddress);
        return enterpriseMapper.selectOne(wrapper);
    }

    @Override
    public List<Enterprise> listEnterprises(Integer status, Integer entRole) {
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Enterprise::getStatus, status);
        }
        if (entRole != null) {
            wrapper.eq(Enterprise::getEntRole, entRole);
        }
        wrapper.orderByDesc(Enterprise::getCreateTime);
        return enterpriseMapper.selectList(wrapper);
    }

    @Override
    public IPage<Enterprise> listEnterprisesPaginated(int pageNum, int pageSize, Integer status, Integer entRole) {
        Page<Enterprise> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Enterprise::getStatus, status);
        }
        if (entRole != null) {
            wrapper.eq(Enterprise::getEntRole, entRole);
        }
        wrapper.orderByDesc(Enterprise::getCreateTime);
        return enterpriseMapper.selectPage(page, wrapper);
    }

    // ==================== 企业状态管理 ====================

    /**
     * 验证企业状态转换是否合法
     * @param currentStatus 当前状态
     * @param newStatus 新状态
     * @return 是否合法
     */
    private boolean isValidStatusTransition(int currentStatus, int newStatus) {
        // 已注销企业不允许任何状态转换
        if (currentStatus == Enterprise.STATUS_CANCELLED) {
            return false;
        }
        // 状态未变化视为合法
        if (currentStatus == newStatus) {
            return true;
        }
        switch (currentStatus) {
            case Enterprise.STATUS_PENDING: // 待审核
                // 待审核 -> 正常(审核通过) 或 冻结(审核拒绝)
                return newStatus == Enterprise.STATUS_NORMAL || newStatus == Enterprise.STATUS_FROZEN;
            case Enterprise.STATUS_NORMAL: // 正常
                // 正常 -> 冻结 或 注销待审核
                return newStatus == Enterprise.STATUS_FROZEN || newStatus == Enterprise.STATUS_PENDING_CANCEL;
            case Enterprise.STATUS_FROZEN: // 冻结
                // 冻结 -> 正常(解冻)
                return newStatus == Enterprise.STATUS_NORMAL;
            case Enterprise.STATUS_PENDING_CANCEL: // 注销待审核
                // 注销待审核 -> 已注销(审核通过) 或 正常(审核拒绝/撤销)
                return newStatus == Enterprise.STATUS_CANCELLED || newStatus == Enterprise.STATUS_NORMAL;
            default:
                return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEnterpriseStatus(Long entId, Integer newStatus) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        int currentStatus = enterprise.getStatus();
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                String.format("非法的企业状态转换: 当前状态=%d, 目标状态=%d", currentStatus, newStatus));
        }

        enterprise.setStatus(newStatus);
        enterpriseMapper.updateById(enterprise);

        logger.info("企业状态已更新: entId={}, oldStatus={}, newStatus={}", entId, currentStatus, newStatus);
        return true;
    }

    @Override
    public boolean freezeEnterprise(Long entId) {
        return updateEnterpriseStatus(entId, Enterprise.STATUS_FROZEN);
    }

    @Override
    public boolean unfreezeEnterprise(Long entId) {
        return updateEnterpriseStatus(entId, Enterprise.STATUS_NORMAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLoginPassword(Long entId, String oldPassword, String newPassword) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, enterprise.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        // FIX: 新密码强度校验
        validatePassword(newPassword);

        // 更新密码
        enterprise.setPassword(passwordEncoder.encode(newPassword));
        enterpriseMapper.updateById(enterprise);

        logger.info("企业登录密码已更新: entId={}", entId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePayPassword(Long entId, String oldPassword, String newPassword) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, enterprise.getPayPassword())) {
            throw new IllegalArgumentException("原交易密码错误");
        }

        // FIX: 新密码强度校验
        validatePassword(newPassword);

        // 更新交易密码
        enterprise.setPayPassword(passwordEncoder.encode(newPassword));
        enterpriseMapper.updateById(enterprise);

        logger.info("企业交易密码已重置: entId={}", entId);
        return true;
    }

    // ==================== 邀请码管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateInvitationCode(Long entId, Integer maxUses, Integer expireDays, String remark) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 生成唯一邀请码
        String code;
        do {
            code = generateUniqueCode();
        } while (getInvitationCodeByCode(code) != null);

        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setInviterEntId(entId);
        invitationCode.setCode(code);
        invitationCode.setMaxUses(maxUses != null ? maxUses : 1);
        invitationCode.setUsedCount(0);
        if (expireDays != null && expireDays > 0) {
            invitationCode.setExpireTime(LocalDateTime.now().plusDays(expireDays));
        }
        invitationCode.setStatus(InvitationCode.STATUS_ENABLED);
        invitationCode.setRemark(remark);

        invitationCodeMapper.insert(invitationCode);

        logger.info("生成邀请码: entId={}, code={}", entId, code);
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long useInvitationCode(String code) {
        InvitationCode invitationCode = getInvitationCodeByCode(code);
        if (invitationCode == null) {
            throw new IllegalArgumentException("邀请码不存在");
        }

        // 验证邀请码有效性（仅做状态检查，原子更新保证并发安全）
        if (invitationCode.isExpired()) {
            throw new IllegalArgumentException("邀请码已过期");
        }
        if (invitationCode.getStatus() != InvitationCode.STATUS_ENABLED) {
            throw new IllegalArgumentException("邀请码已禁用");
        }
        if (invitationCode.isExhausted()) {
            throw new IllegalArgumentException("邀请码已使用完毕");
        }

        // FIX: 使用原子更新替代read-modify-write，防止并发超用
        int updated = invitationCodeMapper.incrementUsedCountAtomically(
                code,
                InvitationCode.STATUS_ENABLED,
                InvitationCode.STATUS_EXHAUSTED
        );

        if (updated == 0) {
            // 原子更新失败，可能是并发冲突或已用尽
            throw new IllegalArgumentException("邀请码使用失败，可能已被其他请求占用");
        }

        logger.info("使用邀请码: code={}, inviterEntId={}", code, invitationCode.getInviterEntId());
        return invitationCode.getInviterEntId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteInvitationCode(Long codeId) {
        InvitationCode invitationCode = invitationCodeMapper.selectById(codeId);
        if (invitationCode == null) {
            throw new IllegalArgumentException("邀请码不存在");
        }

        invitationCode.setStatus(InvitationCode.STATUS_DISABLED);
        invitationCodeMapper.updateById(invitationCode);

        logger.info("删除邀请码: codeId={}", codeId);
        return true;
    }

    @Override
    public List<InvitationCode> listInvitationCodes(Long entId) {
        LambdaQueryWrapper<InvitationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvitationCode::getInviterEntId, entId);
        wrapper.orderByDesc(InvitationCode::getCreateTime);
        return invitationCodeMapper.selectList(wrapper);
    }

    @Override
    public boolean validateInvitationCode(String code) {
        InvitationCode invitationCode = getInvitationCodeByCode(code);
        return invitationCode != null && invitationCode.isValid();
    }

    // ==================== 企业注销管理 ====================

    @Override
    public CancellationResult applyCancellation(Long entId, String password, String reason) {
        CancellationResult result = new CancellationResult();

        // 查询企业信息
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            result.setSuccess(false);
            result.setMessage("企业不存在");
            return result;
        }

        // 校验密码
        if (password == null || !passwordEncoder.matches(password, enterprise.getPassword())) {
            result.setSuccess(false);
            result.setMessage("密码验证失败");
            return result;
        }

        // 检查企业状态是否为正常
        if (enterprise.getStatus() != Enterprise.STATUS_NORMAL) {
            result.setSuccess(false);
            result.setMessage("企业状态异常，无法申请注销。当前状态：" + getStatusName(enterprise.getStatus()));
            return result;
        }

        // 校验链上资产余额
        AssetBalance assetBalance = checkAssetBalance(entId);
        // 只有当企业确实存在未结清资产时才阻止注销
        // 注意: checkAssetBalance 目前未完全实现，临时返回0，若后续完善应查询链上真实资产
        if (assetBalance.hasAssets()) {
            result.setSuccess(false);
            result.setMessage("企业存在未结清资产，无法申请注销。仓单：" + assetBalance.getWarehouseReceiptCount()
                    + "，票据：" + assetBalance.getBillCount()
                    + "，应收款：" + assetBalance.getReceivableCount());
            return result;
        }

        // 更新企业状态为注销待审核（等待管理员审核）
        enterprise.setStatus(Enterprise.STATUS_PENDING_CANCEL);
        enterpriseMapper.updateById(enterprise);

        result.setSuccess(true);
        result.setMessage("注销申请已提交，等待管理员审核");
        result.setEntId(entId);
        result.setReason(reason);
        result.setApplyTime(LocalDateTime.now());

        logger.info("企业注销申请成功: entId={}, reason={}", entId, reason);
        return result;
    }

    @Override
    public boolean revokeCancellation(Long entId, String password) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 校验密码
        if (password == null || !passwordEncoder.matches(password, enterprise.getPassword())) {
            throw new IllegalArgumentException("密码验证失败");
        }

        // 仅注销待审核状态可以撤回
        if (enterprise.getStatus() != Enterprise.STATUS_PENDING_CANCEL) {
            throw new IllegalArgumentException("只有注销待审核的企业才能撤回申请");
        }

        // 更新状态为正常
        enterprise.setStatus(Enterprise.STATUS_NORMAL);
        enterpriseMapper.updateById(enterprise);

        logger.info("企业注销申请已撤回: entId={}", entId);
        return true;
    }

    @Override
    public List<Enterprise> getPendingCancellationEnterprises() {
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enterprise::getStatus, Enterprise.STATUS_PENDING_CANCEL);
        return enterpriseMapper.selectList(wrapper);
    }

    @Override
    public CancellationAuditResult auditCancellation(Long entId, boolean approved) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }
        if (enterprise.getStatus() != Enterprise.STATUS_PENDING_CANCEL) {
            throw new IllegalArgumentException("该企业不是注销待审核状态，无法审核");
        }

        CancellationAuditResult result = new CancellationAuditResult();
        result.setEntId(entId);

        // 审核通过设为已注销(4)，审核拒绝恢复正常(1)
        int newStatus = approved ? Enterprise.STATUS_CANCELLED : Enterprise.STATUS_NORMAL;
        enterprise.setStatus(newStatus);
        enterpriseMapper.updateById(enterprise);

        result.setNewStatus(newStatus);
        result.setAction(approved ? "通过" : "拒绝");

        // FIX: 审核通过时同步更新链上企业状态
        String txHash = null;
        if (approved && enterprise.getBlockchainAddress() != null) {
            try {
                txHash = updateEnterpriseStatusOnChain(entId, newStatus);
                if (txHash == null) {
                    String errMsg = "企业链上状态同步失败: entId=" + entId + ", newStatus=" + newStatus
                        + ", 请稍后重试或联系技术支持";
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String errMsg = "企业链上状态同步异常: entId=" + entId + ", error=" + e.getMessage();
                logger.error(errMsg, e);
                throw new RuntimeException(errMsg, e);
            }
        }
        result.setTxHash(txHash);

        // 审核通过时同步锁定该企业的信用额度
        if (approved && creditFeignClient != null) {
            try {
                Map<String, Object> lockResult = creditFeignClient.lockCreditLimit();
                if (lockResult != null && "0".equals(String.valueOf(lockResult.get("code")))) {
                    logger.info("企业注销后信用额度已锁定: entId={}", entId);
                }
            } catch (Exception e) {
                logger.error("企业信用额度锁定失败: entId={}, error={}", entId, e.getMessage());
            }
        }

        result.setSuccess(true);
        result.setMessage("审核完成");
        logger.info("企业注销审核完成: entId={}, approved={}, newStatus={}, txHash={}", entId, approved, newStatus, txHash);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forceCancellation(Long entId, String reason) {
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }

        // 强制注销：直接将状态设为已注销，跳过资产检查
        enterprise.setStatus(Enterprise.STATUS_CANCELLED);
        enterpriseMapper.updateById(enterprise);

        // 同步更新链上企业状态
        if (enterprise.getBlockchainAddress() != null) {
            try {
                String txHash = updateEnterpriseStatusOnChain(entId, Enterprise.STATUS_CANCELLED);
                if (txHash == null) {
                    String errMsg = "强制注销链上状态同步失败: entId=" + entId;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String errMsg = "强制注销链上状态同步异常: entId=" + entId + ", error=" + e.getMessage();
                logger.error(errMsg, e);
                throw new RuntimeException(errMsg, e);
            }
        }

        logger.warn("管理员强制注销企业: entId={}, reason={}", entId, reason);
        return true;
    }

    @Override
    public AssetBalance checkAssetBalance(Long entId) {
        AssetBalance balance = new AssetBalance();
        balance.setEntId(entId);

        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null || enterprise.getBlockchainAddress() == null) {
            balance.setTotalAssets(0);
            return balance;
        }

        balance.setBlockchainAddress(enterprise.getBlockchainAddress());

        // 【修复SC-001-02】调用区块链服务查询真实资产数量
        String ownerHash = enterprise.getBlockchainAddress();

        // 1. 查询仓单数量
        if (blockchainFeignClient != null) {
            try {
                Result<List<String>> receiptResult = blockchainFeignClient.getReceiptIdsByOwner(ownerHash);
                if (receiptResult != null && receiptResult.getCode() == 0 && receiptResult.getData() != null) {
                    balance.setWarehouseReceiptCount(receiptResult.getData().size());
                }
            } catch (Exception e) {
                logger.warn("查询仓单数量失败: entId={}, blockchainAddress={}", entId, ownerHash, e);
            }
        }

        // 2. 票据数量 - 暂时设置为0（enterprise-service无可用的链上票据查询接口）
        balance.setBillCount(0);

        // 3. 应收款数量 - 暂时设置为0（enterprise-service无可用的链上应收款查询接口）
        balance.setReceivableCount(0);

        // 计算资产总数
        balance.setTotalAssets(balance.getWarehouseReceiptCount() + balance.getBillCount() + balance.getReceivableCount());

        return balance;
    }

    // ==================== 区块链操作 ====================

    @Override
    public EnterpriseInfo getEnterpriseFromChain(String blockchainAddress) {
        if (blockchainFeignClient == null) {
            logger.warn("区块链网关服务不可用，无法查询链上企业信息");
            return null;
        }
        try {
            var result = blockchainFeignClient.getEnterprise(blockchainAddress);
            if (result != null && result.getCode() == 0 && result.getData() != null) {
                Map<String, Object> data = result.getData();
                EnterpriseInfo info = new EnterpriseInfo();
                info.setAddress(blockchainAddress);
                info.setCreditCode((String) data.get("creditCode"));
                info.setRole(data.get("role") != null ? new BigInteger(data.get("role").toString()) : null);
                info.setStatus(data.get("status") != null ? new BigInteger(data.get("status").toString()) : null);
                // 【YX-03修复】合约getEnterprise()对creditRating和creditLimit返回0（这些值存储在metadataHash中但合约实现未解析）
                // Java端不应从链上获取这些值，因为它们总是返回0
                // 如需creditRating和creditLimit，应从Java DB查询（Enterprise实体有独立字段存储）
                // 注意：链上企业信息(creditCode, role, status)可从链上获取，但rating和limit应从DB获取
                info.setCreditLimit(null); // 不从链上获取（返回0无意义）
                info.setCreditRating(null); // 不从链上获取（返回0无意义）
                return info;
            }
        } catch (Exception e) {
            logger.error("查询链上企业信息失败: blockchainAddress={}", blockchainAddress, e);
        }
        return null;
    }

    @Override
    public String getEnterpriseAddressByOrgCode(String orgCode) {
        if (blockchainFeignClient == null) {
            logger.warn("区块链网关服务不可用，无法根据信用代码查询企业地址");
            return null;
        }
        try {
            var result = blockchainFeignClient.getEnterpriseByCreditCode(orgCode);
            if (result != null && result.getCode() == 0 && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            logger.error("根据信用代码查询链上企业地址失败: orgCode={}", orgCode, e);
        }
        return null;
    }

    @Override
    public List<String> getEnterpriseListFromChain() {
        if (blockchainFeignClient == null) {
            logger.warn("区块链网关服务不可用，无法查询链上企业列表");
            return List.of();
        }
        try {
            var result = blockchainFeignClient.getEnterpriseList();
            if (result != null && result.getCode() == 0 && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            logger.error("查询链上企业列表失败", e);
        }
        return List.of();
    }

    @Override
    public String updateEnterpriseStatusOnChain(Long entId, Integer status) {
        if (blockchainFeignClient == null) {
            logger.warn("区块链网关服务不可用，无法更新链上企业状态");
            return null;
        }
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null || enterprise.getBlockchainAddress() == null) {
            throw new IllegalArgumentException("企业不存在或未上链");
        }
        try {
            BlockchainFeignClient.EnterpriseStatusRequest request = new BlockchainFeignClient.EnterpriseStatusRequest();
            request.setEnterpriseAddress(enterprise.getBlockchainAddress());
            // 链下: STATUS_CANCELLED=4, STATUS_PENDING_CANCEL=5
            // 链上: Deleted=5, PendingDeletion=4
            int chainStatus = mapToChainStatus(status);
            request.setNewStatus(chainStatus);
            var result = blockchainFeignClient.updateEnterpriseStatus(request);
            // 【P2-7修复】区块链响应码检查，失败时抛出异常保持一致性
            if (result == null || result.getCode() != 0) {
                String errMsg = "更新链上企业状态失败: entId=" + entId + ", result=" + result;
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
            logger.info("更新链上企业状态成功: entId={}, dbStatus={}, chainStatus={}, txHash={}", entId, status, chainStatus, result.getData());
            return result.getData();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新链上企业状态异常: entId={}", entId, e);
            throw new RuntimeException("更新链上企业状态异常: " + e.getMessage(), e);
        }
    }

    /**
     * 将链下企业状态映射到链上状态
     * 链下状态定义(Enterprise.STATUS_*):
     *   STATUS_PENDING=0, STATUS_NORMAL=1, STATUS_FROZEN=2,
     *   STATUS_CANCELLED=4, STATUS_PENDING_CANCEL=5
     * 链上状态定义(EnterpriseRegistryV2.EnterpriseStatus):
     *   Pending=0, Normal=1, Frozen=2, Cancelled=4, PendingCancel=5
     * @param dbStatus 链下状态值
     * @return 链上状态值
     */
    private int mapToChainStatus(int dbStatus) {
        switch (dbStatus) {
            case Enterprise.STATUS_PENDING:
                return 0;  // Pending
            case Enterprise.STATUS_NORMAL:
                return 1;  // Active
            case Enterprise.STATUS_FROZEN:
                return 2;  // Suspended
            case Enterprise.STATUS_CANCELLED:
                return 4;  // Cancelled (链下4 → 链上4)
            case Enterprise.STATUS_PENDING_CANCEL:
                return 5;  // PendingCancel (链下5 → 链上5)
            default:
                return dbStatus;
        }
    }

    @Override
    public String registerEnterpriseOnChain(Long entId) {
        if (blockchainFeignClient == null) {
            logger.warn("区块链网关服务不可用，无法注册企业上链");
            return null;
        }
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null) {
            throw new IllegalArgumentException("企业不存在");
        }
        // 如果没有区块链地址，自动生成并回填到数据库
        if (enterprise.getBlockchainAddress() == null) {
            try {
                var keyPairResult = blockchainFeignClient.generateKeyPair();
                if (keyPairResult == null || keyPairResult.getCode() != 0 || keyPairResult.getData() == null) {
                    throw new RuntimeException("自动生成区块链地址失败: " + keyPairResult);
                }
                String address = keyPairResult.getData().getAddress();
                String privateKey = keyPairResult.getData().getPrivateKey();
                String encryptedPrivateKey = privateKey != null ? encryptWithAes(privateKey) : null;
                enterprise.setBlockchainAddress(address);
                enterprise.setEncryptedPrivateKey(encryptedPrivateKey);
                enterpriseMapper.updateById(enterprise);
                logger.info("自动生成区块链地址并回填: entId={}, address={}", entId, address);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("自动生成区块链地址异常: " + e.getMessage(), e);
            }
        }
        try {
            BlockchainFeignClient.EnterpriseRegisterRequest request = new BlockchainFeignClient.EnterpriseRegisterRequest();
            request.setIdempotencyKey("enterprise_register_" + entId);
            request.setEnterpriseAddress(enterprise.getBlockchainAddress());
            request.setCreditCode(enterprise.getOrgCode());
            request.setRole(enterprise.getEntRole());
            request.setMetadataHash("0x" + String.format("%064x", entId));
            var result = blockchainFeignClient.registerEnterprise(request);
            // 【P2-7修复】区块链响应码检查，失败时抛出异常保持一致性
            if (result == null || result.getCode() != 0) {
                String errMsg = "注册企业上链失败: entId=" + entId + ", result=" + result;
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
            logger.info("注册企业上链成功: entId={}, txHash={}", entId, result.getData());
            return result.getData();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("注册企业上链异常: entId={}", entId, e);
            throw new RuntimeException("注册企业上链异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String updateCreditRatingOnChain(Long entId, String rating) {
        if (blockchainFeignClient == null) {
            logger.warn("区块链网关服务不可用，无法更新链上企业信用评级");
            return null;
        }
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null || enterprise.getBlockchainAddress() == null) {
            throw new IllegalArgumentException("企业不存在或未上链");
        }
        try {
            BlockchainFeignClient.EnterpriseCreditRatingRequest request = new BlockchainFeignClient.EnterpriseCreditRatingRequest();
            request.setEnterpriseAddress(enterprise.getBlockchainAddress());
            request.setNewRating(rating != null ? Integer.parseInt(rating) : 0);
            var result = blockchainFeignClient.updateCreditRating(request);
            // 【P2-7修复】区块链响应码检查，失败时抛出异常保持一致性
            if (result == null || result.getCode() != 0) {
                String errMsg = "更新链上企业信用评级失败: entId=" + entId + ", result=" + result;
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
            logger.info("更新链上企业信用评级成功: entId={}, rating={}, txHash={}", entId, rating, result.getData());
            return result.getData();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新链上企业信用评级异常: entId={}", entId, e);
            throw new RuntimeException("更新链上企业信用评级异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String setCreditLimitOnChain(Long entId, Long creditLimit) {
        if (blockchainFeignClient == null) {
            logger.warn("区块链网关服务不可用，无法设置链上企业授信额度");
            return null;
        }
        Enterprise enterprise = getEnterpriseById(entId);
        if (enterprise == null || enterprise.getBlockchainAddress() == null) {
            throw new IllegalArgumentException("企业不存在或未上链");
        }
        try {
            BlockchainFeignClient.EnterpriseCreditLimitRequest request = new BlockchainFeignClient.EnterpriseCreditLimitRequest();
            request.setEnterpriseAddress(enterprise.getBlockchainAddress());
            request.setNewLimit(creditLimit);
            var result = blockchainFeignClient.setCreditLimit(request);
            // 【P2-7修复】区块链响应码检查，失败时抛出异常保持一致性
            if (result == null || result.getCode() != 0) {
                String errMsg = "设置链上企业授信额度失败: entId=" + entId + ", result=" + result;
                logger.error(errMsg);
                throw new RuntimeException(errMsg);
            }
            logger.info("设置链上企业授信额度成功: entId={}, limit={}, txHash={}", entId, creditLimit, result.getData());
            return result.getData();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("设置链上企业授信额度异常: entId={}", entId, e);
            throw new RuntimeException("设置链上企业授信额度异常: " + e.getMessage(), e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 区块链企业信息
     */
    public static class EnterpriseInfo extends EnterpriseService.EnterpriseInfo {
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

    @Override
    public InvitationCode getInvitationCodeByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<InvitationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvitationCode::getCode, code);
        return invitationCodeMapper.selectOne(wrapper);
    }

    private String generateUniqueCode() {
        // 生成8位大写字母+数字组合（符合schema声明6-10位范围，约 32^8 ≈ 10^12 种组合）
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = SECURE_RANDOM.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 获取企业状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case Enterprise.STATUS_PENDING:
                return "待审核";
            case Enterprise.STATUS_NORMAL:
                return "正常";
            case Enterprise.STATUS_FROZEN:
                return "冻结";
            case Enterprise.STATUS_CANCELLING:
                return "注销中";
            case Enterprise.STATUS_CANCELLED:
                return "已注销";
            default:
                return "未知";
        }
    }

    /**
     * 校验密码强度
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("密码长度不能少于8位");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("密码必须包含大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("密码必须包含小写字母");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("密码必须包含数字");
        }
    }

    /**
     * AES加密
     * FIX: 移除硬编码密钥fallback，必须通过环境变量ENCRYPTION_KEY配置
     */
    private String encryptWithAes(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        if (aesKey == null || aesKey.isEmpty()) {
            throw new IllegalStateException("AES加密密钥未配置，请设置ENCRYPTION_KEY环境变量");
        }
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            byte[] keyBytes = aesKey.getBytes();
            if (keyBytes.length != 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            }
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("AES加密失败", e);
            return null;
        }
    }
}
