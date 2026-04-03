package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * 区块链网关服务 Feign 客户端
 *
 * 提供所有业务服务的区块链操作统一入口
 */
@FeignClient(name = "fisco-gateway-service", contextId = "blockchainFeignClient",
        url = "http://fisco-gateway-service:8087")
public interface BlockchainFeignClient {

    // ==================== 基础操作 (已存在) ====================

    @PostMapping("/api/v1/blockchain/tx/submit")
    Result<Object> submitTransaction(@RequestBody Object transactionRequest);

    @GetMapping("/api/v1/blockchain/tx/{txHash}")
    Result<Object> getTransactionStatus(@PathVariable("txHash") String txHash);

    @GetMapping("/api/v1/blockchain/receipt/{txHash}")
    Result<Object> getTransactionReceipt(@PathVariable("txHash") String txHash);

    @GetMapping("/api/v1/blockchain/block/latest")
    Result<Object> getLatestBlockNumber();

    @GetMapping("/api/v1/blockchain/status")
    Result<Object> getBlockchainStatus();

    // ==================== 企业操作 ====================

    @PostMapping("/api/v1/blockchain/enterprise/register")
    Result<String> registerEnterprise(@RequestBody EnterpriseRegisterRequest request);

    @PostMapping("/api/v1/blockchain/enterprise/update-status")
    Result<String> updateEnterpriseStatus(@RequestBody EnterpriseStatusRequest request);

    @PostMapping("/api/v1/blockchain/enterprise/update-credit-rating")
    Result<String> updateCreditRating(@RequestBody EnterpriseCreditRatingRequest request);

    @PostMapping("/api/v1/blockchain/enterprise/set-credit-limit")
    Result<String> setCreditLimit(@RequestBody EnterpriseCreditLimitRequest request);

    @GetMapping("/api/v1/blockchain/enterprise/{address}")
    Result<Map<String, Object>> getEnterprise(@PathVariable("address") String address);

    @GetMapping("/api/v1/blockchain/enterprise/by-credit-code/{creditCode}")
    Result<String> getEnterpriseByCreditCode(@PathVariable("creditCode") String creditCode);

    @GetMapping("/api/v1/blockchain/enterprise/list")
    Result<List<String>> getEnterpriseList();

    @GetMapping("/api/v1/blockchain/enterprise/valid/{address}")
    Result<Boolean> isEnterpriseValid(@PathVariable("address") String address);

    // ==================== 仓单操作 ====================

    @PostMapping("/api/v1/blockchain/receipt/issue")
    Result<String> issueReceipt(@RequestBody ReceiptIssueRequest request);

    @GetMapping("/api/v1/blockchain/receipt/{receiptId}")
    Result<Map<String, Object>> getReceipt(@PathVariable("receiptId") String receiptId);

    @GetMapping("/api/v1/blockchain/receipt/owner/{owner}")
    Result<List<String>> getReceiptIdsByOwner(@PathVariable("owner") String owner);

    @PostMapping("/api/v1/blockchain/receipt/launch-endorsement")
    Result<String> launchEndorsement(@RequestBody EndorsementRequest request);

    @PostMapping("/api/v1/blockchain/receipt/confirm-endorsement")
    Result<String> confirmEndorsement(@RequestBody EndorsementRequest request);

    @PostMapping("/api/v1/blockchain/receipt/split")
    Result<String> splitReceipt(@RequestBody SplitReceiptRequest request);

    @PostMapping("/api/v1/blockchain/receipt/merge")
    Result<String> mergeReceipts(@RequestBody MergeReceiptRequest request);

    @PostMapping("/api/v1/blockchain/receipt/lock")
    Result<String> lockReceipt(@RequestBody ReceiptOperationRequest request);

    @PostMapping("/api/v1/blockchain/receipt/unlock")
    Result<String> unlockReceipt(@RequestBody ReceiptOperationRequest request);

    @PostMapping("/api/v1/blockchain/receipt/burn")
    Result<String> burnReceipt(@RequestBody BurnReceiptRequest request);

    @PostMapping("/api/v1/blockchain/receipt/transfer")
    Result<String> transferReceipt(@RequestBody TransferReceiptRequest request);

    @PostMapping("/api/v1/blockchain/receipt/cancel")
    Result<String> cancelReceipt(@RequestBody CancelReceiptRequest request);

    // ==================== 物流操作 ====================

    @PostMapping("/api/v1/blockchain/logistics/create")
    Result<String> createLogisticsDelegate(@RequestBody LogisticsCreateRequest request);

    @PostMapping("/api/v1/blockchain/logistics/pickup")
    Result<String> pickup(@RequestBody LogisticsPickupRequest request);

    @PostMapping("/api/v1/blockchain/logistics/arrive-add")
    Result<String> arriveAndAddQuantity(@RequestBody LogisticsArriveAddRequest request);

    @PostMapping("/api/v1/blockchain/logistics/arrive-create")
    Result<String> arriveAndCreateReceipt(@RequestBody LogisticsArriveCreateRequest request);

    @PostMapping("/api/v1/blockchain/logistics/assign-carrier")
    Result<String> assignCarrier(@RequestBody LogisticsAssignCarrierRequest request);

    @PostMapping("/api/v1/blockchain/logistics/confirm-delivery")
    Result<String> confirmDelivery(@RequestBody LogisticsConfirmDeliveryRequest request);

    @PostMapping("/api/v1/blockchain/logistics/update-status")
    Result<String> updateLogisticsStatus(@RequestBody LogisticsUpdateStatusRequest request);

    @GetMapping("/api/v1/blockchain/logistics/track/{voucherNo}")
    Result<List<BigInteger>> getLogisticsTrack(@PathVariable("voucherNo") String voucherNo);

    @GetMapping("/api/v1/blockchain/logistics/valid/{voucherNo}")
    Result<Boolean> validateLogisticsDelegate(@PathVariable("voucherNo") String voucherNo);

    @PostMapping("/api/v1/blockchain/logistics/invalidate")
    Result<String> invalidateLogistics(@RequestBody LogisticsInvalidateRequest request);

    // ==================== 贷款操作 ====================

    @PostMapping("/api/v1/blockchain/loan/create")
    Result<String> createLoan(@RequestBody LoanCreateRequest request);

    @PostMapping("/api/v1/blockchain/loan/approve")
    Result<String> approveLoan(@RequestBody LoanApproveRequest request);

    @PostMapping("/api/v1/blockchain/loan/cancel")
    Result<String> cancelLoan(@RequestBody LoanCancelRequest request);

    @PostMapping("/api/v1/blockchain/loan/disburse")
    Result<String> disburseLoan(@RequestBody LoanDisburseRequest request);

    @PostMapping("/api/v1/blockchain/loan/repay")
    Result<String> recordLoanRepayment(@RequestBody LoanRepayRequest request);

    @PostMapping("/api/v1/blockchain/loan/mark-overdue")
    Result<String> markOverdue(@RequestBody LoanMarkOverdueRequest request);

    @PostMapping("/api/v1/blockchain/loan/mark-defaulted")
    Result<String> markDefaulted(@RequestBody LoanMarkDefaultedRequest request);

    @PostMapping("/api/v1/blockchain/loan/set-receipt")
    Result<String> setReceiptLoanId(@RequestBody LoanSetReceiptRequest request);

    @PostMapping("/api/v1/blockchain/loan/update-receipt")
    Result<String> updateReceiptLoanId(@RequestBody LoanUpdateReceiptRequest request);

    @GetMapping("/api/v1/blockchain/loan/core/{loanNo}")
    Result<String> getLoanCore(@PathVariable("loanNo") String loanNo);

    @GetMapping("/api/v1/blockchain/loan/status/{loanNo}")
    Result<Integer> getLoanStatus(@PathVariable("loanNo") String loanNo);

    @GetMapping("/api/v1/blockchain/loan/by-receipt/{receiptId}")
    Result<String> getLoanByReceipt(@PathVariable("receiptId") String receiptId);

    @GetMapping("/api/v1/blockchain/loan/exists/{loanNo}")
    Result<Boolean> loanExists(@PathVariable("loanNo") String loanNo);

    // ==================== 应收账款操作 ====================

    @PostMapping("/api/v1/blockchain/receivable/create")
    Result<String> createReceivable(@RequestBody ReceivableCreateRequest request);

    @PostMapping("/api/v1/blockchain/receivable/confirm")
    Result<String> confirmReceivable(@RequestBody ReceivableConfirmRequest request);

    @PostMapping("/api/v1/blockchain/receivable/adjust")
    Result<String> adjustReceivable(@RequestBody ReceivableAdjustRequest request);

    @PostMapping("/api/v1/blockchain/receivable/finance")
    Result<String> financeReceivable(@RequestBody ReceivableFinanceRequest request);

    @PostMapping("/api/v1/blockchain/receivable/settle")
    Result<String> settleReceivable(@RequestBody ReceivableSettleRequest request);

    @GetMapping("/api/v1/blockchain/receivable/{receivableId}")
    Result<Map<String, Object>> getReceivable(@PathVariable("receivableId") String receivableId);

    @GetMapping("/api/v1/blockchain/receivable/status/{receivableId}")
    Result<Integer> getReceivableStatus(@PathVariable("receivableId") String receivableId);

    @PostMapping("/api/v1/blockchain/receivable/record-repayment")
    Result<String> recordReceivableRepayment(@RequestBody ReceivableRecordRepaymentRequest request);

    @PostMapping("/api/v1/blockchain/receivable/record-full-repayment")
    Result<String> recordFullRepayment(@RequestBody ReceivableFullRepaymentRequest request);

    @PostMapping("/api/v1/blockchain/receivable/offset-debt")
    Result<String> offsetDebtWithCollateral(@RequestBody OffsetDebtRequest request);

    // ==================== 请求 DTO ====================

    // 企业请求
    class EnterpriseRegisterRequest {
        private String enterpriseAddress;
        private String creditCode;
        private Integer role;
        private String metadataHash;

        public String getEnterpriseAddress() { return enterpriseAddress; }
        public void setEnterpriseAddress(String v) { this.enterpriseAddress = v; }
        public String getCreditCode() { return creditCode; }
        public void setCreditCode(String v) { this.creditCode = v; }
        public Integer getRole() { return role; }
        public void setRole(Integer v) { this.role = v; }
        public String getMetadataHash() { return metadataHash; }
        public void setMetadataHash(String v) { this.metadataHash = v; }
    }

    class EnterpriseStatusRequest {
        private String enterpriseAddress;
        private Integer newStatus;

        public String getEnterpriseAddress() { return enterpriseAddress; }
        public void setEnterpriseAddress(String v) { this.enterpriseAddress = v; }
        public Integer getNewStatus() { return newStatus; }
        public void setNewStatus(Integer v) { this.newStatus = v; }
    }

    class EnterpriseCreditRatingRequest {
        private String enterpriseAddress;
        private Integer newRating;

        public String getEnterpriseAddress() { return enterpriseAddress; }
        public void setEnterpriseAddress(String v) { this.enterpriseAddress = v; }
        public Integer getNewRating() { return newRating; }
        public void setNewRating(Integer v) { this.newRating = v; }
    }

    class EnterpriseCreditLimitRequest {
        private String enterpriseAddress;
        private Long newLimit;

        public String getEnterpriseAddress() { return enterpriseAddress; }
        public void setEnterpriseAddress(String v) { this.enterpriseAddress = v; }
        public Long getNewLimit() { return newLimit; }
        public void setNewLimit(Long v) { this.newLimit = v; }
    }

    // 仓单请求
    class ReceiptIssueRequest {
        private String receiptId;
        private String ownerHash;
        private String warehouseHash;
        private String goodsDetailHash;
        private String locationPhotoHash;
        private String contractHash;
        private Long weight;
        private String unit;
        private Long quantity;
        private Long storageDate;
        private Long expiryDate;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getOwnerHash() { return ownerHash; }
        public void setOwnerHash(String v) { this.ownerHash = v; }
        public String getWarehouseHash() { return warehouseHash; }
        public void setWarehouseHash(String v) { this.warehouseHash = v; }
        public String getGoodsDetailHash() { return goodsDetailHash; }
        public void setGoodsDetailHash(String v) { this.goodsDetailHash = v; }
        public String getLocationPhotoHash() { return locationPhotoHash; }
        public void setLocationPhotoHash(String v) { this.locationPhotoHash = v; }
        public String getContractHash() { return contractHash; }
        public void setContractHash(String v) { this.contractHash = v; }
        public Long getWeight() { return weight; }
        public void setWeight(Long v) { this.weight = v; }
        public String getUnit() { return unit; }
        public void setUnit(String v) { this.unit = v; }
        public Long getQuantity() { return quantity; }
        public void setQuantity(Long v) { this.quantity = v; }
        public Long getStorageDate() { return storageDate; }
        public void setStorageDate(Long v) { this.storageDate = v; }
        public Long getExpiryDate() { return expiryDate; }
        public void setExpiryDate(Long v) { this.expiryDate = v; }
    }

    class EndorsementRequest {
        private String receiptId;
        private String fromHash;
        private String toHash;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getFromHash() { return fromHash; }
        public void setFromHash(String v) { this.fromHash = v; }
        public String getToHash() { return toHash; }
        public void setToHash(String v) { this.toHash = v; }
    }

    class SplitReceiptRequest {
        private String originalReceiptId;
        private List<String> newReceiptIds;
        private List<Long> weights;
        private List<String> ownerHashes;
        private String unit;

        public String getOriginalReceiptId() { return originalReceiptId; }
        public void setOriginalReceiptId(String v) { this.originalReceiptId = v; }
        public List<String> getNewReceiptIds() { return newReceiptIds; }
        public void setNewReceiptIds(List<String> v) { this.newReceiptIds = v; }
        public List<Long> getWeights() { return weights; }
        public void setWeights(List<Long> v) { this.weights = v; }
        public List<String> getOwnerHashes() { return ownerHashes; }
        public void setOwnerHashes(List<String> v) { this.ownerHashes = v; }
        public String getUnit() { return unit; }
        public void setUnit(String v) { this.unit = v; }
    }

    class MergeReceiptRequest {
        private List<String> sourceReceiptIds;
        private String targetReceiptId;
        private String targetOwnerHash;
        private String unit;
        private Long totalWeight;

        public List<String> getSourceReceiptIds() { return sourceReceiptIds; }
        public void setSourceReceiptIds(List<String> v) { this.sourceReceiptIds = v; }
        public String getTargetReceiptId() { return targetReceiptId; }
        public void setTargetReceiptId(String v) { this.targetReceiptId = v; }
        public String getTargetOwnerHash() { return targetOwnerHash; }
        public void setTargetOwnerHash(String v) { this.targetOwnerHash = v; }
        public String getUnit() { return unit; }
        public void setUnit(String v) { this.unit = v; }
        public Long getTotalWeight() { return totalWeight; }
        public void setTotalWeight(Long v) { this.totalWeight = v; }
    }

    class ReceiptOperationRequest {
        private String receiptId;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
    }

    class BurnReceiptRequest {
        private String receiptId;
        private String signatureHash;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String v) { this.signatureHash = v; }
    }

    class TransferReceiptRequest {
        private String receiptId;
        private String newOwnerHash;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getNewOwnerHash() { return newOwnerHash; }
        public void setNewOwnerHash(String v) { this.newOwnerHash = v; }
    }

    class CancelReceiptRequest {
        private String receiptId;
        private String reason;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }
    }

    // 物流请求
    class LogisticsCreateRequest {
        private String voucherNo;
        private Integer businessScene;
        private String receiptId;
        private Long transportQuantity;
        private String unit;
        private String ownerHash;
        private String carrierHash;
        private String sourceWhHash;
        private String targetWhHash;
        private Long validUntil;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public Integer getBusinessScene() { return businessScene; }
        public void setBusinessScene(Integer v) { this.businessScene = v; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public Long getTransportQuantity() { return transportQuantity; }
        public void setTransportQuantity(Long v) { this.transportQuantity = v; }
        public String getUnit() { return unit; }
        public void setUnit(String v) { this.unit = v; }
        public String getOwnerHash() { return ownerHash; }
        public void setOwnerHash(String v) { this.ownerHash = v; }
        public String getCarrierHash() { return carrierHash; }
        public void setCarrierHash(String v) { this.carrierHash = v; }
        public String getSourceWhHash() { return sourceWhHash; }
        public void setSourceWhHash(String v) { this.sourceWhHash = v; }
        public String getTargetWhHash() { return targetWhHash; }
        public void setTargetWhHash(String v) { this.targetWhHash = v; }
        public Long getValidUntil() { return validUntil; }
        public void setValidUntil(Long v) { this.validUntil = v; }
    }

    class LogisticsPickupRequest {
        private String voucherNo;
        private Long quantity;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public Long getQuantity() { return quantity; }
        public void setQuantity(Long v) { this.quantity = v; }
    }

    class LogisticsArriveAddRequest {
        private String voucherNo;
        private String targetReceiptId;
        private Long quantity;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public String getTargetReceiptId() { return targetReceiptId; }
        public void setTargetReceiptId(String v) { this.targetReceiptId = v; }
        public Long getQuantity() { return quantity; }
        public void setQuantity(Long v) { this.quantity = v; }
    }

    class LogisticsArriveCreateRequest {
        private String voucherNo;
        private String newReceiptId;
        private Long weight;
        private String unit;
        private String ownerHash;
        private String warehouseHash;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public String getNewReceiptId() { return newReceiptId; }
        public void setNewReceiptId(String v) { this.newReceiptId = v; }
        public Long getWeight() { return weight; }
        public void setWeight(Long v) { this.weight = v; }
        public String getUnit() { return unit; }
        public void setUnit(String v) { this.unit = v; }
        public String getOwnerHash() { return ownerHash; }
        public void setOwnerHash(String v) { this.ownerHash = v; }
        public String getWarehouseHash() { return warehouseHash; }
        public void setWarehouseHash(String v) { this.warehouseHash = v; }
    }

    class LogisticsAssignCarrierRequest {
        private String voucherNo;
        private String carrierHash;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public String getCarrierHash() { return carrierHash; }
        public void setCarrierHash(String v) { this.carrierHash = v; }
    }

    class LogisticsConfirmDeliveryRequest {
        private String voucherNo;
        private Integer action;
        private String targetReceiptId;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public Integer getAction() { return action; }
        public void setAction(Integer v) { this.action = v; }
        public String getTargetReceiptId() { return targetReceiptId; }
        public void setTargetReceiptId(String v) { this.targetReceiptId = v; }
    }

    class LogisticsUpdateStatusRequest {
        private String voucherNo;
        private Integer newStatus;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public Integer getNewStatus() { return newStatus; }
        public void setNewStatus(Integer v) { this.newStatus = v; }
    }

    class LogisticsInvalidateRequest {
        private String voucherNo;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
    }

    // 贷款请求
    class LoanCreateRequest {
        private String loanNo;
        private String borrowerHash;
        private String financeEntHash;
        private Double interestRate;
        private Long amount;
        private Integer loanDays;
        private String receiptId;
        private Long pledgeAmount;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getBorrowerHash() { return borrowerHash; }
        public void setBorrowerHash(String v) { this.borrowerHash = v; }
        public String getFinanceEntHash() { return financeEntHash; }
        public void setFinanceEntHash(String v) { this.financeEntHash = v; }
        public Double getInterestRate() { return interestRate; }
        public void setInterestRate(Double v) { this.interestRate = v; }
        public Long getAmount() { return amount; }
        public void setAmount(Long v) { this.amount = v; }
        public Integer getLoanDays() { return loanDays; }
        public void setLoanDays(Integer v) { this.loanDays = v; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public Long getPledgeAmount() { return pledgeAmount; }
        public void setPledgeAmount(Long v) { this.pledgeAmount = v; }
    }

    class LoanApproveRequest {
        private String loanNo;
        private Long approvedAmount;
        private Double interestRate;
        private Integer loanDays;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public Long getApprovedAmount() { return approvedAmount; }
        public void setApprovedAmount(Long v) { this.approvedAmount = v; }
        public Double getInterestRate() { return interestRate; }
        public void setInterestRate(Double v) { this.interestRate = v; }
        public Integer getLoanDays() { return loanDays; }
        public void setLoanDays(Integer v) { this.loanDays = v; }
    }

    class LoanCancelRequest {
        private String loanNo;
        private String reason;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }
    }

    class LoanDisburseRequest {
        private String loanNo;
        private String receiptId;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
    }

    class LoanRepayRequest {
        private String loanNo;
        private Long amount;
        private Long interestAmount;
        private Integer installmentIndex;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public Long getAmount() { return amount; }
        public void setAmount(Long v) { this.amount = v; }
        public Long getInterestAmount() { return interestAmount; }
        public void setInterestAmount(Long v) { this.interestAmount = v; }
        public Integer getInstallmentIndex() { return installmentIndex; }
        public void setInstallmentIndex(Integer v) { this.installmentIndex = v; }
    }

    class LoanMarkOverdueRequest {
        private String loanNo;
        private Integer overdueDays;
        private Double penaltyRate;
        private Long penaltyAmount;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public Integer getOverdueDays() { return overdueDays; }
        public void setOverdueDays(Integer v) { this.overdueDays = v; }
        public Double getPenaltyRate() { return penaltyRate; }
        public void setPenaltyRate(Double v) { this.penaltyRate = v; }
        public Long getPenaltyAmount() { return penaltyAmount; }
        public void setPenaltyAmount(Long v) { this.penaltyAmount = v; }
    }

    class LoanMarkDefaultedRequest {
        private String loanNo;
        private String disposalMethod;
        private Long disposalAmount;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getDisposalMethod() { return disposalMethod; }
        public void setDisposalMethod(String v) { this.disposalMethod = v; }
        public Long getDisposalAmount() { return disposalAmount; }
        public void setDisposalAmount(Long v) { this.disposalAmount = v; }
    }

    class LoanSetReceiptRequest {
        private String receiptId;
        private String loanNo;
        private Long pledgeAmount;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public Long getPledgeAmount() { return pledgeAmount; }
        public void setPledgeAmount(Long v) { this.pledgeAmount = v; }
    }

    class LoanUpdateReceiptRequest {
        private String receiptId;
        private String newLoanNo;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getNewLoanNo() { return newLoanNo; }
        public void setNewLoanNo(String v) { this.newLoanNo = v; }
    }

    // 应收账款请求
    class ReceivableCreateRequest {
        private String receivableId;
        private Long initialAmount;
        private Long dueDate;
        private String buyerSellerPairHash;
        private String invoiceHash;
        private String contractHash;
        private String goodsDetailHash;
        private Integer businessScene;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public Long getInitialAmount() { return initialAmount; }
        public void setInitialAmount(Long v) { this.initialAmount = v; }
        public Long getDueDate() { return dueDate; }
        public void setDueDate(Long v) { this.dueDate = v; }
        public String getBuyerSellerPairHash() { return buyerSellerPairHash; }
        public void setBuyerSellerPairHash(String v) { this.buyerSellerPairHash = v; }
        public String getInvoiceHash() { return invoiceHash; }
        public void setInvoiceHash(String v) { this.invoiceHash = v; }
        public String getContractHash() { return contractHash; }
        public void setContractHash(String v) { this.contractHash = v; }
        public String getGoodsDetailHash() { return goodsDetailHash; }
        public void setGoodsDetailHash(String v) { this.goodsDetailHash = v; }
        public Integer getBusinessScene() { return businessScene; }
        public void setBusinessScene(Integer v) { this.businessScene = v; }
    }

    class ReceivableConfirmRequest {
        private String receivableId;
        private String signature;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public String getSignature() { return signature; }
        public void setSignature(String v) { this.signature = v; }
    }

    class ReceivableAdjustRequest {
        private String receivableId;
        private Long adjustedAmount;
        private Integer adjustType;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public Long getAdjustedAmount() { return adjustedAmount; }
        public void setAdjustedAmount(Long v) { this.adjustedAmount = v; }
        public Integer getAdjustType() { return adjustType; }
        public void setAdjustType(Integer v) { this.adjustType = v; }
    }

    class ReceivableFinanceRequest {
        private String receivableId;
        private Long financeAmount;
        private String financeEntity;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public Long getFinanceAmount() { return financeAmount; }
        public void setFinanceAmount(Long v) { this.financeAmount = v; }
        public String getFinanceEntity() { return financeEntity; }
        public void setFinanceEntity(String v) { this.financeEntity = v; }
    }

    class ReceivableSettleRequest {
        private String receivableId;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
    }

    class ReceivableRecordRepaymentRequest {
        private String receivableId;
        private Long repaymentAmount;
        private Integer repaymentType;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public Long getRepaymentAmount() { return repaymentAmount; }
        public void setRepaymentAmount(Long v) { this.repaymentAmount = v; }
        public Integer getRepaymentType() { return repaymentType; }
        public void setRepaymentType(Integer v) { this.repaymentType = v; }
    }

    class ReceivableFullRepaymentRequest {
        private String receivableId;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
    }

    class OffsetDebtRequest {
        private String receivableId;
        private String receiptId;
        private Long offsetAmount;
        private String signatureHash;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public Long getOffsetAmount() { return offsetAmount; }
        public void setOffsetAmount(Long v) { this.offsetAmount = v; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String v) { this.signatureHash = v; }
    }
}
