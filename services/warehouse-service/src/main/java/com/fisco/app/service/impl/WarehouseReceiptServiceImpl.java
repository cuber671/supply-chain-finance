package com.fisco.app.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fisco.app.feign.BlockchainFeignClient;
import com.fisco.app.util.CurrentUser;
import com.fisco.app.entity.ReceiptEndorsement;
import com.fisco.app.entity.ReceiptOperationLog;
import com.fisco.app.entity.StockOrder;
import com.fisco.app.entity.Warehouse;
import com.fisco.app.entity.WarehouseReceipt;
import com.fisco.app.mapper.ReceiptEndorsementMapper;
import com.fisco.app.mapper.ReceiptOperationLogMapper;
import com.fisco.app.mapper.StockOrderMapper;
import com.fisco.app.mapper.WarehouseMapper;
import com.fisco.app.mapper.WarehouseReceiptMapper;
import com.fisco.app.service.WarehouseReceiptService;

/**
 * 仓单业务服务实现类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class WarehouseReceiptServiceImpl implements WarehouseReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseReceiptServiceImpl.class);

    @Autowired
    private WarehouseReceiptMapper warehouseReceiptMapper;

    @Autowired
    private StockOrderMapper stockOrderMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private ReceiptEndorsementMapper endorsementMapper;

    @Autowired
    private ReceiptOperationLogMapper operationLogMapper;

    @Autowired(required = false)
    private BlockchainFeignClient blockchainFeignClient;

    // ==================== 入库单管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyStockIn(Long warehouseId, Long entId, Long userId, String goodsName,
            BigDecimal weight, String unit, String attachmentUrl) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("仓库ID不能为空");
        }
        if (goodsName == null || goodsName.isEmpty()) {
            throw new IllegalArgumentException("货物名称不能为空");
        }
        if (weight == null) {
            throw new IllegalArgumentException("货物重量不能为空");
        }
        if (unit == null || unit.isEmpty()) {
            throw new IllegalArgumentException("计量单位不能为空");
        }

        StockOrder stockOrder = new StockOrder();
        stockOrder.setWarehouseId(warehouseId);
        stockOrder.setEntId(entId);
        stockOrder.setUserId(userId);
        stockOrder.setGoodsName(goodsName);
        stockOrder.setWeight(weight);
        stockOrder.setUnit(unit);
        stockOrder.setAttachmentUrl(attachmentUrl);
        stockOrder.setStatus(StockOrder.STATUS_PENDING);

        stockOrderMapper.insert(stockOrder);
        logger.info("申请入库成功: stockOrderId={}", stockOrder.getId());
        return stockOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmStockOrder(Long stockOrderId) {
        StockOrder stockOrder = stockOrderMapper.selectById(stockOrderId);
        if (stockOrder == null) {
            throw new IllegalArgumentException("入库单不存在");
        }
        if (stockOrder.getStatus() != StockOrder.STATUS_PENDING) {
            throw new IllegalArgumentException("入库单状态不是待确认");
        }

        stockOrder.setStatus(StockOrder.STATUS_CONFIRMED);
        stockOrderMapper.updateById(stockOrder);
        logger.info("确认入库单成功: stockOrderId={}", stockOrderId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelStockOrder(Long stockOrderId) {
        StockOrder stockOrder = stockOrderMapper.selectById(stockOrderId);
        if (stockOrder == null) {
            throw new IllegalArgumentException("入库单不存在");
        }
        if (stockOrder.getStatus() != StockOrder.STATUS_PENDING) {
            throw new IllegalArgumentException("只有待确认的入库单才能取消");
        }

        stockOrder.setStatus(StockOrder.STATUS_CANCELLED);
        stockOrderMapper.updateById(stockOrder);
        logger.info("取消入库单成功: stockOrderId={}", stockOrderId);
        return true;
    }

    @Override
    public StockOrder getStockOrderById(Long stockOrderId) {
        return stockOrderMapper.selectById(stockOrderId);
    }

    @Override
    public StockOrder getStockOrderByStockNo(String stockNo) {
        LambdaQueryWrapper<StockOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOrder::getId, Long.parseLong(stockNo));
        return stockOrderMapper.selectOne(wrapper);
    }

    @Override
    public List<StockOrder> getStockOrdersByEntId(Long entId) {
        LambdaQueryWrapper<StockOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOrder::getEntId, entId);
        wrapper.orderByDesc(StockOrder::getCreateTime);
        return stockOrderMapper.selectList(wrapper);
    }

    @Override
    public IPage<StockOrder> getStockOrdersByEntIdPaginated(Long entId, int pageNum, int pageSize) {
        Page<StockOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StockOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockOrder::getEntId, entId);
        wrapper.orderByDesc(StockOrder::getCreateTime);
        return stockOrderMapper.selectPage(page, wrapper);
    }

    // ==================== 仓单签发 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long mintReceipt(Long stockOrderId, Long warehouseUserId, String onChainId) {
        StockOrder stockOrder = stockOrderMapper.selectById(stockOrderId);
        if (stockOrder == null) {
            throw new IllegalArgumentException("入库单不存在");
        }
        if (stockOrder.getStatus() != StockOrder.STATUS_CONFIRMED) {
            throw new IllegalArgumentException("入库单未确认，不能签发仓单");
        }

        Warehouse warehouse = warehouseMapper.selectById(stockOrder.getWarehouseId());
        if (warehouse == null) {
            throw new IllegalArgumentException("仓库不存在");
        }

        // FIX: 校验入库单企业必须与仓库企业一致，防止跨企业伪造仓单
        if (!stockOrder.getEntId().equals(warehouse.getEntId())) {
            throw new IllegalArgumentException("入库单企业与仓库企业不匹配，不能签发仓单");
        }

        WarehouseReceipt receipt = new WarehouseReceipt();
        receipt.setWarehouseId(stockOrder.getWarehouseId());
        receipt.setOnChainId(onChainId);
        receipt.setOwnerEntId(stockOrder.getEntId());
        receipt.setOwnerUserId(stockOrder.getUserId());
        receipt.setWarehouseEntId(warehouse.getEntId());
        receipt.setWarehouseUserId(warehouseUserId);
        receipt.setGoodsName(stockOrder.getGoodsName());
        receipt.setWeight(stockOrder.getWeight());
        receipt.setUnit(stockOrder.getUnit());
        receipt.setParentId(0L);
        receipt.setRootId(0L);
        receipt.setIsLocked(false);
        receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
        // FIX: 初始状态为PENDING上链，区块链成功后才改为SYNCED
        receipt.setOnChainStatus(WarehouseReceipt.ON_CHAIN_STATUS_PENDING);

        warehouseReceiptMapper.insert(receipt);
        logger.info("签发仓单成功: receiptId={}, onChainStatus=PENDING", receipt.getId());

        // 区块链上链
        if (blockchainFeignClient != null && onChainId != null) {
            try {
                BlockchainFeignClient.ReceiptIssueRequest request = new BlockchainFeignClient.ReceiptIssueRequest();
                request.setReceiptId(onChainId);
                request.setOwnerHash(receipt.getOwnerEntId().toString());
                request.setWarehouseHash(warehouse.getId().toString());
                request.setGoodsDetailHash(stockOrder.getGoodsName());
                request.setWeight(stockOrder.getWeight().longValue());
                request.setUnit(stockOrder.getUnit());
                request.setQuantity(1L);
                blockchainFeignClient.issueReceipt(request);

                // FIX: 区块链成功后更新onChainStatus为SYNCED
                receipt.setOnChainStatus(WarehouseReceipt.ON_CHAIN_STATUS_SYNCED);
                warehouseReceiptMapper.updateById(receipt);
                logger.info("仓单上链成功: receiptId={}, onChainId={}", receipt.getId(), onChainId);
            } catch (Exception e) {
                // FIX: 区块链失败时标记为FAILED，不再吞掉异常
                receipt.setOnChainStatus(WarehouseReceipt.ON_CHAIN_STATUS_FAILED);
                warehouseReceiptMapper.updateById(receipt);
                logger.error("仓单上链失败: receiptId={}, 已标记为FAILED", receipt.getId(), e);
            }
        } else {
            // 无区块链网关时标记为FAILED
            receipt.setOnChainStatus(WarehouseReceipt.ON_CHAIN_STATUS_FAILED);
            warehouseReceiptMapper.updateById(receipt);
        }

        return receipt.getId();
    }

    @Override
    public WarehouseReceipt getReceiptById(Long receiptId) {
        return warehouseReceiptMapper.selectById(receiptId);
    }

    @Override
    public WarehouseReceipt getReceiptByOnChainId(String onChainId) {
        LambdaQueryWrapper<WarehouseReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehouseReceipt::getOnChainId, onChainId);
        return warehouseReceiptMapper.selectOne(wrapper);
    }

    @Override
    public List<WarehouseReceipt> getReceiptsByEntId(Long entId) {
        LambdaQueryWrapper<WarehouseReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehouseReceipt::getOwnerEntId, entId);
        wrapper.orderByDesc(WarehouseReceipt::getCreateTime);
        return warehouseReceiptMapper.selectList(wrapper);
    }

    @Override
    public IPage<WarehouseReceipt> getReceiptsByEntIdPaginated(Long entId, int pageNum, int pageSize) {
        Page<WarehouseReceipt> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WarehouseReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehouseReceipt::getOwnerEntId, entId);
        wrapper.orderByDesc(WarehouseReceipt::getCreateTime);
        return warehouseReceiptMapper.selectPage(page, wrapper);
    }

    @Override
    public List<WarehouseReceipt> getInStockReceipts(Long entId) {
        LambdaQueryWrapper<WarehouseReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehouseReceipt::getOwnerEntId, entId);
        wrapper.eq(WarehouseReceipt::getStatus, WarehouseReceipt.STATUS_IN_STOCK);
        wrapper.orderByDesc(WarehouseReceipt::getCreateTime);
        return warehouseReceiptMapper.selectList(wrapper);
    }

    @Override
    public IPage<WarehouseReceipt> getInStockReceiptsPaginated(Long entId, int pageNum, int pageSize) {
        Page<WarehouseReceipt> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WarehouseReceipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehouseReceipt::getOwnerEntId, entId);
        wrapper.eq(WarehouseReceipt::getStatus, WarehouseReceipt.STATUS_IN_STOCK);
        wrapper.orderByDesc(WarehouseReceipt::getCreateTime);
        return warehouseReceiptMapper.selectPage(page, wrapper);
    }

    @Override
    public java.util.Map<String, Object> validateReceiptOwnership(Long receiptId, Long entId) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            result.put("valid", false);
            result.put("message", "仓单不存在");
            return result;
        }

        // 校验是否属于指定企业
        boolean isOwner = receipt.getOwnerEntId() != null
                && receipt.getOwnerEntId().equals(entId);

        result.put("valid", isOwner);
        result.put("receiptId", receipt.getId());
        result.put("ownerEntId", receipt.getOwnerEntId());
        result.put("ownerUserId", receipt.getOwnerUserId());
        result.put("status", receipt.getStatus());
        result.put("statusName", getStatusName(receipt.getStatus()));
        result.put("isLocked", receipt.getIsLocked());
        result.put("goodsName", receipt.getGoodsName());
        result.put("weight", receipt.getWeight());
        result.put("unit", receipt.getUnit());
        result.put("onChainId", receipt.getOnChainId());

        if (!isOwner) {
            result.put("message", "仓单不属于该企业");
        } else {
            result.put("message", "仓单所有权校验通过");
        }

        return result;
    }

    private String getStatusName(int status) {
        switch (status) {
            case WarehouseReceipt.STATUS_IN_STOCK: return "在库";
            case WarehouseReceipt.STATUS_PENDING_TRANSFER: return "待转让";
            case WarehouseReceipt.STATUS_SPLIT_MERGED: return "已拆分合并";
            case WarehouseReceipt.STATUS_BURNED: return "已核销";
            case WarehouseReceipt.STATUS_IN_TRANSIT: return "运输中";
            default: return "未知状态";
        }
    }

    // ==================== 背书转让 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long launchEndorsement(Long receiptId, Long transferorUserId, Long transfereeEntId,
            String signatureHash) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }
        if (receipt.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
            throw new IllegalArgumentException("仓单状态不是在库");
        }
        if (receipt.getIsLocked()) {
            throw new IllegalArgumentException("仓单已质押锁定");
        }

        // FIX: 校验只有仓单持有人才能发起转让
        if (!receipt.getOwnerUserId().equals(transferorUserId)) {
            throw new IllegalArgumentException("无权限：不是仓单持有人");
        }

        ReceiptEndorsement endorsement = new ReceiptEndorsement();
        endorsement.setReceiptId(receiptId);
        endorsement.setTransferorEntId(receipt.getOwnerEntId());
        endorsement.setTransferorUserId(transferorUserId);
        endorsement.setTransfereeEntId(transfereeEntId);
        endorsement.setSignatureHash(signatureHash);
        endorsement.setStatus(ReceiptEndorsement.STATUS_PENDING);

        // FIX: 调用区块链记录背书转让交易
        if (blockchainFeignClient != null && receipt.getOnChainId() != null) {
            try {
                BlockchainFeignClient.EndorsementRequest request = new BlockchainFeignClient.EndorsementRequest();
                request.setReceiptId(receipt.getOnChainId());
                request.setFromHash(receipt.getOwnerEntId().toString());
                request.setToHash(transfereeEntId.toString());
                var result = blockchainFeignClient.launchEndorsement(request);
                // FIX: 记录txHash用于追踪
                if (result != null && result.getData() != null) {
                    endorsement.setTxHash(result.getData());
                }
            } catch (Exception e) {
                logger.error("仓单区块链背书失败: receiptId={}", receiptId, e);
            }
        }

        endorsementMapper.insert(endorsement);

        // 更新仓单状态为待转让
        receipt.setStatus(WarehouseReceipt.STATUS_PENDING_TRANSFER);
        warehouseReceiptMapper.updateById(receipt);

        logger.info("发起背书转让成功: endorsementId={}", endorsement.getId());
        return endorsement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmEndorsement(Long endorsementId, Long transfereeUserId, boolean accept) {
        ReceiptEndorsement endorsement = endorsementMapper.selectById(endorsementId);
        if (endorsement == null) {
            throw new IllegalArgumentException("背书记录不存在");
        }
        if (endorsement.getStatus() != ReceiptEndorsement.STATUS_PENDING) {
            throw new IllegalArgumentException("背书状态不是待确认");
        }

        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(endorsement.getReceiptId());
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }

        if (accept) {
            // 更新仓单持有人
            receipt.setOwnerEntId(endorsement.getTransfereeEntId());
            receipt.setOwnerUserId(transfereeUserId);
            receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
            warehouseReceiptMapper.updateById(receipt);

            endorsement.setTransfereeUserId(transfereeUserId);
            endorsement.setStatus(ReceiptEndorsement.STATUS_ACCEPTED);
        } else {
            // 拒绝，恢复仓单状态
            receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
            warehouseReceiptMapper.updateById(receipt);

            endorsement.setStatus(ReceiptEndorsement.STATUS_REJECTED);
        }
        endorsement.setFinishTime(java.time.LocalDateTime.now());
        endorsementMapper.updateById(endorsement);

        logger.info("{}背书转让: endorsementId={}", accept ? "确认" : "拒绝", endorsementId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeEndorsement(Long endorsementId) {
        ReceiptEndorsement endorsement = endorsementMapper.selectById(endorsementId);
        if (endorsement == null) {
            throw new IllegalArgumentException("背书记录不存在");
        }
        if (endorsement.getStatus() != ReceiptEndorsement.STATUS_PENDING) {
            throw new IllegalArgumentException("只有待确认的背书才能撤回");
        }

        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(endorsement.getReceiptId());
        if (receipt != null) {
            receipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
            warehouseReceiptMapper.updateById(receipt);
        }

        endorsement.setStatus(ReceiptEndorsement.STATUS_REVOKED);
        endorsement.setFinishTime(java.time.LocalDateTime.now());
        endorsementMapper.updateById(endorsement);

        logger.info("撤回背书转让成功: endorsementId={}", endorsementId);
        return true;
    }

    @Override
    public List<ReceiptEndorsement> getEndorsementsByReceiptId(Long receiptId) {
        LambdaQueryWrapper<ReceiptEndorsement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReceiptEndorsement::getReceiptId, receiptId);
        wrapper.orderByDesc(ReceiptEndorsement::getCreateTime);
        return endorsementMapper.selectList(wrapper);
    }

    @Override
    public void checkEndorsementTargetPermission(Long endorsementId) {
        ReceiptEndorsement endorsement = endorsementMapper.selectById(endorsementId);
        if (endorsement == null) {
            throw new IllegalArgumentException("背书记录不存在");
        }
        Long currentEntId = CurrentUser.getEntId();
        if (currentEntId == null) {
            throw new IllegalArgumentException("无法获取当前企业信息");
        }
        if (!endorsement.getTransfereeEntId().equals(currentEntId)) {
            throw new IllegalArgumentException("无权限操作：仅被背书目标企业可确认");
        }
    }

    @Override
    public void checkEndorsementInitiatorPermission(Long endorsementId) {
        ReceiptEndorsement endorsement = endorsementMapper.selectById(endorsementId);
        if (endorsement == null) {
            throw new IllegalArgumentException("背书记录不存在");
        }
        Long currentEntId = CurrentUser.getEntId();
        if (currentEntId == null) {
            throw new IllegalArgumentException("无法获取当前企业信息");
        }
        if (!endorsement.getTransferorEntId().equals(currentEntId)) {
            throw new IllegalArgumentException("无权限操作：仅背书发起方可撤回");
        }
    }

    // ==================== 拆分/合并 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applySplit(Long receiptId, Long applyUserId, BigDecimal[] targetWeights) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }
        if (receipt.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
            throw new IllegalArgumentException("仓单状态不是在库");
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BigDecimal w : targetWeights) {
            totalWeight = totalWeight.add(w);
        }
        if (totalWeight.compareTo(receipt.getWeight()) != 0) {
            throw new IllegalArgumentException("拆分重量之和与原仓单重量不相等");
        }

        ReceiptOperationLog opLog = new ReceiptOperationLog();
        opLog.setOpType(ReceiptOperationLog.OP_TYPE_SPLIT);
        opLog.setSourceReceiptIds(String.valueOf(receiptId));
        opLog.setTargetReceiptIds(""); // 待执行后填充
        opLog.setTotalWeight(receipt.getWeight());
        opLog.setApplyEntId(receipt.getOwnerEntId());
        opLog.setApplyUserId(applyUserId);
        opLog.setExecuteEntId(receipt.getWarehouseEntId());
        opLog.setExecuteUserId(0L);
        opLog.setStatus(ReceiptOperationLog.STATUS_PENDING);
        // 存储拆分重量分配信息到remark字段（JSON格式）
        StringBuilder weightsJson = new StringBuilder("[");
        for (int i = 0; i < targetWeights.length; i++) {
            weightsJson.append(targetWeights[i].toString());
            if (i < targetWeights.length - 1) weightsJson.append(",");
        }
        weightsJson.append("]");
        opLog.setRemark("targetWeights=" + weightsJson.toString());

        operationLogMapper.insert(opLog);
        logger.info("发起拆分申请成功: opLogId={}", opLog.getId());
        return opLog.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyMerge(List<Long> receiptIds, Long applyUserId) {
        if (receiptIds == null || receiptIds.size() < 2) {
            throw new IllegalArgumentException("合并至少需要2个仓单");
        }

        List<WarehouseReceipt> receipts = warehouseReceiptMapper.selectBatchIds(receiptIds);
        for (WarehouseReceipt r : receipts) {
            if (r.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
                throw new IllegalArgumentException("仓单状态不是在庫");
            }
        }

        BigDecimal totalWeight = receipts.stream()
                .map(WarehouseReceipt::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReceiptOperationLog opLog = new ReceiptOperationLog();
        opLog.setOpType(ReceiptOperationLog.OP_TYPE_MERGE);
        opLog.setSourceReceiptIds(String.join(",", receiptIds.stream().map(String::valueOf).toArray(String[]::new)));
        opLog.setTargetReceiptIds("");
        opLog.setTotalWeight(totalWeight);
        opLog.setApplyEntId(receipts.get(0).getOwnerEntId());
        opLog.setApplyUserId(applyUserId);
        opLog.setExecuteEntId(receipts.get(0).getWarehouseEntId());
        opLog.setExecuteUserId(0L);
        opLog.setStatus(ReceiptOperationLog.STATUS_PENDING);

        operationLogMapper.insert(opLog);
        logger.info("发起合并申请成功: opLogId={}", opLog.getId());
        return opLog.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeSplitMerge(Long opLogId, Long executeUserId, boolean execute) {
        ReceiptOperationLog opLog = operationLogMapper.selectById(opLogId);
        if (opLog == null) {
            throw new IllegalArgumentException("操作记录不存在");
        }
        if (opLog.getStatus() != ReceiptOperationLog.STATUS_PENDING) {
            throw new IllegalArgumentException("操作状态不是待执行");
        }

        if (execute) {
            // 执行拆分或合并
            if (opLog.getOpType() == ReceiptOperationLog.OP_TYPE_SPLIT) {
                executeSplit(opLog);
            } else if (opLog.getOpType() == ReceiptOperationLog.OP_TYPE_MERGE) {
                executeMerge(opLog);
            }
            opLog.setStatus(ReceiptOperationLog.STATUS_COMPLETED);
        } else {
            // 驳回：恢复源仓单状态
            opLog.setStatus(ReceiptOperationLog.STATUS_REJECTED);
            if (opLog.getOpType() == ReceiptOperationLog.OP_TYPE_SPLIT) {
                // 驳回拆分，源仓单保持不变（因为源仓单从未被修改）
            }
        }
        opLog.setExecuteUserId(executeUserId);
        opLog.setFinishTime(java.time.LocalDateTime.now());
        operationLogMapper.updateById(opLog);

        logger.info("{}拆分合并操作: opLogId={}", execute ? "执行" : "驳回", opLogId);
        return true;
    }

    private void executeSplit(ReceiptOperationLog opLog) {
        // 解析目标重量分配
        String remark = opLog.getRemark();
        BigDecimal[] targetWeights = parseTargetWeights(remark);

        Long sourceReceiptId = Long.parseLong(opLog.getSourceReceiptIds());
        WarehouseReceipt sourceReceipt = warehouseReceiptMapper.selectById(sourceReceiptId);
        if (sourceReceipt == null) {
            throw new IllegalArgumentException("源仓单不存在");
        }

        // 创建N个新仓单
        StringBuilder newReceiptIds = new StringBuilder();
        for (int i = 0; i < targetWeights.length; i++) {
            WarehouseReceipt newReceipt = new WarehouseReceipt();
            newReceipt.setWarehouseId(sourceReceipt.getWarehouseId());
            newReceipt.setOnChainId(""); // 拆分后新生成，链上ID待上链
            newReceipt.setOwnerEntId(sourceReceipt.getOwnerEntId());
            newReceipt.setOwnerUserId(sourceReceipt.getOwnerUserId());
            newReceipt.setWarehouseEntId(sourceReceipt.getWarehouseEntId());
            newReceipt.setWarehouseUserId(sourceReceipt.getWarehouseUserId());
            newReceipt.setGoodsName(sourceReceipt.getGoodsName());
            newReceipt.setWeight(targetWeights[i]);
            newReceipt.setUnit(sourceReceipt.getUnit());
            newReceipt.setParentId(sourceReceiptId);
            newReceipt.setRootId(sourceReceipt.getRootId() == 0L ? sourceReceiptId : sourceReceipt.getRootId());
            newReceipt.setIsLocked(false);
            newReceipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
            newReceipt.setOnChainStatus(WarehouseReceipt.ON_CHAIN_STATUS_PENDING);

            warehouseReceiptMapper.insert(newReceipt);

            newReceiptIds.append(newReceipt.getId());
            if (i < targetWeights.length - 1) newReceiptIds.append(",");
        }

        // 将源仓单标记为已拆分合并
        sourceReceipt.setStatus(WarehouseReceipt.STATUS_SPLIT_MERGED);
        warehouseReceiptMapper.updateById(sourceReceipt);

        // 更新操作记录的目标仓单ID
        opLog.setTargetReceiptIds(newReceiptIds.toString());
        logger.info("执行仓单拆分: sourceReceiptId={}, newReceiptIds={}", sourceReceiptId, newReceiptIds);
    }

    private void executeMerge(ReceiptOperationLog opLog) {
        String[] sourceReceiptIdStrs = opLog.getSourceReceiptIds().split(",");
        List<Long> sourceReceiptIds = new java.util.ArrayList<>();
        for (String s : sourceReceiptIdStrs) {
            sourceReceiptIds.add(Long.parseLong(s.trim()));
        }

        List<WarehouseReceipt> sourceReceipts = warehouseReceiptMapper.selectBatchIds(sourceReceiptIds);
        if (sourceReceipts.isEmpty()) {
            throw new IllegalArgumentException("源仓单不存在");
        }

        // 使用第一个源仓单作为模板
        WarehouseReceipt template = sourceReceipts.get(0);
        BigDecimal totalWeight = sourceReceipts.stream()
                .map(WarehouseReceipt::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 创建合并后的新仓单
        WarehouseReceipt newReceipt = new WarehouseReceipt();
        newReceipt.setWarehouseId(template.getWarehouseId());
        newReceipt.setOnChainId(""); // 合并后新生成，链上ID待上链
        newReceipt.setOwnerEntId(template.getOwnerEntId());
        newReceipt.setOwnerUserId(template.getOwnerUserId());
        newReceipt.setWarehouseEntId(template.getWarehouseEntId());
        newReceipt.setWarehouseUserId(template.getWarehouseUserId());
        newReceipt.setGoodsName(template.getGoodsName());
        newReceipt.setWeight(totalWeight);
        newReceipt.setUnit(template.getUnit());
        newReceipt.setParentId(0L);
        newReceipt.setRootId(0L);
        newReceipt.setIsLocked(false);
        newReceipt.setStatus(WarehouseReceipt.STATUS_IN_STOCK);
        newReceipt.setOnChainStatus(WarehouseReceipt.ON_CHAIN_STATUS_PENDING);

        warehouseReceiptMapper.insert(newReceipt);

        // 将源仓单标记为已拆分合并
        for (WarehouseReceipt r : sourceReceipts) {
            r.setStatus(WarehouseReceipt.STATUS_SPLIT_MERGED);
            warehouseReceiptMapper.updateById(r);
        }

        // 更新操作记录的目标仓单ID
        opLog.setTargetReceiptIds(String.valueOf(newReceipt.getId()));
        logger.info("执行仓单合并: sourceReceiptIds={}, newReceiptId={}", opLog.getSourceReceiptIds(), newReceipt.getId());
    }

    private BigDecimal[] parseTargetWeights(String remark) {
        // 解析 remark 中的 targetWeights=[1.0,2.0,3.0] 格式
        if (remark == null || !remark.contains("targetWeights=")) {
            throw new IllegalArgumentException("拆分重量分配信息不存在");
        }
        int start = remark.indexOf("[");
        int end = remark.indexOf("]");
        if (start == -1 || end == -1) {
            throw new IllegalArgumentException("拆分重量分配信息格式错误");
        }
        String weightsStr = remark.substring(start + 1, end);
        String[] weightStrs = weightsStr.split(",");
        BigDecimal[] weights = new BigDecimal[weightStrs.length];
        for (int i = 0; i < weightStrs.length; i++) {
            weights[i] = new BigDecimal(weightStrs[i].trim());
        }
        return weights;
    }

    @Override
    public ReceiptOperationLog getOperationLogById(Long opLogId) {
        return operationLogMapper.selectById(opLogId);
    }

    // ==================== 质押/解押 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockReceipt(Long receiptId, String loanId) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }
        if (receipt.getIsLocked()) {
            throw new IllegalArgumentException("仓单已质押锁定");
        }

        receipt.setIsLocked(true);
        receipt.setLoanId(loanId);
        // FIX: 使用乐观锁，@Version注解会在更新时自动检查version
        try {
            warehouseReceiptMapper.updateById(receipt);
        } catch (Exception e) {
            // MyBatis Plus乐观锁失败抛出MyBatisPlusException，包装了ObjectOptimisticLockingFailureException
            if (e.getClass().getName().contains("OptimisticLock") || e.getMessage() != null && e.getMessage().contains("version")) {
                throw new IllegalStateException("仓单已被其他请求修改，请重试");
            }
            throw e;
        }
        logger.info("质押锁定仓单成功: receiptId={}, loanId={}", receiptId, loanId);

        // FIX: 区块链锁定失败时应记录异常而非吞掉，让调用方知道链上未锁定
        if (blockchainFeignClient != null && receipt.getOnChainId() != null) {
            try {
                BlockchainFeignClient.ReceiptOperationRequest request = new BlockchainFeignClient.ReceiptOperationRequest();
                request.setReceiptId(receipt.getOnChainId());
                blockchainFeignClient.lockReceipt(request);
                logger.info("仓单区块链锁定成功: receiptId={}", receiptId);
            } catch (Exception e) {
                // 链上锁定失败，但DB已锁定，抛出异常让调用方处理
                logger.error("仓单区块链锁定失败: receiptId={}, DB已锁定但链上未锁定", receiptId, e);
                throw new RuntimeException("仓单区块链锁定失败: " + e.getMessage());
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlockReceipt(Long receiptId) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }
        if (!receipt.getIsLocked()) {
            throw new IllegalArgumentException("仓单未质押锁定");
        }

        receipt.setIsLocked(false);
        receipt.setLoanId(null);
        warehouseReceiptMapper.updateById(receipt);
        logger.info("还款解押仓单成功: receiptId={}", receiptId);

        // FIX: 区块链解锁失败时应抛出异常，让调用方知道链上未解锁
        if (blockchainFeignClient != null && receipt.getOnChainId() != null) {
            try {
                BlockchainFeignClient.ReceiptOperationRequest request = new BlockchainFeignClient.ReceiptOperationRequest();
                request.setReceiptId(receipt.getOnChainId());
                blockchainFeignClient.unlockReceipt(request);
                logger.info("仓单区块链解锁成功: receiptId={}", receiptId);
            } catch (Exception e) {
                logger.error("仓单区块链解锁失败: receiptId={}, DB已解锁但链上未解锁", receiptId, e);
                throw new RuntimeException("仓单区块链解锁失败: " + e.getMessage());
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forceUnlockReceipt(Long receiptId, String reason) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }

        if (!receipt.getIsLocked()) {
            logger.warn("管理员强制解锁：仓单未锁定，跳过解锁操作: receiptId={}", receiptId);
            return true; // 未锁定视为成功
        }

        String previousLoanId = receipt.getLoanId();
        receipt.setIsLocked(false);
        receipt.setLoanId(null);
        warehouseReceiptMapper.updateById(receipt);
        logger.warn("管理员强制解锁仓单: receiptId={}, reason={}, previousLoanId={}", receiptId, reason, previousLoanId);

        // 区块链强制解锁
        if (blockchainFeignClient != null && receipt.getOnChainId() != null) {
            try {
                BlockchainFeignClient.ReceiptOperationRequest request = new BlockchainFeignClient.ReceiptOperationRequest();
                request.setReceiptId(receipt.getOnChainId());
                blockchainFeignClient.unlockReceipt(request);
                logger.info("仓单区块链强制解锁成功: receiptId={}", receiptId);
            } catch (Exception e) {
                logger.error("仓单区块链强制解锁失败: receiptId={}, DB已解锁但链上未解锁", receiptId, e);
                throw new RuntimeException("仓单区块链解锁失败: " + e.getMessage());
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidReceipt(Long receiptId, Long operatorUserId, String reason) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }

        // 仅在库、未锁定的仓单可作废
        if (receipt.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
            throw new IllegalArgumentException("只有在库状态的仓单可作废");
        }
        if (Boolean.TRUE.equals(receipt.getIsLocked())) {
            throw new IllegalArgumentException("已质押锁定的仓单不能作废");
        }

        // 检查是否有未完成的背书转让
        LambdaQueryWrapper<ReceiptEndorsement> endorsementWrapper = new LambdaQueryWrapper<>();
        endorsementWrapper.eq(ReceiptEndorsement::getReceiptId, receiptId);
        endorsementWrapper.eq(ReceiptEndorsement::getStatus, ReceiptEndorsement.STATUS_PENDING);
        long pendingEndorsements = endorsementMapper.selectCount(endorsementWrapper);
        if (pendingEndorsements > 0) {
            throw new IllegalArgumentException("仓单有待处理的背书转让，不能作废");
        }

        // 检查是否有未完成的拆分合并申请
        LambdaQueryWrapper<ReceiptOperationLog> opLogWrapper = new LambdaQueryWrapper<>();
        opLogWrapper.eq(ReceiptOperationLog::getSourceReceiptIds, receiptId.toString());
        opLogWrapper.eq(ReceiptOperationLog::getStatus, ReceiptOperationLog.STATUS_PENDING);
        long pendingOps = operationLogMapper.selectCount(opLogWrapper);
        if (pendingOps > 0) {
            throw new IllegalArgumentException("仓单有待处理的拆分合并申请，不能作废");
        }

        receipt.setStatus(WarehouseReceipt.STATUS_VOID);
        receipt.setRemark("作废原因: " + reason);
        warehouseReceiptMapper.updateById(receipt);

        logger.info("仓单作废成功: receiptId={}, operatorUserId={}, reason={}", receiptId, operatorUserId, reason);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelSplitMerge(Long opLogId, Long applyUserId) {
        ReceiptOperationLog opLog = operationLogMapper.selectById(opLogId);
        if (opLog == null) {
            throw new IllegalArgumentException("操作记录不存在");
        }
        if (opLog.getStatus() != ReceiptOperationLog.STATUS_PENDING) {
            throw new IllegalArgumentException("只有待执行的申请才能撤销");
        }
        // 仅申请人可撤销
        if (!opLog.getApplyUserId().equals(applyUserId)) {
            throw new IllegalArgumentException("无权限撤销：仅申请人可撤销自己的申请");
        }

        opLog.setStatus(ReceiptOperationLog.STATUS_CANCELLED);
        opLog.setFinishTime(java.time.LocalDateTime.now());
        opLog.setRemark(opLog.getRemark() + " | 申请人撤销");
        operationLogMapper.updateById(opLog);

        logger.info("拆分合并申请已撤销: opLogId={}, applyUserId={}", opLogId, applyUserId);
        return true;
    }

    // ==================== 核销出库 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyBurn(Long receiptId, Long applyUserId, String signatureHash) {
        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            throw new IllegalArgumentException("仓单不存在");
        }
        if (receipt.getStatus() != WarehouseReceipt.STATUS_IN_STOCK) {
            throw new IllegalArgumentException("仓单状态不是在庫");
        }
        if (receipt.getIsLocked()) {
            throw new IllegalArgumentException("仓单已质押锁定");
        }

        // 创建出库单（复用过库单状态）
        StockOrder stockOrder = new StockOrder();
        stockOrder.setWarehouseId(receipt.getWarehouseId());
        stockOrder.setEntId(receipt.getOwnerEntId());
        stockOrder.setUserId(applyUserId);
        // FIX: 保存receiptId用于confirmBurn时关联仓单，避免用goodsName匹配导致的错误关联
        stockOrder.setReceiptId(receiptId);
        stockOrder.setGoodsName(receipt.getGoodsName());
        stockOrder.setWeight(receipt.getWeight());
        stockOrder.setUnit(receipt.getUnit());
        stockOrder.setStatus(StockOrder.STATUS_PENDING);
        stockOrder.setRemark("核销出库");

        stockOrderMapper.insert(stockOrder);

        // 更新仓单状态
        receipt.setStatus(WarehouseReceipt.STATUS_BURNED);
        warehouseReceiptMapper.updateById(receipt);

        logger.info("申请核销出库成功: receiptId={}, stockOrderId={}", receiptId, stockOrder.getId());
        return stockOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmBurn(Long stockOrderId, Long warehouseUserId) {
        StockOrder stockOrder = stockOrderMapper.selectById(stockOrderId);
        if (stockOrder == null) {
            throw new IllegalArgumentException("出库单不存在");
        }

        // FIX: 使用receiptId直接获取仓单，避免goodsName匹配导致的错误关联
        WarehouseReceipt receipt = null;
        if (stockOrder.getReceiptId() != null) {
            receipt = warehouseReceiptMapper.selectById(stockOrder.getReceiptId());
        } else {
            // 兼容旧数据：使用goodsName匹配（建议旧数据迁移后移除此逻辑）
            logger.warn("出库单缺少receiptId关联，使用goodsName匹配: stockOrderId={}", stockOrderId);
            LambdaQueryWrapper<WarehouseReceipt> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WarehouseReceipt::getGoodsName, stockOrder.getGoodsName());
            wrapper.eq(WarehouseReceipt::getStatus, WarehouseReceipt.STATUS_BURNED);
            receipt = warehouseReceiptMapper.selectOne(wrapper);
        }

        if (receipt == null) {
            throw new IllegalArgumentException("关联的仓单不存在");
        }

        // FIX: 使用STATUS_COMPLETED表示已完成出库，STATUS_CANCELLED仅用于取消
        stockOrder.setStatus(StockOrder.STATUS_COMPLETED);
        stockOrderMapper.updateById(stockOrder);
        logger.info("确认核销出库成功: stockOrderId={}, receiptId={}", stockOrderId, receipt.getId());

        // 区块链核销
        if (blockchainFeignClient != null && receipt.getOnChainId() != null) {
            try {
                BlockchainFeignClient.BurnReceiptRequest request = new BlockchainFeignClient.BurnReceiptRequest();
                request.setReceiptId(receipt.getOnChainId());
                blockchainFeignClient.burnReceipt(request);
                logger.info("仓单区块链核销成功: receiptId={}", receipt.getId());
            } catch (Exception e) {
                logger.error("仓单区块链核销失败: receiptId={}", receipt.getId(), e);
            }
        }

        return true;
    }

    // ==================== 仓库管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouse(Long entId, String name, String address, String contactUser,
            String contactPhone) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("仓库名称不能为空");
        }
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("仓库地址不能为空");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setEntId(entId);
        warehouse.setName(name);
        warehouse.setAddress(address);
        warehouse.setContactUser(contactUser);
        warehouse.setContactPhone(contactPhone);
        warehouse.setStatus(Warehouse.STATUS_NORMAL);

        warehouseMapper.insert(warehouse);
        logger.info("创建仓库成功: warehouseId={}", warehouse.getId());
        return warehouse.getId();
    }

    @Override
    public List<Warehouse> getWarehousesByEntId(Long entId) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warehouse::getEntId, entId);
        wrapper.orderByDesc(Warehouse::getCreateTime);
        return warehouseMapper.selectList(wrapper);
    }

    @Override
    public IPage<Warehouse> getWarehousesByEntIdPaginated(Long entId, int pageNum, int pageSize) {
        Page<Warehouse> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Warehouse::getEntId, entId);
        wrapper.orderByDesc(Warehouse::getCreateTime);
        return warehouseMapper.selectPage(page, wrapper);
    }

    @Override
    public Warehouse getWarehouseById(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        return warehouseMapper.selectById(warehouseId);
    }

    // ==================== 溯源查询 ====================

    @Override
    public TraceInfo traceReceipt(Long receiptId) {
        TraceInfo traceInfo = new TraceInfo();

        WarehouseReceipt current = warehouseReceiptMapper.selectById(receiptId);
        traceInfo.setCurrentReceipt(current);

        // 查询历史记录
        LambdaQueryWrapper<WarehouseReceipt> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(WarehouseReceipt::getRootId, receiptId);
        historyWrapper.or().eq(WarehouseReceipt::getParentId, receiptId);
        traceInfo.setHistoryReceipts(warehouseReceiptMapper.selectList(historyWrapper));

        // 背书历史
        LambdaQueryWrapper<ReceiptEndorsement> endWrapper = new LambdaQueryWrapper<>();
        endWrapper.eq(ReceiptEndorsement::getReceiptId, receiptId);
        traceInfo.setEndorsementHistory(endorsementMapper.selectList(endWrapper));

        // 操作历史
        LambdaQueryWrapper<ReceiptOperationLog> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.like(ReceiptOperationLog::getSourceReceiptIds, String.valueOf(receiptId));
        traceInfo.setOperationHistory(operationLogMapper.selectList(opWrapper));

        return traceInfo;
    }
}
