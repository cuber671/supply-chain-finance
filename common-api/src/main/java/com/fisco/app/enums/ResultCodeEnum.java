package com.fisco.app.enums;

import lombok.Getter;

/**
 * 统一响应码枚举
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(0, "操作成功"),
    ACCEPTED(202, "请求已接收"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "尚未登录或登录超时"),
    FORBIDDEN(403, "权限不足，拒绝访问"),
    NOT_FOUND(404, "资源不存在"),
    TIMEOUT(40003, "请求超时，请稍后重试"),
    SYSTEM_ERROR(500, "服务器内部异常"),

    ASYNC_TIMEOUT(40008, "异步处理超时"),
    ASYNC_FAILED(40009, "异步处理失败"),
    ASYNC_TASK_NOT_FOUND(40010, "异步任务不存在"),

    IDEMPOTENT_DUPLICATE(40005, "重复请求，请勿重复提交"),
    IDEMPOTENT_EXPIRED(40006, "transactionId已过期，请重新生成"),
    IDEMPOTENT_MISSING(40007, "缺少transactionId参数"),

    ENTP_NOT_EXISTS(601, "企业信息不存在"),
    ENTP_ALREADY_EXISTS(602, "企业名称或组织机构代码已注册"),
    ENTP_STATUS_ERROR(603, "企业状态异常，操作被中止"),
    PAY_PWD_ERROR(604, "交易密码验证失败"),

    BC_SDK_ERROR(701, "区块链节点连接异常"),
    BC_SIGN_ERROR(702, "区块链交易签名失败"),
    BC_CONTRACT_REVERT(703, "智能合约执行逻辑回滚"),
    BC_TX_TIMEOUT(704, "区块链交易共识超时"),

    CONTRACT_DEPLOY_FAILED(10001, "合约部署失败"),
    CONTRACT_CALL_FAILED(10002, "合约调用失败"),
    CONTRACT_QUERY_FAILED(10003, "合约查询失败"),
    CONTRACT_REVERT(10004, "合约执行回滚"),
    CONTRACT_NOT_FOUND(10005, "合约不存在"),
    CONTRACT_INVALID_ADDRESS(10006, "合约地址无效"),

    USER_NOT_FOUND(20001, "用户不存在"),
    USER_DISABLED(20002, "用户已被禁用"),
    USER_PASSWORD_ERROR(20003, "密码错误"),
    USER_TOKEN_EXPIRED(20004, "令牌已过期"),
    USER_TOKEN_INVALID(20005, "令牌无效"),
    PERMISSION_DENIED(20006, "权限不足"),
    ROLE_NOT_FOUND(20007, "角色不存在"),
    ROLE_INVALID(20008, "角色无效"),

    PARAM_MISSING(30001, "缺少必需参数"),
    PARAM_INVALID(30002, "参数格式错误"),
    PARAM_OUT_OF_RANGE(30003, "参数值超出范围"),
    PARAM_TYPE_MISMATCH(30004, "参数类型不匹配"),
    PARAM_DUPLICATE(30005, "参数重复"),

    SYSTEM_BUSY(40001, "系统繁忙，请稍后重试"),
    DATABASE_ERROR(40002, "数据库操作失败"),
    NETWORK_ERROR(40003, "网络异常"),
    SERVICE_UNAVAILABLE(40004, "服务不可用"),
    NODE_CONNECTION_FAILED(40005, "节点连接失败"),
    NODE_TIMEOUT(40006, "节点响应超时"),
    CONFIG_ERROR(40007, "配置错误"),
    RESOURCE_NOT_FOUND(40008, "资源不存在"),
    INSUFFICIENT_PERMISSIONS(40009, "权限不足");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
