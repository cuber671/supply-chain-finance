package com.fisco.app.service;

import java.math.BigDecimal;
import java.util.List;

import com.fisco.app.entity.Receivable;
import com.fisco.app.entity.RepaymentRecord;

/**
 * 金融服务接口
 *
 * 定义金融模块的核心业务方法，包括：
 * - 应收款生成、确认、调整
 * - 还款记录（现金还款、仓单抵债）
 * - 账务流水查询
 */
public interface FinanceService {

    /**
     * 生成应收款
     *
     * 根据物流委托单生成应收款记录，应收款金额 = 物流单金额 × 单价。
     * 初始状态为"待确认"，需由债务人确认后生效。
     *
     * @param voucherId 物流委托单ID
     * @param unitPrice 单价（元）
     * @return 创建的应收款记录
     * @throws IllegalArgumentException 物流单不存在或状态不允许生成应收款
     */
    Receivable generateReceivable(Long voucherId, BigDecimal unitPrice);

    /**
     * 根据ID查询应收款详情
     *
     * @param id 应收款记录ID
     * @return 应收款记录，不存在返回null
     */
    Receivable getReceivableById(Long id);

    /**
     * 根据应收款编号查询应收款
     *
     * @param receivableNo 应收款编号
     * @return 应收款记录，不存在返回null
     */
    Receivable getReceivableByNo(String receivableNo);

    /**
     * 查询债权人（核心企业）的所有应收款
     *
     * @param creditorEntId 债权人（核心企业）ID
     * @return 应收款列表
     */
    List<Receivable> listByCreditor(Long creditorEntId);

    /**
     * 查询债务人（上游供应商）的所有应收款
     *
     * @param debtorEntId 债务人（上游供应商）ID
     * @return 应收款列表
     */
    List<Receivable> listByDebtor(Long debtorEntId);

    /**
     * 确认应收款（债务人签署）
     *
     * 债务人对应收款进行确认，签署后应收款正式生效。
     *
     * @param receivableId 应收款记录ID
     * @param signature 债务人签名（签名数据的哈希值）
     * @return 确认后的应收款记录
     * @throws IllegalArgumentException 应收款不存在或状态不允许确认
     */
    Receivable confirmReceivable(Long receivableId, String signature);

    /**
     * 调整应收款金额
     *
     * 支持两种调整类型：
     * - TYPE_INCREASE (1): 增加应收款金额
     * - TYPE_DECREASE (2): 减少应收款金额
     * 调整后更新待还余额。
     *
     * @param receivableId 应收款记录ID
     * @param adjustType 调整类型
     * @param amount 调整金额
     * @return 调整后的应收款记录
     * @throws IllegalArgumentException 应收款不存在或调整后金额不合法
     */
    Receivable adjustReceivable(Long receivableId, Integer adjustType, BigDecimal amount);

    /**
     * 现金还款
     *
     * 债务人通过银行转账等方式现金还款，还款后更新应收款待还余额。
     * 若还清则状态变更为"已结清"，否则为"部分还款"。
     *
     * @param receivableId 应收款记录ID
     * @param amount 还款金额
     * @param paymentVoucher 支付凭证（银行转账凭证号或截图URL）
     * @return 创建的还款记录
     * @throws IllegalArgumentException 应收款不存在或还款金额超过待还余额
     */
    RepaymentRecord cashRepayment(Long receivableId, BigDecimal amount, String paymentVoucher);

    /**
     * 仓单抵债
     *
     * 债务人使用仓单（仓单质押贷款形成的资产）抵消应收款。
     * 仓单由金融机构持有，通过签名哈希验证权属。
     *
     * @param receivableId 应收款记录ID
     * @param receiptId 仓单记录ID
     * @param offsetPrice 抵债金额（仓单评估价值）
     * @param signatureHash 仓单持有人签名哈希
     * @return 创建的还款记录
     * @throws IllegalArgumentException 应收款或仓单不存在，或抵债金额不匹配
     */
    RepaymentRecord offsetWithCollateral(Long receivableId, Long receiptId, BigDecimal offsetPrice, String signatureHash);

    /**
     * 查询应收款的还款记录列表
     *
     * @param receivableId 应收款记录ID
     * @return 还款记录列表，按还款时间倒序
     */
    List<RepaymentRecord> listRepayments(Long receivableId);

    /**
     * 同步仓单拆分后的应收款关系
     *
     * 当仓单被拆分时，原有应收款对应的仓单ID需要更新为新的子仓单ID。
     * 此方法更新关联关系，确保拆分后的子仓单能正确关联到原应收款。
     *
     * @param parentReceiptId 父仓单ID（原仓单）
     * @param childReceiptIds 子仓单ID列表（拆分后的新仓单）
     * @return 同步后的应收款列表
     */
    List<Receivable> syncReceiptSplit(Long parentReceiptId, List<Long> childReceiptIds);

    /**
     * 应收款融资（保理）
     *
     * 金融机构购买应收款（保理），支付融资款给债权人，
     * 债务人后续还款给金融机构。应收款状态更新为"已融资"。
     *
     * @param receivableId 应收款记录ID
     * @param financeAmount 融资金额
     * @param financeEntId 金融机构企业ID
     * @return 融资后的应收款记录
     * @throws IllegalArgumentException 应收款不存在或状态不允许融资
     */
    Receivable financeReceivable(Long receivableId, BigDecimal financeAmount, Long financeEntId);

    /**
     * 结清应收款
     *
     * 当债务人完成全部还款后，将应收款状态更新为"已结清"。
     * 通常由系统在最后一笔还款后自动调用。
     *
     * @param receivableId 应收款记录ID
     * @return 结清后的应收款记录
     * @throws IllegalArgumentException 应收款不存在或尚有未还余额
     */
    Receivable settleReceivable(Long receivableId);
}