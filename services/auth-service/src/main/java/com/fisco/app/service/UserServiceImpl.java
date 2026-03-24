package com.fisco.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fisco.app.dto.RegisterRequestDTO;
import com.fisco.app.entity.User;
import com.fisco.app.feign.EnterpriseFeignClient;
import com.fisco.app.mapper.UserMapper;
import com.fisco.app.enums.UserStatusEnum;
import com.fisco.app.util.Result;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户业务服务实现类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final EnterpriseFeignClient enterpriseFeignClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper, EnterpriseFeignClient enterpriseFeignClient) {
        this.userMapper = userMapper;
        this.enterpriseFeignClient = enterpriseFeignClient;
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        User user = getUserByUsername(username);
        if (user == null) {
            return null;
        }

        // 检查用户状态
        if (user.getStatus() == UserStatusEnum.FROZEN.getValue()) {
            throw new IllegalStateException("账户已被冻结");
        }
        if (user.getStatus() == UserStatusEnum.CANCELLED.getValue()) {
            throw new IllegalStateException("账户已注销");
        }
        if (user.getStatus() == UserStatusEnum.PENDING.getValue()) {
            throw new IllegalStateException("账户待审核，暂不能登录");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        // 更新最后登录时间
        updateLastLoginTime(user.getUserId());

        return user;
    }

    @Override
    public Long registerUser(RegisterRequestDTO request) {
        // 邀请码校验（如果提供了邀请码）
        if (request.getInviteCode() != null && !request.getInviteCode().isEmpty()) {
            try {
                Result<?> validateResult = enterpriseFeignClient.validateInviteCode(request.getInviteCode());
                if (validateResult == null || validateResult.getCode() != 0 || validateResult.getData() == null || !Boolean.TRUE.equals(validateResult.getData())) {
                    throw new IllegalArgumentException("无效的邀请码");
                }
                // 如果邀请码有效，从邀请码中获取企业ID
                // 注意：实际的企业ID获取可能需要从邀请码验证结果中获取，此处假设通过其他方式获取
                // 企业服务validateInviteCode返回的数据应包含企业信息
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.warn("邀请码校验调用失败, inviteCode={}, error={}", request.getInviteCode(), e.getMessage());
                // 邀请码校验服务不可用时，允许用户不通过邀请码注册（可按需改为拒绝）
            }
        }

        // 如果提供了企业ID且提供了邀请码，校验企业ID与邀请码匹配
        if (request.getEnterpriseId() != null && request.getEnterpriseId() != 0 && request.getInviteCode() != null && !request.getInviteCode().isEmpty()) {
            try {
                Result<?> entResult = enterpriseFeignClient.getEnterpriseById(request.getEnterpriseId());
                if (entResult == null || entResult.getCode() != 0) {
                    throw new IllegalArgumentException("无效的企业ID");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setEnterpriseId(request.getEnterpriseId() != null ? request.getEnterpriseId() : 0L);
        user.setStatus(UserStatusEnum.PENDING.getValue());
        user.setUserRole(request.getUserRole() != null ? request.getUserRole() : User.ROLE_OPERATOR);

        userMapper.insert(user);
        log.info("用户注册成功, userId={}, username={}, inviteCode={}", user.getUserId(), user.getUsername(), request.getInviteCode());

        return user.getUserId();
    }

    @Override
    public User getUserByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        userMapper.updateLastLoginTimeDirect(userId, LocalDateTime.now());
    }

    // ==================== 用户查询 ====================

    @Override
    public User getUserById(Long userId) {
        if (userId == null) return null;
        return userMapper.selectById(userId);
    }

    @Override
    public IPage<User> getUserPageByEnterpriseId(Long enterpriseId, int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEnterpriseId, enterpriseId);
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(page, wrapper);
    }

    @Override
    public List<User> getPendingUsers(Long enterpriseId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, UserStatusEnum.PENDING.getValue());
        if (enterpriseId != null) {
            wrapper.eq(User::getEnterpriseId, enterpriseId);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectList(wrapper);
    }

    @Override
    public List<User> getPendingCancellationUsers(Long enterpriseId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, UserStatusEnum.PENDING_CANCEL.getValue());
        if (enterpriseId != null) {
            wrapper.eq(User::getEnterpriseId, enterpriseId);
        }
        wrapper.orderByDesc(User::getUpdateTime);
        return userMapper.selectList(wrapper);
    }

    // ==================== 用户信息管理 ====================

    @Override
    public boolean updateUserInfo(Long userId, String realName, String phone, String email) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        // 密码强度校验
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        return userMapper.updateById(user) > 0;
    }

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

    // ==================== 用户状态管理 ====================

    @Override
    public boolean freezeUser(Long userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.NORMAL.getValue()) {
            throw new IllegalStateException("只有正常状态的用户可以冻结");
        }
        user.setStatus(UserStatusEnum.FROZEN.getValue());
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            log.info("用户冻结成功: userId={}", userId);
        }
        return result;
    }

    @Override
    public boolean unfreezeUser(Long userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.FROZEN.getValue()) {
            throw new IllegalStateException("只有冻结状态的用户可以解冻");
        }
        user.setStatus(UserStatusEnum.NORMAL.getValue());
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            log.info("用户解冻成功: userId={}", userId);
        }
        return result;
    }

    @Override
    public boolean disableUser(Long userId) {
        return freezeUser(userId); // 禁用等同于冻结
    }

    @Override
    public boolean deleteUser(Long userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        boolean result = userMapper.deleteById(userId) > 0;
        if (result) {
            log.info("用户删除成功: userId={}", userId);
        }
        return result;
    }

    // ==================== 审核管理 ====================

    @Override
    public boolean auditUser(Long userId, boolean approved, Long auditorId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.PENDING.getValue()) {
            throw new IllegalArgumentException("该用户不是待审核状态");
        }
        // 审核通过设为正常(2)，拒绝设为冻结(3)
        int newStatus = approved ? UserStatusEnum.NORMAL.getValue() : UserStatusEnum.FROZEN.getValue();
        user.setStatus(newStatus);
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            log.info("用户审核完成: userId={}, approved={}, newStatus={}, auditorId={}", userId, approved, newStatus, auditorId);
        }
        return result;
    }

    @Override
    public boolean applyCancellation(Long userId, String reason, String password) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.NORMAL.getValue()) {
            throw new IllegalStateException("用户状态异常，无法申请注销");
        }
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("密码验证失败");
        }
        user.setStatus(UserStatusEnum.PENDING_CANCEL.getValue());
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            log.info("用户注销申请成功: userId={}, reason={}", userId, reason);
        }
        return result;
    }

    @Override
    public boolean auditCancellation(Long userId, boolean approved, Long auditorId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.PENDING_CANCEL.getValue()) {
            throw new IllegalArgumentException("该用户不是注销待审核状态");
        }
        // 审核通过设为已注销(5)，拒绝恢复正常(2)
        int newStatus = approved ? UserStatusEnum.CANCELLED.getValue() : UserStatusEnum.NORMAL.getValue();
        user.setStatus(newStatus);
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            log.info("用户注销审核完成: userId={}, approved={}, newStatus={}, auditorId={}", userId, approved, newStatus, auditorId);
        }
        return result;
    }

    @Override
    public boolean revokeCancellation(Long userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.PENDING_CANCEL.getValue()) {
            throw new IllegalArgumentException("该用户不是注销待审核状态，无法撤回");
        }
        // 撤回后恢复正常状态
        user.setStatus(UserStatusEnum.NORMAL.getValue());
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            log.info("用户注销申请已撤回: userId={}", userId);
        }
        return result;
    }
}
