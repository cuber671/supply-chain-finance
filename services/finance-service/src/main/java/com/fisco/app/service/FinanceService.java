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

    Receivable generateReceivable(Long voucherId, BigDecimal unitPrice);

    Receivable getReceivableById(Long id);

    Receivable getReceivableByNo(String receivableNo);

    List<Receivable> listByCreditor(Long creditorEntId);

    List<Receivable> listByDebtor(Long debtorEntId);

    Receivable confirmReceivable(Long receivableId, String signature);

    Receivable adjustReceivable(Long receivableId, Integer adjustType, BigDecimal amount);

    RepaymentRecord cashRepayment(Long receivableId, BigDecimal amount, String paymentVoucher);

    RepaymentRecord offsetWithCollateral(Long receivableId, Long receiptId, BigDecimal offsetPrice, String signatureHash);

    List<RepaymentRecord> listRepayments(Long receivableId);

    List<Receivable> syncReceiptSplit(Long parentReceiptId, List<Long> childReceiptIds);

    Receivable financeReceivable(Long receivableId, BigDecimal financeAmount, Long financeEntId);

    Receivable settleReceivable(Long receivableId);
}
