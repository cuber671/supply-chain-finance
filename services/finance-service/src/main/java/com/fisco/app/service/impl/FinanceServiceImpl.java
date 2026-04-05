package com.fisco.app.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.entity.Receivable;
import com.fisco.app.feign.BlockchainFeignClient;
import com.fisco.app.feign.LogisticsFeignClient;
import com.fisco.app.feign.WarehouseFeignClient;
import com.fisco.app.util.Result;
import com.fisco.app.entity.RepaymentRecord;
import com.fisco.app.mapper.ReceivableMapper;
import com.fisco.app.mapper.RepaymentRecordMapper;
import com.fisco.app.service.FinanceService;
import com.fisco.app.util.CurrentUser;

/**
 * 金融服务实现类
 */
@Service
public class FinanceServiceImpl implements FinanceService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceServiceImpl.class);

    @Autowired
    private ReceivableMapper receivableMapper;

    @Autowired
    private RepaymentRecordMapper repaymentRecordMapper;

    @Autowired(required = false)
    private BlockchainFeignClient blockchainFeignClient;

    @Autowired(required = false)
    private WarehouseFeignClient warehouseFeignClient;

    @Autowired(required = false)
    private LogisticsFeignClient logisticsFeignClient;

    @Override
    @Transactional
    public Receivable generateReceivable(Long voucherId, BigDecimal unitPrice) {
        if (voucherId == null) {
            throw new IllegalArgumentException("物流单ID不能为空");
        }
        // H7: 校验 voucherId 有效性（未来应调用 logistics-service 验证）
        if (voucherId <= 0) {
            throw new IllegalArgumentException("物流单ID无效");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("单价必须大于0");
        }

        // H1: 正确逻辑应从物流单获取货物数量后计算 initialAmount = unitPrice * transportQuantity
        BigDecimal transportQuantity = BigDecimal.ONE; // 默认值，防止物流服务不可用时出错
        if (logisticsFeignClient != null) {
            try {
                Result<?> result = logisticsFeignClient.getDelegateById(voucherId.toString());
                if (result != null && result.getCode() == 200 && result.getData() != null) {
                    // 解析 transportQuantity - Result.data is LinkedHashMap when JSON serialized
                    Object data = result.getData();
                    if (data instanceof Map) {
                        Map<?, ?> delegateMap = (Map<?, ?>) data;
                        Object qty = delegateMap.get("transportQuantity");
                        if (qty != null) {
                            transportQuantity = new BigDecimal(qty.toString());
                        }
                    }
                } else {
                    logger.warn("获取物流单信息失败: voucherId={}, code={}", voucherId, result != null ? result.getCode() : "null");
                }
            } catch (Exception e) {
                logger.warn("调用物流服务异常，使用默认值1: voucherId={}", voucherId, e);
            }
        } else {
            logger.warn("LogisticsFeignClient 未注入，跳过物流单查询，voucherId={}", voucherId);
        }
        BigDecimal initialAmount = unitPrice.multiply(transportQuantity);

        String receivableNo = "AR" + System.currentTimeMillis();

        Receivable receivable = new Receivable();
        receivable.setReceivableNo(receivableNo);
        receivable.setSourceVoucherId(voucherId);
        // H9: 设置债权人和债务人信息
        Long creditorEntId = CurrentUser.getEntId();
        receivable.setCreditorEntId(creditorEntId);
        receivable.setDebtorEntId(creditorEntId); // 临时方案：债务人待业务确认
        receivable.setInitialAmount(initialAmount);
        receivable.setAdjustedAmount(initialAmount);
        receivable.setCollectedAmount(BigDecimal.ZERO);
        receivable.setBalanceUnpaid(initialAmount);
        receivable.setCurrency("CNY");
        receivable.setDueDate(LocalDateTime.now().plusDays(30));
        receivable.setStatus(Receivable.STATUS_PENDING);
        receivable.setIsFinanced(0);
        // H9: 业务场景设置 - 默认入库生成场景,债务人待业务确认后修正
        receivable.setBusinessScene(Receivable.SCENE_STOCK_IN);

        receivableMapper.insert(receivable);

        logger.info("生成应收款: receivableNo={}, initialAmount={}, voucherId={}",
                receivableNo, initialAmount, voucherId);

        // 区块链上链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.ReceivableCreateRequest request = new BlockchainFeignClient.ReceivableCreateRequest();
                request.setReceivableId(receivableNo);
                request.setInitialAmount(initialAmount.longValue());
                request.setDueDate(receivable.getDueDate().toLocalDate().toEpochDay());
                request.setBusinessScene(receivable.getBusinessScene() != null ? receivable.getBusinessScene() : Receivable.SCENE_STOCK_IN);
                Result<String> result = blockchainFeignClient.createReceivable(request);
                // 【新问题修复】检查响应码确保区块链调用真正成功
                if (result == null || result.getCode() != 0) {
                    String errMsg = "应收账款区块链创建失败: receivableNo=" + receivableNo + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    receivable.setChainTxHash(result.getData());
                    receivableMapper.updateById(receivable);
                }
                logger.info("应收账款上链成功: receivableNo={}, chainTxHash={}", receivableNo, result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("应收账款上链失败: receivableNo={}", receivableNo, e);
                throw new RuntimeException("区块链操作失败，应收款创建已回滚", e);
            }
        }

        return receivable;
    }

    @Override
    public Receivable getReceivableById(Long id) {
        if (id == null) {
            return null;
        }
        return receivableMapper.selectById(id);
    }

    @Override
    public Receivable getReceivableByNo(String receivableNo) {
        if (receivableNo == null || receivableNo.isBlank()) {
            return null;
        }
        return receivableMapper.selectByReceivableNo(receivableNo);
    }

    @Override
    public List<Receivable> listByCreditor(Long creditorEntId) {
        if (creditorEntId == null) {
            return List.of();
        }
        return receivableMapper.selectByCreditorEntId(creditorEntId);
    }

    @Override
    public List<Receivable> listByDebtor(Long debtorEntId) {
        if (debtorEntId == null) {
            return List.of();
        }
        return receivableMapper.selectByDebtorEntId(debtorEntId);
    }

    @Override
    @Transactional
    public Receivable confirmReceivable(Long receivableId, String signature) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        if (receivable.getStatus() != Receivable.STATUS_PENDING) {
            throw new IllegalStateException("应收款状态不是待确认，无法确认，当前状态: " + receivable.getStatus());
        }

        receivable.setStatus(Receivable.STATUS_ACTIVE);
        receivable.setSignature(signature);
        receivableMapper.updateById(receivable);

        logger.info("确认应收款: receivableId={}, receivableNo={}", receivableId, receivable.getReceivableNo());

        // 区块链上链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.ReceivableConfirmRequest request = new BlockchainFeignClient.ReceivableConfirmRequest();
                request.setReceivableId(receivable.getReceivableNo());
                // 【新问题修复】检查响应码确保区块链调用真正成功
                Result<String> result = blockchainFeignClient.confirmReceivable(request);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "应收账款确认区块链失败: receivableNo=" + receivable.getReceivableNo() + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    receivable.setChainTxHash(result.getData());
                    receivableMapper.updateById(receivable);
                }
                logger.info("应收账款确认上链成功: receivableNo={}, chainTxHash={}", receivable.getReceivableNo(), result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("应收账款确认上链失败: receivableNo={}", receivable.getReceivableNo(), e);
                throw new RuntimeException("区块链操作失败，应收款确认已回滚", e);
            }
        }

        return receivable;
    }

    @Override
    @Transactional
    public Receivable adjustReceivable(Long receivableId, Integer adjustType, BigDecimal amount) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        if (receivable.getStatus() != Receivable.STATUS_ACTIVE) {
            throw new IllegalStateException("应收款状态不是生效中，无法调整，当前状态: " + receivable.getStatus());
        }

        if (adjustType == null || (adjustType != 1 && adjustType != 2)) {
            throw new IllegalArgumentException("调整类型必须是1(物流损耗扣减)或2(仓单拆分同步)");
        }

        // M3: 校验调整类型与金额语义匹配 - 损耗扣减和拆分同步都应减少金额
        if ((adjustType == 1 || adjustType == 2) && amount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("调整类型为扣减时，金额必须为负数");
        }

        BigDecimal newAdjustedAmount = receivable.getAdjustedAmount().add(amount);

        if (newAdjustedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("调整后金额不能为负数");
        }

        BigDecimal balanceDiff = receivable.getAdjustedAmount().subtract(newAdjustedAmount);
        receivable.setAdjustedAmount(newAdjustedAmount);
        receivable.setBalanceUnpaid(receivable.getBalanceUnpaid().add(balanceDiff));
        receivableMapper.updateById(receivable);

        logger.info("调整应收款: receivableId={}, adjustType={}, oldAmount={}, newAmount={}",
                receivableId, adjustType, receivable.getAdjustedAmount(), newAdjustedAmount);

        return receivable;
    }

    @Override
    @Transactional
    public RepaymentRecord cashRepayment(Long receivableId, BigDecimal amount, String paymentVoucher) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("还款金额必须大于0");
        }

        if (amount.compareTo(receivable.getBalanceUnpaid()) > 0) {
            throw new IllegalArgumentException("还款金额超过待还余额");
        }

        String repaymentNo = "REP" + System.currentTimeMillis();

        RepaymentRecord record = new RepaymentRecord();
        record.setReceivableId(receivableId);
        record.setRepaymentNo(repaymentNo);
        record.setRepaymentType(1);
        record.setAmount(amount);
        record.setCurrency("CNY");
        record.setPaymentVoucher(paymentVoucher);
        record.setRepaymentTime(LocalDateTime.now());

        repaymentRecordMapper.insert(record);

        BigDecimal newCollected = receivable.getCollectedAmount().add(amount);
        BigDecimal newBalance = receivable.getBalanceUnpaid().subtract(amount);
        receivable.setCollectedAmount(newCollected);
        receivable.setBalanceUnpaid(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            receivable.setStatus(Receivable.STATUS_SETTLED);
        } else {
            receivable.setStatus(Receivable.STATUS_PARTIAL_REPAYMENT);
        }

        receivableMapper.updateById(receivable);

        // H6: 现金还款上链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.ReceivableSettleRequest request = new BlockchainFeignClient.ReceivableSettleRequest();
                request.setReceivableId(receivable.getReceivableNo());
                // 【新问题修复】添加result.getCode()检查
                Result<String> result = blockchainFeignClient.settleReceivable(request);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "现金还款区块链结算失败: receivableNo=" + receivable.getReceivableNo()
                        + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    record.setChainTxHash(result.getData());
                    repaymentRecordMapper.updateById(record);
                    logger.info("现金还款上链成功: receivableNo={}, amount={}, chainTxHash={}",
                        receivable.getReceivableNo(), amount, result.getData());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("现金还款上链失败: receivableNo={}", receivable.getReceivableNo(), e);
                throw new RuntimeException("区块链操作失败，现金还款已回滚", e);
            }
        }

        logger.info("现金还款成功: receivableId={}, amount={}, repaymentNo={}", receivableId, amount, repaymentNo);

        return record;
    }

    @Override
    @Transactional
    public RepaymentRecord offsetWithCollateral(Long receivableId, Long receiptId, BigDecimal offsetPrice, String signatureHash) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在");
        }

        // H5: 仓单归属校验 - 仓单必须属于债务人才能用于抵债
        if (receiptId != null && warehouseFeignClient != null) {
            Object receiptObj = warehouseFeignClient.getReceiptById(receiptId);
            if (receiptObj instanceof Map) {
                Map<?, ?> receipt = (Map<?, ?>) receiptObj;
                Object ownerEntId = receipt.get("ownerEntId");
                if (ownerEntId != null && receivable.getDebtorEntId() != null
                    && !ownerEntId.toString().equals(receivable.getDebtorEntId().toString())) {
                    throw new IllegalArgumentException("仓单不属于债务人，无权用于抵债");
                }
            }
        }

        String repaymentNo = "REP" + System.currentTimeMillis();

        RepaymentRecord record = new RepaymentRecord();
        record.setReceivableId(receivableId);
        record.setRepaymentNo(repaymentNo);
        record.setRepaymentType(2);
        record.setAmount(offsetPrice);
        record.setReceiptId(receiptId);
        record.setOffsetPrice(offsetPrice != null ? offsetPrice : BigDecimal.ZERO);
        record.setSignatureHash(signatureHash);
        record.setRepaymentTime(LocalDateTime.now());

        repaymentRecordMapper.insert(record);

        BigDecimal newCollected = receivable.getCollectedAmount().add(offsetPrice);
        BigDecimal newBalance = receivable.getBalanceUnpaid().subtract(offsetPrice);
        receivable.setCollectedAmount(newCollected);
        receivable.setBalanceUnpaid(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            receivable.setStatus(Receivable.STATUS_SETTLED);
        } else {
            receivable.setStatus(Receivable.STATUS_PARTIAL_REPAYMENT);
        }

        receivableMapper.updateById(receivable);

        logger.info("仓单抵债成功: receivableId={}, receiptId={}, offsetPrice={}", receivableId, receiptId, offsetPrice);

        // 区块链上链 - 仓单抵债需要上链记录
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.OffsetDebtRequest request = new BlockchainFeignClient.OffsetDebtRequest();
                request.setReceivableId(receivable.getReceivableNo());
                request.setReceiptId(receiptId != null ? receiptId.toString() : null);
                request.setOffsetAmount(offsetPrice != null ? offsetPrice.longValue() : 0L);
                // 【新问题修复】检查响应码确保区块链调用真正成功
                Result<String> result = blockchainFeignClient.offsetDebtWithCollateral(request);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "仓单抵债区块链失败: receivableNo=" + receivable.getReceivableNo() + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    receivable.setChainTxHash(result.getData());
                    receivableMapper.updateById(receivable);
                }
                logger.info("仓单抵债上链成功: receivableNo={}, chainTxHash={}", receivable.getReceivableNo(), result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("仓单抵债上链失败: receivableNo={}", receivable.getReceivableNo(), e);
                throw new RuntimeException("区块链操作失败，仓单抵债已回滚", e);
            }
        }

        return record;
    }

    @Override
    public List<RepaymentRecord> listRepayments(Long receivableId) {
        if (receivableId == null) {
            return List.of();
        }
        return repaymentRecordMapper.selectByReceivableId(receivableId);
    }

    @Override
    public List<Receivable> syncReceiptSplit(Long parentReceiptId, List<Long> childReceiptIds) {
        return List.of();
    }

    @Override
    @Transactional
    public Receivable financeReceivable(Long receivableId, BigDecimal financeAmount, Long financeEntId) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在");
        }

        receivable.setIsFinanced(1);
        receivable.setFinanceAmount(financeAmount);
        receivable.setFinanceEntId(financeEntId);
        receivableMapper.updateById(receivable);

        logger.info("应收款融资: receivableId={}, financeAmount={}, financeEntId={}", receivableId, financeAmount, financeEntId);

        return receivable;
    }

    @Override
    @Transactional
    public Receivable settleReceivable(Long receivableId) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在");
        }

        receivable.setStatus(Receivable.STATUS_SETTLED);
        receivable.setBalanceUnpaid(BigDecimal.ZERO);
        // 结算时同步 collectedAmount 与 adjustedAmount
        if (receivable.getAdjustedAmount() != null) {
            receivable.setCollectedAmount(receivable.getAdjustedAmount());
        }
        receivableMapper.updateById(receivable);

        logger.info("应收款结算: receivableId={}", receivableId);

        // 区块链上链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.ReceivableSettleRequest request = new BlockchainFeignClient.ReceivableSettleRequest();
                request.setReceivableId(receivable.getReceivableNo());
                // 【新问题修复】检查响应码确保区块链调用真正成功
                Result<String> result = blockchainFeignClient.settleReceivable(request);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "应收账款结算区块链失败: receivableNo=" + receivable.getReceivableNo() + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    receivable.setChainTxHash(result.getData());
                    receivableMapper.updateById(receivable);
                }
                logger.info("应收账款结算上链成功: receivableNo={}, chainTxHash={}", receivable.getReceivableNo(), result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("应收账款结算上链失败: receivableNo={}", receivable.getReceivableNo(), e);
                throw new RuntimeException("区块链操作失败，应收款结算已回滚", e);
            }
        }

        return receivable;
    }
}
