// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title CreditLimitCore
 * @dev 信用额度核心合约
 *
 * 管理企业信用额度的核心操作，包括额度管理、事件上报等
 *
 * 遵循数据上链规范：
 * - 明文上链：creditLimit, creditScore, lastUpdateTime
 * - 哈希化上链：eventDataHash
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract CreditLimitCore {

    // ==================== 常量定义 ====================

    uint256 constant MAX_CREDIT_LIMIT = 1000000000000 ether; // 最大额度 1万亿
    uint256 constant MIN_CREDIT_LIMIT = 0;
    uint256 constant MAX_BATCH_SIZE = 50;

    // ==================== 状态变量 ====================

    address public admin;
    address public javaBackend;
    uint256 public enterpriseCreditCount;
    uint256 public constant VERSION = 2;

    // ==================== 数据结构 ====================

    /**
     * @dev 信用事件类型枚举
     */
    enum CreditEventType {
        Registration,       // 注册
        Financing,          // 融资
        Repayment,         // 还款
        Overdue,           // 逾期
        Default,           // 违约
        Pledge,            // 质押
        Release,           // 释放
        Guarantee,         // 担保
        CrossGuarantee     // 交叉担保
    }

    /**
     * @dev 信用额度信息
     */
    struct CreditInfo {
        address enterpriseAddress;   // 企业地址
        uint256 creditLimit;       // 授信额度
        uint256 usedLimit;         // 已用额度
        uint256 availableLimit;   // 可用额度
        uint256 lastUpdateTime;    // 最后更新时间
    }

    /**
     * @dev 信用事件记录
     */
    struct CreditEvent {
        address enterpriseAddress;
        CreditEventType eventType;
        int256 impact;           // 影响值（正负）
        bytes32 eventDataHash;   // 事件详情哈希
        uint256 timestamp;
    }

    // ==================== 存储层 ====================

    mapping(address => CreditInfo) private creditInfo;
    mapping(address => CreditEvent[]) private creditEvents;
    mapping(address => uint256) private creditEventCount;

    // ==================== 事件定义 ====================

    event CreditLimitUpdated(
        address indexed enterpriseAddress,
        uint256 oldLimit,
        uint256 newLimit,
        address indexed updater,
        uint256 timestamp
    );

    event CreditUsed(
        address indexed enterpriseAddress,
        uint256 amount,
        string operationType,
        address indexed operator,
        uint256 timestamp
    );

    event CreditReleased(
        address indexed enterpriseAddress,
        uint256 amount,
        string operationType,
        address indexed operator,
        uint256 timestamp
    );

    event CreditEventReported(
        address indexed enterpriseAddress,
        uint8 indexed eventType,
        int256 impact,
        bytes32 eventDataHash,
        address indexed reporter,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Only Java backend");
        _;
    }

    modifier onlyValidEnterprise(address enterpriseAddress) {
        require(enterpriseAddress != address(0), "Invalid enterprise address");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
        javaBackend = _initialAdmin;
    }

    // ==================== 额度管理功能 ====================

    /**
     * @dev 设置授信额度
     * @param enterpriseAddress 企业地址
     * @param newLimit 新授信额度
     * @return success 是否成功
     */
    function setCreditLimit(address enterpriseAddress, uint256 newLimit)
        external onlyAdmin onlyValidEnterprise(enterpriseAddress) returns (bool success)
    {
        require(newLimit >= MIN_CREDIT_LIMIT && newLimit <= MAX_CREDIT_LIMIT, "Invalid limit");

        CreditInfo storage info = creditInfo[enterpriseAddress];
        uint256 oldLimit = info.creditLimit;

        // 更新额度
        if (info.enterpriseAddress == address(0)) {
            // 新企业
            info.enterpriseAddress = enterpriseAddress;
            info.creditLimit = newLimit;
            info.usedLimit = 0;
            info.availableLimit = newLimit;
            enterpriseCreditCount++;
        } else {
            // 已有企业
            info.creditLimit = newLimit;
            info.availableLimit = newLimit - info.usedLimit;
        }
        info.lastUpdateTime = block.timestamp;

        emit CreditLimitUpdated(
            enterpriseAddress,
            oldLimit,
            newLimit,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 批量设置授信额度
     * @param enterprises 企业地址数组
     * @param limits 额度数组
     * @return success 是否成功
     */
    function batchSetCreditLimit(address[] calldata enterprises, uint256[] calldata limits)
        external onlyAdmin returns (bool success)
    {
        require(enterprises.length <= MAX_BATCH_SIZE, "Batch size too large");
        require(enterprises.length == limits.length, "Length mismatch");

        for (uint256 i = 0; i < enterprises.length; i++) {
            address enterpriseAddress = enterprises[i];
            uint256 newLimit = limits[i];
            require(newLimit >= MIN_CREDIT_LIMIT && newLimit <= MAX_CREDIT_LIMIT, "Invalid limit");

            CreditInfo storage info = creditInfo[enterpriseAddress];
            uint256 oldLimit = info.creditLimit;

            if (info.enterpriseAddress == address(0)) {
                info.enterpriseAddress = enterpriseAddress;
                info.creditLimit = newLimit;
                info.usedLimit = 0;
                info.availableLimit = newLimit;
                enterpriseCreditCount++;
            } else {
                info.creditLimit = newLimit;
                info.availableLimit = newLimit - info.usedLimit;
            }
            info.lastUpdateTime = block.timestamp;

            emit CreditLimitUpdated(
                enterpriseAddress,
                oldLimit,
                newLimit,
                msg.sender,
                block.timestamp
            );
        }

        return true;
    }

    /**
     * @dev 使用额度
     * @param enterpriseAddress 企业地址
     * @param amount 使用金额
     * @param operationType 操作类型
     * @return success 是否成功
     */
    function useCredit(address enterpriseAddress, uint256 amount, string calldata operationType)
        external onlyJavaBackend onlyValidEnterprise(enterpriseAddress) returns (bool success)
    {
        require(amount > 0, "Invalid amount");
        require(bytes(operationType).length > 0, "Invalid operation type");

        CreditInfo storage info = creditInfo[enterpriseAddress];
        require(info.enterpriseAddress != address(0), "Credit not initialized");
        require(info.availableLimit >= amount, "Insufficient credit");

        info.usedLimit += amount;
        info.availableLimit -= amount;
        info.lastUpdateTime = block.timestamp;

        emit CreditUsed(
            enterpriseAddress,
            amount,
            operationType,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 释放额度
     * @param enterpriseAddress 企业地址
     * @param amount 释放金额
     * @param operationType 操作类型
     * @return success 是否成功
     */
    function releaseCredit(address enterpriseAddress, uint256 amount, string calldata operationType)
        external onlyJavaBackend onlyValidEnterprise(enterpriseAddress) returns (bool success)
    {
        require(amount > 0, "Invalid amount");
        require(bytes(operationType).length > 0, "Invalid operation type");

        CreditInfo storage info = creditInfo[enterpriseAddress];
        require(info.enterpriseAddress != address(0), "Credit not initialized");
        require(info.usedLimit >= amount, "Invalid release amount");

        info.usedLimit -= amount;
        info.availableLimit += amount;
        info.lastUpdateTime = block.timestamp;

        emit CreditReleased(
            enterpriseAddress,
            amount,
            operationType,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 调整已用额度（不改变总额度）
     * @param enterpriseAddress 企业地址
     * @param adjustment 调整金额（正负）
     * @return success 是否成功
     */
    function adjustUsedCredit(address enterpriseAddress, int256 adjustment)
        external onlyAdmin onlyValidEnterprise(enterpriseAddress) returns (bool success)
    {
        CreditInfo storage info = creditInfo[enterpriseAddress];
        require(info.enterpriseAddress != address(0), "Credit not initialized");

        if (adjustment > 0) {
            uint256 increase = uint256(adjustment);
            require(info.availableLimit >= increase, "Insufficient available");
            info.usedLimit += increase;
            info.availableLimit -= increase;
        } else if (adjustment < 0) {
            uint256 decrease = uint256(-adjustment);
            require(info.usedLimit >= decrease, "Invalid adjustment");
            info.usedLimit -= decrease;
            info.availableLimit += decrease;
        }

        info.lastUpdateTime = block.timestamp;

        return true;
    }

    // ==================== 事件上报功能 ====================

    /**
     * @dev 上报信用事件
     * @param enterpriseAddress 企业地址
     * @param eventType 事件类型
     * @param impact 影响值
     * @param eventDataHash 事件数据哈希
     * @return success 是否成功
     */
    function reportCreditEvent(
        address enterpriseAddress,
        CreditEventType eventType,
        int256 impact,
        bytes32 eventDataHash
    )
        external onlyJavaBackend onlyValidEnterprise(enterpriseAddress) returns (bool success)
    {
        CreditEvent memory eventRecord = CreditEvent({
            enterpriseAddress: enterpriseAddress,
            eventType: eventType,
            impact: impact,
            eventDataHash: eventDataHash,
            timestamp: block.timestamp
        });

        creditEvents[enterpriseAddress].push(eventRecord);
        creditEventCount[enterpriseAddress]++;

        emit CreditEventReported(
            enterpriseAddress,
            uint8(eventType),
            impact,
            eventDataHash,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取信用额度信息
     * @param enterpriseAddress 企业地址
     * @return _enterpriseAddress 企业地址
     */
    function getCreditInfo(address enterpriseAddress)
        external view returns (
            address _enterpriseAddress,
            uint256 creditLimit,
            uint256 usedLimit,
            uint256 availableLimit,
            uint256 lastUpdateTime
        )
    {
        CreditInfo storage info = creditInfo[enterpriseAddress];
        return (
            info.enterpriseAddress,
            info.creditLimit,
            info.usedLimit,
            info.availableLimit,
            info.lastUpdateTime
        );
    }

    /**
     * @dev 检查可用额度
     * @param enterpriseAddress 企业地址
     * @param amount 检查金额
     * @return hasLimit 是否有足够额度
     */
    function checkCreditLimit(address enterpriseAddress, uint256 amount)
        external view returns (bool hasLimit)
    {
        CreditInfo storage info = creditInfo[enterpriseAddress];
        return info.enterpriseAddress != address(0) && info.availableLimit >= amount;
    }

    /**
     * @dev 获取信用事件数量
     * @param enterpriseAddress 企业地址
     * @return count 事件数量
     */
    function getCreditEventCount(address enterpriseAddress)
        external view returns (uint256)
    {
        return creditEventCount[enterpriseAddress];
    }

    /**
     * @dev 获取信用事件（分页）
     * @param enterpriseAddress 企业地址
     * @param offset 起始索引
     * @param limit 数量限制
     * @return events 事件数组
     */
    function getCreditEvents(address enterpriseAddress, uint256 offset, uint256 limit)
        external view returns (CreditEvent[] memory)
    {
        require(limit <= MAX_BATCH_SIZE, "Limit too large");

        uint256 total = creditEventCount[enterpriseAddress];
        if (offset >= total) {
            return new CreditEvent[](0);
        }

        uint256 resultLength = total - offset;
        if (resultLength > limit) {
            resultLength = limit;
        }

        CreditEvent[] memory result = new CreditEvent[](resultLength);
        CreditEvent[] storage events = creditEvents[enterpriseAddress];

        for (uint256 i = 0; i < resultLength; i++) {
            result[i] = events[offset + i];
        }

        return result;
    }

    /**
     * @dev 检查企业是否有信用记录
     * @param enterpriseAddress 企业地址
     * @return hasRecord 是否有记录
     */
    function hasCreditRecord(address enterpriseAddress)
        external view returns (bool)
    {
        return creditInfo[enterpriseAddress].enterpriseAddress != address(0);
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setJavaBackend(address newBackend) external onlyAdmin returns (bool) {
        require(newBackend != address(0), "Invalid address");
        javaBackend = newBackend;
        return true;
    }
}
