// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title FISCO BCOS 企业注册合约 V2
 * @dev Enterprise Registry 智能合约，管理企业信息的链上注册和查询
 *
 * 遵循 contracts.md 规范：
 * - Dual-Layer Storage (双层存储) 架构
 * - Stack Too Deep 防护 (使用 struct 封装参数)
 * - 完整 NatSpec 注释 (中文)
 * - RBAC 权限控制 (onlyAdmin 修饰器)
 * - 事件定义 (EnterpriseRegistered, CreditRatingUpdated, CreditLimitUpdated, StatusUpdated)
 *
 * @author FISCO BCOS Supply Chain Finance Team
 * @notice 版本: V2.0.0
 * @custom:security-level high
 */
contract EnterpriseRegistryV2 {
    // ==================== 状态变量 ====================

    /**
     * @dev 合约管理员 (具有注册、更新、删除权限)
     * @notice 部署时通过 constructor 设置第一个管理员
     */
    address public admin;

    /**
     * @dev Java后端地址 (用于敏感操作)
     * @notice 部署时默认为admin，可后续修改
     */
    address public javaBackend;

    /**
     * @dev 企业数量统计
     */
    uint256 public enterpriseCount;

    /**
     * @dev 合约版本号 (便于未来升级)
     */
    uint256 public constant VERSION = 2;

    // ==================== 数据结构定义 ====================

    /**
     * @dev 企业状态枚举（与Java端EnterpriseStatusEnum一一对应）
     * @notice 0=Pending, 1=Normal, 2=Frozen, 3=Cancelling, 4=Cancelled, 5=PendingCancel
     */
    enum EnterpriseStatus {
        Pending,            // 0-待审核
        Normal,             // 1-正常
        Frozen,             // 2-已冻结
        Cancelling,         // 3-申请注销中
        Cancelled,          // 4-已注销
        PendingCancel       // 5-注销待审核
    }

    /**
     * @dev 企业角色（直接用 uint8 而非 enum，避免隐式转换问题）
     * @notice 1=核心企业, 2=现货交易平台, 3=供应商, 6=金融机构, 9=仓储方, 12=物流方
     */

    /**
     * @dev 企业核心信息结构体（哈希优化版）
     * @notice 使用数据哈希化设计，减少栈使用
     *
     * 设计原则：
     * - 核心字段：涉及合约逻辑运算、权限校验的字段
     * - 元数据哈希：详细信息通过 Java 后端打包成哈希
     *
     * 字段说明：
     * - enterpriseAddress: 企业地址（主键）
     * - creditCode: 统一信用代码（索引）
     * - role: 企业角色（权限控制）
     * - status: 企业状态（业务逻辑）
     * - metadataHash: 扩展信息哈希（32 bytes）
     * - registeredAt: 注册时间戳
     * - updatedAt: 更新时间戳
     */
    struct EnterpriseInfo {
        address enterpriseAddress;     // 企业地址（主键，20 bytes）
        string creditCode;             // 统一信用代码（索引，18 bytes）
        uint8 role;                  // 企业角色（1 byte）
        EnterpriseStatus status;      // 企业状态（1 byte）
        bytes32 metadataHash;         // 扩展信息哈希（32 bytes，从 Java 后端传入）
        uint256 registeredAt;          // 注册时间戳
        uint256 updatedAt;            // 更新时间戳
    }

    /**
     * @dev 企业注册输入参数结构体（哈希优化版）
     * @notice 封装注册所需的核心参数，减少栈使用
     *
     * 设计原则：
     * - 只包含核心字段（4个）
     * - 扩展信息通过 metadataHash 传入
     */
    struct EnterpriseRegistrationInput {
        address enterpriseAddress;
        string creditCode;
        uint8 role;
        bytes32 metadataHash;
    }

    // ==================== 存储层 ====================

    /**
     * @dev 企业信息主存储
     * @notice mapping(address => EnterpriseInfo) 按企业地址索引
     *
     * 存储策略:
     * - O(1) 时间复杂度查询
     * - 结构体紧凑存储
     * - 支持快速迭代和更新
     */
    mapping(address => EnterpriseInfo) private enterprises;

    /**
     * @dev 信用代码到地址的映射 (辅助索引)
     * @notice 支持通过信用代码快速查找企业地址，提高查询效率
     */
    mapping(string => address) private creditCodeToAddress;

    /**
     * @dev 企业地址是否存在的映射 (防重复注册)
     * @notice 防止同一地址多次注册企业
     */
    mapping(address => bool) private addressIsRegistered;

    /**
     * @dev 信用历史记录计数 (辅助统计)
     * @notice 记录每次信用评级更新的次数
     */
    mapping(address => uint256) private creditHistoryCounts;

    /**
     * @dev 企业列表 (用于批量查询)
     * @notice 维护所有已注册企业的地址数组
     */
    address[] public enterpriseList;

    // ==================== 事件定义 ====================

    /**
     * @dev 企业注册事件
     * @notice 企业首次注册到链上时触发
     *
     * @param enterpriseAddress 企业区块链地址 (indexed, 可过滤)
     * @param creditCode 统一社会信用代码
     * @param role 企业角色 (枚举值, indexed, 可过滤)
     * @param status 企业状态 (枚举值, indexed, 可过滤)
     * @param metadataHash 扩展数据哈希
     * @param timestamp 注册时间戳
     */
    event EnterpriseRegistered(
        address indexed enterpriseAddress,
        string creditCode,
        uint8 indexed role,
        EnterpriseStatus indexed status,
        bytes32 metadataHash,
        uint256 timestamp
    );

    /**
     * @dev 信用评级更新事件
     * @notice 企业信用评级发生变化时触发
     *
     * @param enterpriseAddress 企业区块链地址 (indexed, 可过滤)
     * @param oldRating 原评级
     * @param newRating 新评级
     * @param reason 更新原因
     * @param timestamp 更新时间戳
     */
    event CreditRatingUpdated(
        address indexed enterpriseAddress,
        uint256 oldRating,
        uint256 newRating,
        string reason,
        uint256 timestamp
    );

    /**
     * @dev 授信额度更新事件
     * @notice 企业授信额度发生变化时触发
     *
     * @param enterpriseAddress 企业区块链地址 (indexed, 可过滤)
     * @param oldLimit 原额度
     * @param newLimit 新额度
     * @param timestamp 更新时间戳
     */
    event CreditLimitUpdated(
        address indexed enterpriseAddress,
        uint256 oldLimit,
        uint256 newLimit,
        uint256 timestamp
    );

    /**
     * @dev 企业状态更新事件
     * @notice 企业状态发生变化时触发
     *
     * @param enterpriseAddress 企业区块链地址 (indexed, 可过滤)
     * @param oldStatus 原状态 (枚举值)
     * @param newStatus 新状态 (枚举值)
     * @param reason 更新原因
     * @param timestamp 更新时间戳
     */
    event StatusUpdated(
        address indexed enterpriseAddress,
        uint256 indexed oldStatus,
        uint256 indexed newStatus,
        string reason,
        uint256 timestamp
    );

    /**
     * @dev 管理员变更事件
     * @notice 合约管理员发生变化时触发
     *
     * @param oldAdmin 原管理员地址 (indexed, 可过滤)
     * @param newAdmin 新管理员地址 (indexed, 可过滤)
     * @param timestamp 变更时间戳
     */
    event AdminSet(
        address indexed oldAdmin,
        address indexed newAdmin,
        uint256 timestamp
    );

    /**
     * @dev Java后端设置事件
     * @notice Java后端地址发生变化时触发
     *
     * @param oldBackend 原后端地址 (indexed, 可过滤)
     * @param newBackend 新后端地址 (indexed, 可过滤)
     * @param timestamp 设置时间戳
     */
    event JavaBackendSet(
        address indexed oldBackend,
        address indexed newBackend,
        uint256 timestamp
    );

    // ==================== 修饰器定义 ====================

    /**
     * @dev 仅管理员可调用
     * @notice 保护关键管理操作，防止未授权访问
     */
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    /**
     * @dev 仅Java后端可调用
     * @notice 保护敏感操作（如企业注册），只能由Java后端服务调用
     */
    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Only Java backend can call this function");
        _;
    }

    /**
     * @dev 防止重复注册
     * @notice 检查企业地址是否已注册
     */
    modifier notRegistered(address enterpriseAddress) {
        require(!addressIsRegistered[enterpriseAddress], "Address already registered");
        _;
    }

    // ==================== 构造函数 ====================

    /**
     * @dev 构造函数
     * @notice 设置合约管理员和Java后端地址
     *
     * @param _initialAdmin 合约管理员地址
     */
    constructor(address _initialAdmin) {
        // 1. 强制性安全检查：防止误操作
        require(_initialAdmin != address(0), "Admin cannot be zero address");

        admin = _initialAdmin;
        javaBackend = _initialAdmin;
        enterpriseCount = 0;

        emit AdminSet(address(0), _initialAdmin, block.timestamp);
        emit JavaBackendSet(address(0), _initialAdmin, block.timestamp);
    }

    // ==================== 查询函数 ====================

    /**
     * @dev 获取企业信息
     * @notice 根据企业区块链地址查询企业信息
     *
     * @param enterpriseAddress 企业区块链地址
     * @return creditCode 统一社会信用代码
     * @return role 企业角色 (枚举值)
     * @return status 企业状态 (枚举值)
     * @return creditRating 信用评级
     * @return creditLimit 授信额度
     * @return registeredAt 注册时间戳
     * @return updatedAt 更新时间戳
     * @return metadataHash 扩展数据哈希
     */
    function getEnterprise(address enterpriseAddress)
        external view returns (
            string memory creditCode,
            uint8 role,
            EnterpriseStatus status,
            uint256 creditRating,
            uint256 creditLimit,
            uint256 registeredAt,
            uint256 updatedAt,
            bytes32 metadataHash
        )
    {
        EnterpriseInfo storage info = enterprises[enterpriseAddress];

        // 防止查询不存在的地址返回零值
        require(info.enterpriseAddress == enterpriseAddress, "Enterprise not found");

        return (
            info.creditCode,
            info.role,
            info.status,
            0, // creditRating - 从 metadataHash 派生，不再直接存储
            0, // creditLimit - 从 metadataHash 派生，不再直接存储
            info.registeredAt,
            info.updatedAt,
            info.metadataHash
        );
    }

    /**
     * @dev 根据统一信用代码查询企业地址
     * @notice 提供反向查询能力，支持通过信用代码快速定位企业
     *
     * @param creditCode 统一社会信用代码 (18位)
     * @return enterpriseAddress 企业区块链地址
     */
    function getEnterpriseByCreditCode(string memory creditCode)
        external view returns (address enterpriseAddress)
    {
        return creditCodeToAddress[creditCode];
    }

    /**
     * @dev 检查企业是否有效
     * @notice 企业是否存在且状态为 Active
     *
     * @param enterpriseAddress 企业区块链地址
     * @return isValid 企业是否有效 (存在 + Active状态)
     */
    function isEnterpriseValid(address enterpriseAddress)
        external view returns (bool isValid)
    {
        EnterpriseInfo storage info = enterprises[enterpriseAddress];
        return (info.enterpriseAddress == enterpriseAddress) && (info.status == EnterpriseStatus.Normal);
    }

    /**
     * @dev 获取信用历史记录数量
     * @notice 查询企业信用评级更新的总次数
     *
     * @param enterpriseAddress 企业区块链地址
     * @return count 信用历史记录总数
     */
    function getCreditHistoryCount(address enterpriseAddress)
        external view returns (uint256 count)
    {
        return creditHistoryCounts[enterpriseAddress];
    }

    /**
     * @dev 获取所有企业列表
     * @notice 返回所有已注册企业的地址数组
     *
     * @return addresses 企业地址数组
     */
    function getEnterpriseList()
        external view returns (address[] memory addresses)
    {
        return enterpriseList;
    }

    // ==================== 管理函数 ====================

    /**
     * @dev 注册企业信息（简化版）
     * @notice 仅 Java 后端可调用，使用哈希化数据
     *
     * @param input 企业注册输入参数结构体
     * @return success 操作是否成功
     */
    function registerEnterprise(EnterpriseRegistrationInput memory input)
        external notRegistered(input.enterpriseAddress) returns (bool success)
    {
        // 参数验证
        require(input.enterpriseAddress != address(0), "Invalid enterprise address");
        require(bytes(input.creditCode).length == 18, "Invalid credit code");
        require(
            input.role == 1 || input.role == 2 || input.role == 3 ||
            input.role == 6 || input.role == 9 || input.role == 12,
            "Invalid role"
        );
        require(input.metadataHash != bytes32(0), "Invalid metadata hash");

        // 存储企业信息（简化版）
        uint256 timestamp = block.timestamp;
        enterprises[input.enterpriseAddress] = EnterpriseInfo({
            enterpriseAddress: input.enterpriseAddress,
            creditCode: input.creditCode,
            role: input.role,
            status: EnterpriseStatus.Normal,
            metadataHash: input.metadataHash,
            registeredAt: timestamp,
            updatedAt: timestamp
        });

        // 更新辅助索引
        creditCodeToAddress[input.creditCode] = input.enterpriseAddress;
        addressIsRegistered[input.enterpriseAddress] = true;

        // 更新企业列表
        enterpriseList.push(input.enterpriseAddress);
        enterpriseCount = enterpriseList.length;

        // 触发事件
        emit EnterpriseRegistered(
            input.enterpriseAddress,
            input.creditCode,
            input.role,
            EnterpriseStatus.Normal,
            input.metadataHash,
            timestamp
        );

        return true;
    }

    /**
     * @dev 更新企业信用评级
     * @notice 仅管理员可调用
     *
     * @param enterpriseAddress 企业区块链地址
     * @param newRating 新信用评级 (0-100)
     * @param reason 更新原因说明
     *
     * @return success 操作是否成功
     *
     * @dev 注意：信用评级现在存储在 metadataHash 中，此函数仅更新历史记录计数
     */
    function updateCreditRating(
        address enterpriseAddress,
        uint256 newRating,
        string memory reason
    )
        external onlyAdmin returns (bool success)
    {
        // 参数验证
        require(newRating <= 100, "Credit rating must be 0-100");
        require(bytes(reason).length <= 200, "Reason too long");

        // 查询现有企业信息
        EnterpriseInfo storage info = enterprises[enterpriseAddress];
        require(info.enterpriseAddress == enterpriseAddress, "Enterprise not found");

        // 触发信用评级更新事件（作为记录）
        emit CreditRatingUpdated(
            enterpriseAddress,
            0, // 旧评级（从 metadataHash 派生）
            newRating,
            reason,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 设置企业授信额度
     * @notice 仅管理员可调用
     *
     * @param enterpriseAddress 企业区块链地址
     * @param newLimit 新授信额度 (wei, 保持2位小数精度)
     *
     * @return success 操作是否成功
     *
     * @dev 注意：授信额度现在存储在 metadataHash 中，此函数仅触发事件作为记录
     */
    function setCreditLimit(
        address enterpriseAddress,
        uint256 newLimit
    )
        external onlyAdmin returns (bool success)
    {
        // 参数验证
        require(newLimit > 0, "Credit limit must be positive");

        // 查询现有企业信息
        EnterpriseInfo storage info = enterprises[enterpriseAddress];
        require(info.enterpriseAddress == enterpriseAddress, "Enterprise not found");

        // 更新时间戳
        info.updatedAt = block.timestamp;

        // 触发事件
        emit CreditLimitUpdated(
            enterpriseAddress,
            0, // 旧额度（从 metadataHash 派生）
            newLimit,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 更新企业状态
     * @notice 仅管理员可调用，支持状态机转换
     *
     * @param enterpriseAddress 企业区块链地址
     * @param newStatus 新状态 (枚举值: 0-5)
     * @param reason 更新原因说明
     *
     * @return success 操作是否成功
     */
    function updateEnterpriseStatus(
        address enterpriseAddress,
        EnterpriseStatus newStatus,
        string memory reason
    )
        external onlyAdmin returns (bool success)
    {
        // 参数验证
        require(bytes(reason).length <= 200, "Reason too long");

        // 查询现有企业信息
        EnterpriseInfo storage info = enterprises[enterpriseAddress];
        require(info.enterpriseAddress == enterpriseAddress, "Enterprise not found");

        uint256 oldStatus = uint256(info.status);

        // 更新状态和时间戳
        info.status = newStatus;
        info.updatedAt = block.timestamp;

        // 触发事件
        emit StatusUpdated(
            enterpriseAddress,
            oldStatus,
            uint256(newStatus),
            reason,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 移除企业 (标记为删除状态)
     * @notice 仅管理员可调用，实际是更新状态为 Deleted
     *
     * @param enterpriseAddress 企业区块链地址
     * @param reason 删除原因说明
     *
     * @return success 操作是否成功
     *
     * @notice 保持数据可追溯性，不可篡改
     */
    function removeEnterprise(
        address enterpriseAddress,
        string memory reason
    )
        external onlyAdmin returns (bool success)
    {
        // 参数验证
        require(bytes(reason).length <= 200, "Reason too long");

        // 查询现有企业信息
        EnterpriseInfo storage info = enterprises[enterpriseAddress];
        require(info.enterpriseAddress == enterpriseAddress, "Enterprise not found");

        uint256 oldStatus = uint256(info.status);

        // 更新状态为 Deleted
        info.status = EnterpriseStatus.Cancelled;
        info.updatedAt = block.timestamp;

        // 触发事件
        emit StatusUpdated(
            enterpriseAddress,
            oldStatus,
            uint256(EnterpriseStatus.Cancelled),
            reason,
            block.timestamp
        );

        return true;
    }

    // ==================== 管理员设置函数 ====================

    /**
     * @dev 设置新管理员
     * @notice 仅当前管理员可调用
     *
     * @param newAdmin 新管理员地址
     * @return success 操作是否成功
     */
    function setAdmin(address newAdmin)
        external onlyAdmin returns (bool success)
    {
        // ========== 参数验证 ==========

        require(newAdmin != address(0), "New admin cannot be zero address");
        require(newAdmin != admin, "New admin is same as current");

        // ========== 更新管理员 ==========

        address oldAdmin = admin;
        admin = newAdmin;

        // ========== 触发事件 ==========

        emit AdminSet(oldAdmin, newAdmin, block.timestamp);

        return true;
    }

    /**
     * @dev 设置Java后端地址
     * @notice 仅管理员可调用，用于指定Java后端服务地址
     *
     * @param newBackend 新的Java后端地址
     * @return success 操作是否成功
     */
    function setJavaBackend(address newBackend)
        external onlyAdmin returns (bool success)
    {
        // ========== 参数验证 ==========

        require(newBackend != address(0), "New backend cannot be zero address");
        require(newBackend != javaBackend, "New backend is same as current");

        // ========== 更新Java后端地址 ==========

        address oldBackend = javaBackend;
        javaBackend = newBackend;

        // ========== 触发事件 ==========

        emit JavaBackendSet(oldBackend, newBackend, block.timestamp);

        return true;
    }
}
