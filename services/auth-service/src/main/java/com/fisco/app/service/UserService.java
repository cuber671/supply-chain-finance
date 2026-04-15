package com.fisco.app.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fisco.app.dto.RegisterRequestDTO;
import com.fisco.app.entity.User;

/**
 * 用户业务服务接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回用户信息，失败返回null
     */
    User login(String username, String password);

    /**
     * 用户注册（含邀请码校验）
     *
     * @param request 注册请求DTO
     * @return 注册成功后返回用户ID
     * @throws IllegalArgumentException 校验失败时抛出
     */
    Long registerUser(RegisterRequestDTO request);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 更新最后登录时间
     *
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);

    // ==================== 用户查询 ====================

    /**
     * 根据ID查询用户
     */
    User getUserById(Long userId);

    /**
     * 分页查询企业下的用户列表
     */
    IPage<User> getUserPageByEnterpriseId(Long enterpriseId, int pageNum, int pageSize);

    /**
     * 获取待审核用户列表
     */
    List<User> getPendingUsers(Long enterpriseId);

    /**
     * 获取待审核注销用户列表
     */
    List<User> getPendingCancellationUsers(Long enterpriseId);

    // ==================== 用户信息管理 ====================

    /**
     * 更新用户信息（realName, phone, email）
     */
    boolean updateUserInfo(Long userId, String realName, String phone, String email);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否修改成功
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    // ==================== 用户状态管理 ====================

    /**
     * 冻结用户
     */
    boolean freezeUser(Long userId);

    /**
     * 解冻用户
     */
    boolean unfreezeUser(Long userId);

    /**
     * 禁用用户
     */
    boolean disableUser(Long userId);

    /**
     * 删除用户
     */
    boolean deleteUser(Long userId);

    /**
     * 更新用户状态（含状态机校验）
     * @param userId 用户ID
     * @param newStatus 新状态
     * @return 是否更新成功
     * @throws IllegalStateException 非法的状态转换时抛出
     */
    boolean updateUserStatus(Long userId, Integer newStatus);

    // ==================== 审核管理 ====================

    /**
     * 审核用户注册申请
     * @param userId 用户ID
     * @param approved 审核结果
     * @param auditorId 审核人ID
     * @param rejectReason 拒绝原因（审核拒绝时使用）
     */
    boolean auditUser(Long userId, boolean approved, Long auditorId, String rejectReason);

    /**
     * 审核用户注销申请
     * @param userId 用户ID
     * @param approved 审核结果
     * @param auditorId 审核人ID
     */
    boolean auditCancellation(Long userId, boolean approved, Long auditorId);

    /**
     * 申请注销
     * @param userId 用户ID
     * @param reason 注销原因
     * @param password 密码验证
     */
    boolean applyCancellation(Long userId, String reason, String password);

    /**
     * 撤回注销申请
     * @param userId 用户ID
     */
    boolean revokeCancellation(Long userId);
}
