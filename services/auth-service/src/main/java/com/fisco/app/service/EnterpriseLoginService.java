package com.fisco.app.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fisco.app.entity.LoginTransaction;
import com.fisco.app.feign.EnterpriseFeignClient;
import com.fisco.app.mapper.LoginTransactionMapper;
import com.fisco.app.util.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 企业登录服务 - TCC模式
 * 提供分布式环境下的登录事务补偿机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseLoginService {

    private static final int TX_EXPIRE_HOURS = 24;

    private final LoginTransactionMapper loginTransactionMapper;
    private final EnterpriseFeignClient enterpriseFeignClient;

    /**
     * TCC - Try阶段：记录登录事务
     *
     * @param username 用户名
     * @param password 密码
     * @return 事务UUID，客户端需保存用于后续Confirm/Cancel
     */
    public String tryLogin(String username, String password) {
        String txUuid = UUID.randomUUID().toString();

        LoginTransaction tx = new LoginTransaction();
        tx.setTxUuid(txUuid);
        tx.setUsername(username);
        tx.setLoginType(LoginTransaction.LOGIN_TYPE_ENTERPRISE);
        tx.setStatus(LoginTransaction.STATUS_TRYING);
        tx.setTryTime(LocalDateTime.now());
        tx.setExpireTime(LocalDateTime.now().plusHours(TX_EXPIRE_HOURS));

        loginTransactionMapper.insertWithAllFields(tx);
        log.info("[TCC-TRY] 登录事务已创建, txUuid={}, username={}", txUuid, username);

        return txUuid;
    }

    /**
     * TCC - Confirm阶段：确认登录成功
     * 当enterprise-service返回成功时调用
     *
     * @param txUuid 事务UUID
     * @param entId 企业ID
     * @param sessionId enterprise-service返回的会话ID
     */
    public void confirmLogin(String txUuid, Long entId, String sessionId) {
        LoginTransaction tx = loginTransactionMapper.selectByUuid(txUuid);
        if (tx == null) {
            log.warn("[TCC-CONFIRM] 事务不存在, txUuid={}", txUuid);
            return;
        }

        if (LoginTransaction.STATUS_CONFIRMED.equals(tx.getStatus())) {
            log.info("[TCC-CONFIRM] 事务已确认, txUuid={}", txUuid);
            return;
        }

        loginTransactionMapper.updateStatusToConfirmed(txUuid, entId, sessionId);
        log.info("[TCC-CONFIRM] 登录事务已确认, txUuid={}, entId={}", txUuid, entId);
    }

    /**
     * TCC - Cancel阶段：取消登录事务
     * 当enterprise-service返回失败或超时触发
     *
     * @param txUuid 事务UUID
     * @param errorMsg 错误信息
     */
    public void cancelLogin(String txUuid, String errorMsg) {
        LoginTransaction tx = loginTransactionMapper.selectByUuid(txUuid);
        if (tx == null) {
            log.warn("[TCC-CANCEL] 事务不存在, txUuid={}", txUuid);
            return;
        }

        if (LoginTransaction.STATUS_CONFIRMED.equals(tx.getStatus())) {
            log.warn("[TCC-CANCEL] 事务已确认无法取消, txUuid={}", txUuid);
            return;
        }

        if (LoginTransaction.STATUS_CANCELLED.equals(tx.getStatus())) {
            log.info("[TCC-CANCEL] 事务已取消, txUuid={}", txUuid);
            return;
        }

        loginTransactionMapper.updateStatusToCancelled(txUuid, errorMsg);
        log.info("[TCC-CANCEL] 登录事务已取消, txUuid={}, error={}", txUuid, errorMsg);
    }

    /**
     * 查询登录事务状态
     * 供客户端轮询查询登录结果
     *
     * @param txUuid 事务UUID
     * @return 事务状态信息
     */
    public LoginTransaction getTransactionStatus(String txUuid) {
        return loginTransactionMapper.selectByUuid(txUuid);
    }

    /**
     * 执行企业登录（带TCC补偿）
     * 步骤：Try -> 调用enterprise-service -> Confirm/Cancel
     *
     * @param username 用户名
     * @param password 密码
     * @param txUuid 事务UUID（由tryLogin生成）
     * @return enterprise-service的登录结果
     */
    public Result<?> executeLoginWithTcc(String username, String password, String txUuid) {
        try {
            // 调用enterprise-service
            Map<String, String> entLoginRequest = Map.of(
                    "username", username,
                    "password", password
            );
            Result<?> result = enterpriseFeignClient.login(entLoginRequest);

            if (result != null && result.getCode() == 0) {
                // 登录成功，Confirm
                Object data = result.getData();
                Long entId = null;
                String sessionId = null;
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    Object entIdObj = dataMap.get("entId");
                    if (entIdObj instanceof Number) {
                        entId = ((Number) entIdObj).longValue();
                    }
                    sessionId = String.valueOf(dataMap.get("sessionId"));
                }
                confirmLogin(txUuid, entId, sessionId);
            } else {
                // 登录失败，Cancel
                String msg = result != null ? result.getMsg() : "企业登录失败";
                cancelLogin(txUuid, msg);
            }

            return result;
        } catch (Exception e) {
            // 调用异常，Cancel
            log.error("[TCC] 企业登录异常, txUuid={}, error={}", txUuid, e.getMessage());
            cancelLogin(txUuid, e.getMessage());
            return Result.error(500, "企业登录服务不可用: " + e.getMessage());
        }
    }
}
