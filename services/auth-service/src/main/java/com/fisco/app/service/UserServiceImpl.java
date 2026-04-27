package com.fisco.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        Long resolvedEnterpriseId = 0L;

        // 邀请码处理：校验并提取 inviterEntId
        if (request.getInviteCode() != null && !request.getInviteCode().isEmpty()) {
            Map<String, String> validateReq = new java.util.HashMap<>();
            validateReq.put("code", request.getInviteCode());

            Result<Map<String, Object>> validateResult;
            try {
                validateResult = enterpriseFeignClient.validateInvitation(validateReq);
            } catch (Exception e) {
                log.warn("邀请码校验服务调用异常, inviteCode={}, error={}", request.getInviteCode(), e.getMessage());
                throw new IllegalArgumentException("邀请码校验失败，请稍后重试");
            }

            if (validateResult == null || validateResult.getCode() != 0 || validateResult.getData() == null) {
                String errMsg = validateResult != null ? validateResult.getMsg() : "邀请码无效";
                throw new IllegalArgumentException(errMsg != null ? errMsg : "无效的邀请码");
            }

            // 从校验结果中提取 inviterEntId
            Object inviterEntIdObj = validateResult.getData().get("inviterEntId");
            if (inviterEntIdObj == null) {
                throw new IllegalArgumentException("邀请码关联企业信息缺失");
            }
            resolvedEnterpriseId = ((Number) inviterEntIdObj).longValue();
        }

        // 手机号查重：如果存在 FROZEN 状态的用户，更新信息并刷新状态
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            User existingUser = getUserByPhone(request.getPhone());
            if (existingUser != null && existingUser.getStatus() == UserStatusEnum.FROZEN.getValue()) {
                existingUser.setUsername(request.getUsername());
                existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
                existingUser.setRealName(request.getRealName());
                existingUser.setEmail(request.getEmail());
                existingUser.setStatus(UserStatusEnum.PENDING.getValue());
                existingUser.setUserRole(request.getUserRole() != null ? request.getUserRole() : User.ROLE_OPERATOR);
                existingUser.setEnterpriseId(resolvedEnterpriseId);
                userMapper.updateById(existingUser);
                log.info("用户重新注册（原FROZEN状态刷新）, userId={}, username={}, phone={}",
                        existingUser.getUserId(), request.getUsername(), request.getPhone());
                return existingUser.getUserId();
            }
        }

        // 构建用户实体（enterpriseId 来自邀请码，不信任请求体中的 enterpriseId）
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setEnterpriseId(resolvedEnterpriseId);
        user.setStatus(UserStatusEnum.PENDING.getValue());
        user.setUserRole(request.getUserRole() != null ? request.getUserRole() : User.ROLE_OPERATOR);

        userMapper.insert(user);
        log.info("用户注册成功, userId={}, username={}, enterpriseId={}, inviteCode={}",
                user.getUserId(), user.getUsername(), resolvedEnterpriseId, request.getInviteCode());

        // 注册成功后，原子递增邀请码使用次数
        if (request.getInviteCode() != null && !request.getInviteCode().isEmpty()) {
            try {
                Map<String, String> useReq = new java.util.HashMap<>();
                useReq.put("code", request.getInviteCode());
                enterpriseFeignClient.useInviteCode(useReq);
            } catch (Exception e) {
                log.error("邀请码usedCount递增失败，需人工核查: inviteCode={}, userId={}, error={}",
                        request.getInviteCode(), user.getUserId(), e.getMessage());
            }
        }

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

    private User getUserByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
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
        // 过滤掉已注销的用户
        wrapper.ne(User::getStatus, UserStatusEnum.CANCELLED.getValue());
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

    @Override
    public boolean updateUserStatus(Long userId, Integer newStatus) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        int currentStatus = user.getStatus();

        // 状态机校验：定义合法的状态转换
        boolean validTransition = false;
        switch (newStatus) {
            case User.STATUS_PENDING:
                // CANCELLED(5) -> PENDING(1)：重新注册
                validTransition = (currentStatus == User.STATUS_CANCELLED);
                break;
            case User.STATUS_NORMAL:
                // PENDING(1) -> NORMAL(2)：审核通过
                // FROZEN(3) -> NORMAL(2)：解冻恢复
                // CANCELLING(4)/PENDING_CANCEL(6) -> NORMAL(2)：注销拒绝/撤回
                validTransition = (currentStatus == User.STATUS_PENDING)
                        || (currentStatus == User.STATUS_FROZEN)
                        || (currentStatus == UserStatusEnum.CANCELLING.getValue())
                        || (currentStatus == UserStatusEnum.PENDING_CANCEL.getValue());
                break;
            case User.STATUS_FROZEN:
                // NORMAL(2) -> FROZEN(3)：冻结/禁用
                // PENDING(1) -> FROZEN(3)：审核拒绝
                validTransition = (currentStatus == User.STATUS_NORMAL)
                        || (currentStatus == User.STATUS_PENDING);
                break;
            case User.STATUS_CANCELLING:
                // NORMAL(2) -> CANCELLING(4)：发起注销（由系统或管理员触发）
                validTransition = (currentStatus == User.STATUS_NORMAL);
                break;
            case User.STATUS_CANCELLED:
                // CANCELLING(4)/PENDING_CANCEL(6) -> CANCELLED(5)：注销审核通过
                validTransition = (currentStatus == UserStatusEnum.CANCELLING.getValue())
                        || (currentStatus == UserStatusEnum.PENDING_CANCEL.getValue());
                break;
            case User.STATUS_PENDING_CANCEL:
                // NORMAL(2) -> PENDING_CANCEL(6)：用户申请注销（待审核）
                validTransition = (currentStatus == User.STATUS_NORMAL);
                break;
            default:
                validTransition = false;
        }

        if (!validTransition) {
            throw new IllegalStateException(
                    String.format("非法的用户状态转换: 当前状态=%d, 目标状态=%d", currentStatus, newStatus));
        }

        user.setStatus(newStatus);
        int updated = userMapper.updateById(user);
        if (updated > 0) {
            log.info("用户状态变更成功: userId={}, oldStatus={}, newStatus={}", userId, currentStatus, newStatus);
        }
        return updated > 0;
    }

    // ==================== 审核管理 ====================

    @Override
    public boolean auditUser(Long userId, boolean approved, Long auditorId, String auditorRole, Long auditorEntId, String auditorUserRole, String rejectReason) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.PENDING.getValue()) {
            throw new IllegalArgumentException("该用户不是待审核状态");
        }

        // 权限检查逻辑：
        // 1. 系统管理员 (role=ADMIN) 可审核所有用户
        // 2. 企业账户 (role=ENTERPRISE) 可审核同企业用户
        // 3. 用户账户 (role=USER) 只有 userRole=ADMIN 时可审核同企业用户
        boolean hasPermission = false;

        if (User.ROLE_ADMIN.equals(auditorRole)) {
            // 1. 系统管理员
            hasPermission = true;
        } else if ("ENTERPRISE".equals(auditorRole)) {
            // 2. 企业账户 - 必须同企业
            if (auditorEntId != null && auditorEntId.equals(user.getEnterpriseId())) {
                hasPermission = true;
            }
        } else if ("USER".equals(auditorRole)) {
            // 3. 用户账户 - 必须是同企业且 userRole=ADMIN
            if (auditorEntId != null
                    && auditorEntId.equals(user.getEnterpriseId())
                    && User.ROLE_ADMIN.equals(auditorUserRole)) {
                hasPermission = true;
            }
        }

        if (!hasPermission) {
            throw new IllegalArgumentException("无权审核该用户");
        }

        // 审核通过设为正常(2)，拒绝设为冻结(3)
        int newStatus = approved ? UserStatusEnum.NORMAL.getValue() : UserStatusEnum.FROZEN.getValue();
        user.setStatus(newStatus);
        boolean result = userMapper.updateById(user) > 0;
        if (result) {
            log.info("用户审核完成: userId={}, approved={}, newStatus={}, auditorId={}, auditorRole={}, auditorEntId={}, auditorUserRole={}",
                    userId, approved, newStatus, auditorId, auditorRole, auditorEntId, auditorUserRole);
        }
        // 审核拒绝时，记录拒绝原因
        if (!approved && rejectReason != null && !rejectReason.isEmpty()) {
            log.info("用户审核拒绝原因: userId={}, auditorId={}, reason={}", userId, auditorId, rejectReason);
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
    public boolean revokeCancellation(Long userId, String password) {
        User user = getUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != UserStatusEnum.PENDING_CANCEL.getValue()) {
            throw new IllegalArgumentException("该用户不是注销待审核状态，无法撤回");
        }
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("密码验证失败");
        }
        // 撤回后恢复正常状态，走统一状态机校验
        boolean result = updateUserStatus(userId, UserStatusEnum.NORMAL.getValue());
        if (result) {
            log.info("用户注销申请已撤回: userId={}", userId);
        }
        return result;
    }
}
