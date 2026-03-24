// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title CreditLimitScore
 * @dev 信用评分合约
 *
 * 管理企业信用评分计算、评分历史等
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract CreditLimitScore {

    // ==================== 常量定义 ====================

    uint256 constant MIN_SCORE = 300;
    uint256 constant MAX_SCORE = 1000;
    uint256 constant DEFAULT_SCORE = 800;
    uint256 constant MAX_HISTORY = 1000;
    uint256 constant MAX_BATCH_SIZE = 50;

    // 评分权重
    uint256 constant WEIGHT_REPAYMENT = 30;      // 还款表现权重
    uint256 constant WEIGHT_UTILIZATION = 25;    // 额度使用率权重
    uint256 constant WEIGHT_DIVERSITY = 20;     // 业务多样性权重
    uint256 constant WEIGHT_AGE = 15;           // 账龄权重
    uint256 constant WEIGHT_GUARANTEE = 10;     // 担保权重

    // ==================== 状态变量 ====================

    address public admin;
    address public creditLimitCore;
    uint256 public scoreCalculationCount;

    // ==================== 数据结构 ====================

    /**
     * @dev 信用评分记录
     */
    struct ScoreRecord {
        address enterpriseAddress;
        uint256 score;
        uint256 previousScore;
        uint256 calculationTime;
        bytes32 calculationFactors;
    }

    /**
     * @dev 评分因素
     */
    struct ScoreFactors {
        uint256 repaymentRate;      // 还款率 0-100
        uint256 utilizationRate;    // 额度使用率 0-100
        uint256 diversityScore;     // 业务多样性 0-100
        uint256 creditAge;          // 账龄（天）
        uint256 guaranteeScore;     // 担保评分 0-100
    }

    // ==================== 存储层 ====================

    // 企业地址 -> 当前评分
    mapping(address => uint256) private currentScores;
    // 企业地址 -> 评分历史记录
    mapping(address => ScoreRecord[]) private scoreHistory;
    // 企业地址 -> 历史记录数量
    mapping(address => uint256) private scoreHistoryCount;

    // ==================== 事件定义 ====================

    event ScoreCalculated(
        address indexed enterpriseAddress,
        uint256 newScore,
        uint256 previousScore,
        bytes32 calculationFactors,
        address indexed calculator,
        uint256 timestamp
    );

    event ScoreAdjusted(
        address indexed enterpriseAddress,
        uint256 oldScore,
        uint256 newScore,
        string reason,
        address indexed adjuster,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyValidEnterprise(address enterpriseAddress) {
        require(enterpriseAddress != address(0), "Invalid enterprise address");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _admin, address _creditLimitCore) {
        require(_admin != address(0), "Admin cannot be zero");
        admin = _admin;
        creditLimitCore = _creditLimitCore;
    }

    // ==================== 评分计算功能 ====================

    /**
     * @dev 计算信用评分
     * @param enterpriseAddress 企业地址
     * @param factors 评分因素
     * @return score 计算后的评分
     */
    function calculateScore(address enterpriseAddress, ScoreFactors calldata factors)
        external onlyValidEnterprise(enterpriseAddress) returns (uint256 score)
    {
        // 验证因素值
        require(factors.repaymentRate <= 100, "Invalid repayment rate");
        require(factors.utilizationRate <= 100, "Invalid utilization rate");
        require(factors.diversityScore <= 100, "Invalid diversity score");
        require(factors.guaranteeScore <= 100, "Invalid guarantee score");

        uint256 previousScore = currentScores[enterpriseAddress];

        // 计算评分（加权平均）
        uint256 repaymentScore = factors.repaymentRate * WEIGHT_REPAYMENT / 100;
        uint256 utilizationScore = calculateUtilizationScore(factors.utilizationRate) * WEIGHT_UTILIZATION / 100;
        uint256 diversityScore = factors.diversityScore * WEIGHT_DIVERSITY / 100;
        uint256 ageScore = calculateAgeScore(factors.creditAge) * WEIGHT_AGE / 100;
        uint256 guaranteeScore = factors.guaranteeScore * WEIGHT_GUARANTEE / 100;

        score = repaymentScore + utilizationScore + diversityScore + ageScore + guaranteeScore;

        // 限制范围
        if (score > MAX_SCORE) score = MAX_SCORE;
        if (score < MIN_SCORE) score = MIN_SCORE;

        // 更新当前评分
        currentScores[enterpriseAddress] = score;

        // 记录历史
        bytes32 factorsHash = keccak256(abi.encode(
            factors.repaymentRate,
            factors.utilizationRate,
            factors.diversityScore,
            factors.creditAge,
            factors.guaranteeScore
        ));

        ScoreRecord memory record = ScoreRecord({
            enterpriseAddress: enterpriseAddress,
            score: score,
            previousScore: previousScore,
            calculationTime: block.timestamp,
            calculationFactors: factorsHash
        });

        // 检查历史记录限制
        if (scoreHistoryCount[enterpriseAddress] < MAX_HISTORY) {
            scoreHistory[enterpriseAddress].push(record);
            scoreHistoryCount[enterpriseAddress]++;
        }

        scoreCalculationCount++;

        emit ScoreCalculated(
            enterpriseAddress,
            score,
            previousScore,
            factorsHash,
            msg.sender,
            block.timestamp
        );

        return score;
    }

    /**
     * @dev 根据额度使用率计算评分
     * @param utilizationRate 额度使用率 0-100
     * @return score 评分
     */
    function calculateUtilizationScore(uint256 utilizationRate) internal pure returns (uint256) {
        // 使用率越低，评分越高
        if (utilizationRate >= 90) return 20;
        if (utilizationRate >= 70) return 40;
        if (utilizationRate >= 50) return 60;
        if (utilizationRate >= 30) return 80;
        return 100;
    }

    /**
     * @dev 根据账龄计算评分
     * @param creditAge 账龄（天）
     * @return score 评分
     */
    function calculateAgeScore(uint256 creditAge) internal pure returns (uint256) {
        if (creditAge >= 730) return 100;       // 2年以上
        if (creditAge >= 365) return 80;        // 1-2年
        if (creditAge >= 180) return 60;        // 6个月-1年
        if (creditAge >= 90) return 40;         // 3-6个月
        if (creditAge >= 30) return 20;         // 1-3个月
        return 10;                              // 1个月以下
    }

    /**
     * @dev 调整信用评分（手动）
     * @param enterpriseAddress 企业地址
     * @param newScore 新评分
     * @param reason 调整原因
     * @return success 是否成功
     */
    function adjustScore(address enterpriseAddress, uint256 newScore, string calldata reason)
        external onlyAdmin onlyValidEnterprise(enterpriseAddress) returns (bool success)
    {
        require(newScore >= MIN_SCORE && newScore <= MAX_SCORE, "Invalid score");
        require(bytes(reason).length > 0, "Invalid reason");

        uint256 oldScore = currentScores[enterpriseAddress];
        currentScores[enterpriseAddress] = newScore;

        // 记录历史
        ScoreRecord memory record = ScoreRecord({
            enterpriseAddress: enterpriseAddress,
            score: newScore,
            previousScore: oldScore,
            calculationTime: block.timestamp,
            calculationFactors: bytes32(0)
        });

        if (scoreHistoryCount[enterpriseAddress] < MAX_HISTORY) {
            scoreHistory[enterpriseAddress].push(record);
            scoreHistoryCount[enterpriseAddress]++;
        }

        emit ScoreAdjusted(
            enterpriseAddress,
            oldScore,
            newScore,
            reason,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 批量计算评分
     * @param enterprises 企业地址数组
     * @param factorsArray 评分因素数组
     * @return success 是否成功
     */
    function batchCalculateScore(
        address[] calldata enterprises,
        ScoreFactors[] calldata factorsArray
    )
        external onlyAdmin returns (bool success)
    {
        require(enterprises.length <= MAX_BATCH_SIZE, "Batch size too large");
        require(enterprises.length == factorsArray.length, "Length mismatch");

        for (uint256 i = 0; i < enterprises.length; i++) {
            address enterpriseAddress = enterprises[i];
            ScoreFactors calldata factors = factorsArray[i];

            // 内联 calculateScore 逻辑
            require(factors.repaymentRate <= 100, "Invalid repayment rate");
            require(factors.utilizationRate <= 100, "Invalid utilization rate");
            require(factors.diversityScore <= 100, "Invalid diversity score");
            require(factors.guaranteeScore <= 100, "Invalid guarantee score");

            uint256 previousScore = currentScores[enterpriseAddress];
            uint256 repaymentScore = factors.repaymentRate * WEIGHT_REPAYMENT / 100;
            uint256 utilizationScore = calculateUtilizationScore(factors.utilizationRate) * WEIGHT_UTILIZATION / 100;
            uint256 diversityScore = factors.diversityScore * WEIGHT_DIVERSITY / 100;
            uint256 ageScore = calculateAgeScore(factors.creditAge) * WEIGHT_AGE / 100;
            uint256 guaranteeScore = factors.guaranteeScore * WEIGHT_GUARANTEE / 100;

            uint256 score = repaymentScore + utilizationScore + diversityScore + ageScore + guaranteeScore;
            if (score > MAX_SCORE) score = MAX_SCORE;
            if (score < MIN_SCORE) score = MIN_SCORE;

            currentScores[enterpriseAddress] = score;

            bytes32 factorsHash = keccak256(abi.encode(
                factors.repaymentRate, factors.utilizationRate, factors.diversityScore,
                factors.creditAge, factors.guaranteeScore
            ));

            if (scoreHistoryCount[enterpriseAddress] < MAX_HISTORY) {
                scoreHistory[enterpriseAddress].push(ScoreRecord({
                    enterpriseAddress: enterpriseAddress,
                    score: score,
                    previousScore: previousScore,
                    calculationTime: block.timestamp,
                    calculationFactors: factorsHash
                }));
                scoreHistoryCount[enterpriseAddress]++;
            }

            scoreCalculationCount++;

            emit ScoreCalculated(
                enterpriseAddress,
                score,
                previousScore,
                factorsHash,
                msg.sender,
                block.timestamp
            );
        }

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取当前评分
     * @param enterpriseAddress 企业地址
     * @return score 当前评分
     */
    function getCurrentScore(address enterpriseAddress)
        external view returns (uint256 score)
    {
        uint256 scoreValue = currentScores[enterpriseAddress];
        return scoreValue > 0 ? scoreValue : DEFAULT_SCORE;
    }

    /**
     * @dev 获取评分历史数量
     * @param enterpriseAddress 企业地址
     * @return count 历史记录数量
     */
    function getScoreHistoryCount(address enterpriseAddress)
        external view returns (uint256)
    {
        return scoreHistoryCount[enterpriseAddress];
    }

    /**
     * @dev 获取评分历史（分页）
     * @param enterpriseAddress 企业地址
     * @param offset 起始索引
     * @param limit 数量限制
     * @return history 评分历史数组
     */
    function getScoreHistory(address enterpriseAddress, uint256 offset, uint256 limit)
        external view returns (ScoreRecord[] memory)
    {
        require(limit <= MAX_BATCH_SIZE, "Limit too large");

        uint256 total = scoreHistoryCount[enterpriseAddress];
        if (offset >= total) {
            return new ScoreRecord[](0);
        }

        uint256 resultLength = total - offset;
        if (resultLength > limit) {
            resultLength = limit;
        }

        ScoreRecord[] memory result = new ScoreRecord[](resultLength);
        ScoreRecord[] storage history = scoreHistory[enterpriseAddress];

        for (uint256 i = 0; i < resultLength; i++) {
            result[i] = history[offset + i];
        }

        return result;
    }

    /**
     * @dev 获取最新评分记录
     * @param enterpriseAddress 企业地址
     * @return score 最新评分
     * @return previousScore 上次评分
     * @return calculationTime 计算时间
     */
    function getLatestScore(address enterpriseAddress)
        external view returns (
            uint256 score,
            uint256 previousScore,
            uint256 calculationTime
        )
    {
        uint256 count = scoreHistoryCount[enterpriseAddress];
        if (count == 0) {
            return (DEFAULT_SCORE, 0, 0);
        }

        ScoreRecord storage record = scoreHistory[enterpriseAddress][count - 1];
        return (record.score, record.previousScore, record.calculationTime);
    }

    /**
     * @dev 检查评分是否在指定等级
     * @param enterpriseAddress 企业地址
     * @param minScore 最低评分
     * @return isEligible 是否符合
     */
    function checkScoreLevel(address enterpriseAddress, uint256 minScore)
        external view returns (bool)
    {
        uint256 score = currentScores[enterpriseAddress];
        if (score == 0) score = DEFAULT_SCORE;
        return score >= minScore;
    }

    /**
     * @dev 获取评分等级
     * @param score 评分
     * @return level 等级字符串
     */
    function getScoreLevel(uint256 score) external pure returns (string memory level) {
        // 信用等级范围: AAA(900+), AA(800+), A(700+), BBB(600+), BB(500+), B(400+), CCC(350+), CC(325+), C(<325)
        if (score >= 900) return "AAA";
        if (score >= 800) return "AA";
        if (score >= 700) return "A";
        if (score >= 600) return "BBB";
        if (score >= 500) return "BB";
        if (score >= 400) return "B";
        if (score >= 350) return "CCC";
        if (score >= 325) return "CC";
        return "C";
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setCreditLimitCore(address newCore) external onlyAdmin returns (bool) {
        require(newCore != address(0), "Invalid address");
        creditLimitCore = newCore;
        return true;
    }
}
