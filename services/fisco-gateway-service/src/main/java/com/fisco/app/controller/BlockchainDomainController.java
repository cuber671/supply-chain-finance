package com.fisco.app.controller;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.annotation.RequireRole;
import com.fisco.app.feign.BlockchainFeignClient.CancelReceiptRequest;
import com.fisco.app.feign.BlockchainFeignClient.LogisticsArriveCreateRequest;
import com.fisco.app.feign.BlockchainFeignClient.UpdateBalanceRequest;
import com.fisco.app.util.Result;
import com.fisco.app.service.impl.EnterpriseContractService;
import com.fisco.app.service.impl.WarehouseReceiptContractService;
import com.fisco.app.service.impl.LogisticsContractService;
import com.fisco.app.service.impl.LoanContractService;
import com.fisco.app.service.impl.ReceivableContractService;
import com.fisco.app.service.SignatureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 区块链业务域操作 Controller
 *
 * 提供企业、仓单、物流、贷款、应收账款等业务域的区块链操作
 */
@Tag(name = "区块链业务服务", description = "企业、仓单、物流、贷款、应收账款等业务域的区块链上链操作")
@RestController
@RequestMapping("/api/v1/blockchain")
public class BlockchainDomainController {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainDomainController.class);

    @Autowired
    private EnterpriseContractService enterpriseContractService;

    @Autowired
    private WarehouseReceiptContractService warehouseReceiptContractService;

    @Autowired
    private LogisticsContractService logisticsContractService;

    @Autowired
    private LoanContractService loanContractService;

    @Autowired
    private ReceivableContractService receivableContractService;

    // ==================== 企业操作 ====================

    @Operation(summary = "注册企业上链", description = "将企业信息注册到区块链上。需要ADMIN角色。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/enterprise/register")
    public Result<String> registerEnterprise(
            @Parameter(description = "企业注册请求信息", required = true)
            @RequestBody EnterpriseRegisterRequest request) {
        try {
            TransactionReceipt receipt = enterpriseContractService.registerEnterprise(
                    request.getEnterpriseAddress(),
                    request.getCreditCode(),
                    BigInteger.valueOf(request.getRole()),
                    hexStringToBytes(request.getMetadataHash())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("注册企业上链失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新企业状态上链", description = "将企业状态更新上链。需要ADMIN角色。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/enterprise/update-status")
    public Result<String> updateEnterpriseStatus(
            @Parameter(description = "企业状态更新请求信息", required = true)
            @RequestBody EnterpriseStatusRequest request) {
        try {
            TransactionReceipt receipt = enterpriseContractService.updateEnterpriseStatus(
                    request.getEnterpriseAddress(),
                    BigInteger.valueOf(request.getNewStatus())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("更新企业状态上链失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新企业信用评级上链", description = "将企业信用评级更新上链。需要ADMIN角色。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/enterprise/update-credit-rating")
    public Result<String> updateCreditRating(
            @Parameter(description = "企业信用评级更新请求信息", required = true)
            @RequestBody EnterpriseCreditRatingRequest request) {
        try {
            TransactionReceipt receipt = enterpriseContractService.updateCreditRating(
                    request.getEnterpriseAddress(),
                    BigInteger.valueOf(request.getNewRating())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("更新企业信用评级上链失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "设置企业授信额度上链", description = "将企业授信额度设置上链。需要ADMIN角色。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "设置成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/enterprise/set-credit-limit")
    public Result<String> setCreditLimit(
            @Parameter(description = "企业授信额度设置请求信息", required = true)
            @RequestBody EnterpriseCreditLimitRequest request) {
        try {
            TransactionReceipt receipt = enterpriseContractService.setCreditLimit(
                    request.getEnterpriseAddress(),
                    BigInteger.valueOf(request.getNewLimit())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("设置企业授信额度上链失败", e);
            String errMsg = e.getMessage();
            if (errMsg == null || errMsg.isEmpty()) {
                errMsg = "区块链交易失败，请检查：1.企业是否已注册 2.调用者是否为合约admin 3.额度值是否大于0";
            }
            return Result.error(500, "操作失败: " + errMsg);
        }
    }

    @Operation(summary = "获取企业信息", description = "根据企业区块链地址查询企业信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/enterprise/{address}")
    public Result<Map<String, Object>> getEnterprise(@PathVariable String address) {
        try {
            var info = enterpriseContractService.getEnterprise(address);
            return Result.success(Map.of(
                    "address", info.getAddress(),
                    "creditCode", info.getCreditCode(),
                    "role", info.getRole(),
                    "status", info.getStatus()
            ));
        } catch (Exception e) {
            logger.error("获取企业信息失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "根据信用代码获取企业地址", description = "根据企业信用代码查询对应的区块链地址。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/enterprise/by-credit-code/{creditCode}")
    public Result<String> getEnterpriseByCreditCode(@PathVariable String creditCode) {
        try {
            String address = enterpriseContractService.getEnterpriseByCreditCode(creditCode);
            return Result.success(address);
        } catch (Exception e) {
            logger.error("根据信用代码获取企业地址失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "获取企业列表", description = "查询区块链上所有注册企业的地址列表。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/enterprise/list")
    public Result<List<String>> getEnterpriseList() {
        try {
            List<String> list = enterpriseContractService.getEnterpriseList();
            return Result.success(list);
        } catch (Exception e) {
            logger.error("获取企业列表失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "验证企业有效性", description = "验证企业在区块链上是否有效注册。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/enterprise/valid/{address}")
    public Result<Boolean> isEnterpriseValid(@PathVariable String address) {
        try {
            boolean valid = enterpriseContractService.isEnterpriseValid(address);
            return Result.success(valid);
        } catch (Exception e) {
            logger.error("验证企业有效性失败", e);
            return Result.error(500, "操作失败");
        }
    }

    // ==================== 仓单操作 ====================

    @Operation(summary = "签发仓单", description = "将仓单信息签发上链。需要ADMIN或WAREHOUSE角色。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "签发成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole(value = {"ADMIN", "WAREHOUSE"}, adminBypass = true)
    @PostMapping("/receipt/issue")
    public Result<String> issueReceipt(
            @Parameter(description = "仓单签发请求信息", required = true)
            @RequestBody ReceiptIssueRequest request) {
        try {
            // 【诊断】记录接收到的原始字符串值
            logger.info("【网关接收】issueReceipt: receiptId={}, ownerHash='{}', warehouseHash='{}', weight={}",
                request.getReceiptId(), request.getOwnerHash(), request.getWarehouseHash(), request.getWeight());
            // 【YX-01修复】ownerHash和warehouseHash是entity ID（decimal数字），应使用entityIdToBytes32
            // 其他hash字段（goodsDetailHash, locationPhotoHash, contractHash）是真正的哈希值，使用hexStringToBytes
            byte[] ownerHashBytes = entityIdToBytes32(request.getOwnerHash());
            byte[] warehouseHashBytes = entityIdToBytes32(request.getWarehouseHash());
            // 【诊断】转换后的bytes32值（显示全部32字节）
            logger.info("【转换诊断】ownerHashBytes.length={}, 十六进制={}", ownerHashBytes.length, bytesToHexFull(ownerHashBytes));
            logger.info("【转换诊断】warehouseHashBytes.length={}, 十六进制={}", warehouseHashBytes.length, bytesToHexFull(warehouseHashBytes));
            TransactionReceipt receipt = warehouseReceiptContractService.issueReceipt(
                    request.getReceiptId(),
                    ownerHashBytes,
                    warehouseHashBytes,
                    hexStringToBytes(request.getGoodsDetailHash()),
                    hexStringToBytes(request.getLocationPhotoHash()),
                    hexStringToBytes(request.getContractHash()),
                    BigInteger.valueOf(request.getWeight()),
                    request.getUnit(),
                    BigInteger.valueOf(request.getQuantity()),
                    BigInteger.valueOf(request.getStorageDate()),
                    BigInteger.valueOf(request.getExpiryDate())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("签发仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    /**
     * 将 byte[] 转换为完整的十六进制字符串（显示所有字节）
     */
    private String bytesToHexFull(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return "0x" + sb.toString();
    }

    @Operation(summary = "发起仓单背书", description = "发起仓单背书转让上链操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发起成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/launch-endorsement")
    public Result<String> launchEndorsement(
            @Parameter(description = "仓单背书发起请求信息", required = true)
            @RequestBody EndorsementRequest request) {
        try {
            // 【YX-01修复】fromHash和toHash是entity ID（企业ID），应使用entityIdToBytes32
            TransactionReceipt receipt = warehouseReceiptContractService.launchEndorsement(
                    request.getReceiptId(),
                    entityIdToBytes32(request.getFromHash()),
                    entityIdToBytes32(request.getToHash())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("发起仓单背书失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "确认仓单背书", description = "确认仓单背书转让上链操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/confirm-endorsement")
    public Result<String> confirmEndorsement(
            @Parameter(description = "仓单背书确认请求信息", required = true)
            @RequestBody EndorsementRequest request) {
        try {
            // 【YX-01修复】fromHash和toHash是entity ID（企业ID），应使用entityIdToBytes32
            TransactionReceipt receipt = warehouseReceiptContractService.confirmEndorsement(
                    request.getReceiptId(),
                    entityIdToBytes32(request.getFromHash()),
                    entityIdToBytes32(request.getToHash())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("确认仓单背书失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "拆分仓单", description = "将一个仓单拆分为多个子仓单上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "拆分成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/split")
    public Result<String> splitReceipt(
            @Parameter(description = "仓单拆分请求信息", required = true)
            @RequestBody SplitReceiptRequest request) {
        try {
            // 【YX-01修复】ownerHashes是entity ID列表，应使用entityIdToBytes32
            List<byte[]> ownerHashes = null;
            if (request.getOwnerHashes() != null) {
                ownerHashes = request.getOwnerHashes().stream()
                        .map(this::entityIdToBytes32).collect(Collectors.toList());
            }
            List<byte[]> warehouseHashes = null;
            if (request.getWarehouseHashes() != null) {
                warehouseHashes = request.getWarehouseHashes().stream()
                        .map(this::entityIdToBytes32).collect(Collectors.toList());
            }
            TransactionReceipt receipt = warehouseReceiptContractService.splitReceipt(
                    request.getOriginalReceiptId(),
                    request.getNewReceiptIds(),
                    request.getWeights().stream().map(l -> BigInteger.valueOf(l.longValue())).collect(Collectors.toList()),
                    ownerHashes,
                    warehouseHashes,
                    request.getUnit()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("拆分仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "合并仓单", description = "将多个仓单合并为一个新仓单上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "合并成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/merge")
    public Result<String> mergeReceipts(
            @Parameter(description = "仓单合并请求信息", required = true)
            @RequestBody MergeReceiptRequest request) {
        try {
            // 【YX-01修复】targetOwnerHash是entity ID，应使用entityIdToBytes32
            TransactionReceipt receipt = warehouseReceiptContractService.mergeReceipts(
                    request.getSourceReceiptIds(),
                    request.getTargetReceiptId(),
                    entityIdToBytes32(request.getTargetOwnerHash()),
                    request.getUnit(),
                    BigInteger.valueOf(request.getTotalWeight())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("合并仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询仓单链上状态", description = "查询仓单在区块链上的当前状态，用于诊断链上/链下状态一致性。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/info/{receiptId}")
    public Result<?> getReceiptOnChainStatus(@PathVariable String receiptId) {
        try {
            WarehouseReceiptContractService.ReceiptInfo info =
                    warehouseReceiptContractService.getReceipt(receiptId);
            if (info == null) {
                return Result.error(404, "仓单不存在");
            }
            // status: 1=InStorage(在库), 6=Pledged(已质押)
            return Result.success(java.util.Map.of(
                    "receiptId", info.getReceiptId() != null ? info.getReceiptId() : "",
                    "status", info.getStatus() != null ? info.getStatus().intValue() : 0,
                    "statusDesc", statusDesc(info.getStatus()),
                    "warehouse", info.getWarehouse() != null ? info.getWarehouse() : "",
                    "weight", info.getWeight() != null ? info.getWeight().intValue() : 0
            ));
        } catch (Exception e) {
            logger.error("查询仓单链上状态失败: receiptId={}", receiptId, e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    private String statusDesc(java.math.BigInteger status) {
        if (status == null) return "未知";
        int s = status.intValue();
        switch (s) {
            case 0: return "None";
            case 1: return "InStorage(在库)";
            case 2: return "PendingTransfer(待转让)";
            case 3: return "SplitMerged(已拆分合并)";
            case 4: return "Burned(已核销)";
            case 5: return "InTransit(物流转运中)";
            case 6: return "Pledged(已质押)";
            default: return "未知(" + s + ")";
        }
    }

    @Operation(summary = "锁定仓单", description = "将仓单锁定上链（作为贷款质押）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "锁定成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/lock")
    public Result<String> lockReceipt(
            @Parameter(description = "仓单锁定请求信息", required = true)
            @RequestBody ReceiptOperationRequest request) {
        try {
            TransactionReceipt receipt = warehouseReceiptContractService.lockReceipt(request.getReceiptId());
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("锁定仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "解锁仓单", description = "将仓单解除锁定上链（还款后解除质押）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "解锁成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/unlock")
    public Result<String> unlockReceipt(
            @Parameter(description = "仓单解锁请求信息", required = true)
            @RequestBody ReceiptOperationRequest request) {
        try {
            TransactionReceipt receipt = warehouseReceiptContractService.unlockReceipt(request.getReceiptId());
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("解锁仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "设置仓单为物流转运中", description = "物流提货确认时调用，将仓单状态设为InTransit。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/set-in-transit")
    public Result<String> setInTransitReceipt(
            @Parameter(description = "仓单转运中请求信息", required = true)
            @RequestBody ReceiptOperationRequest request) {
        try {
            TransactionReceipt receipt = warehouseReceiptContractService.setInTransit(request.getReceiptId());
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("设置仓单为转运中失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "从物流转运中恢复到在库状态", description = "部分交付确认时调用，将仓单状态从InTransit恢复到InStorage。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/restore-from-transit")
    public Result<String> restoreFromTransitReceipt(
            @Parameter(description = "仓单恢复请求信息", required = true)
            @RequestBody ReceiptOperationRequest request) {
        try {
            TransactionReceipt receipt = warehouseReceiptContractService.restoreFromTransit(request.getReceiptId());
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("仓单从转运中恢复失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "核销仓单", description = "将仓单核销上链，完成出库操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "核销成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/burn")
    public Result<String> burnReceipt(
            @Parameter(description = "仓单核销请求信息", required = true)
            @RequestBody BurnReceiptRequest request) {
        try {
            // 【QS-05修复】合约burnReceipt的signatureHash参数未被使用（合约只有owner/admin/javaBackend授权校验）
            // 由于合约未实现签名验证，传入占位符值（空bytes32）
            // 注意：如果后续合约升级实现签名验证，需要在此处传入真实的signatureHash
            byte[] signatureHash = request.getSignatureHash() != null && !request.getSignatureHash().isEmpty()
                    ? hexStringToBytes(request.getSignatureHash())
                    : new byte[32]; // 传入空bytes32作为占位符
            TransactionReceipt receipt = warehouseReceiptContractService.burnReceipt(
                    request.getReceiptId(),
                    signatureHash
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("核销仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "取消仓单", description = "将仓单取消上链，用于直接移库等场景的仓单作废。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "取消成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/cancel")
    public Result<String> cancelReceipt(
            @Parameter(description = "仓单取消请求信息", required = true)
            @RequestBody CancelReceiptRequest request) {
        try {
            TransactionReceipt receipt = warehouseReceiptContractService.cancelReceipt(
                    request.getReceiptId(),
                    request.getReason()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("取消仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    // ==================== 物流操作 ====================

    @Operation(summary = "创建物流委托单", description = "将物流委托单信息创建上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/create")
    public Result<String> createLogisticsDelegate(
            @Parameter(description = "物流委派单创建请求信息", required = true)
            @RequestBody LogisticsCreateRequest request) {
        try {
            // 【YX-01修复】ownerHash, carrierHash, sourceWhHash, targetWhHash是entity ID，应使用entityIdToBytes32
            TransactionReceipt receipt = logisticsContractService.createLogisticsDelegate(
                    request.getVoucherNo(),
                    request.getBusinessScene(),
                    request.getReceiptId(),
                    BigInteger.valueOf(request.getTransportQuantity()),
                    request.getUnit(),
                    entityIdToBytes32(request.getOwnerHash()),
                    entityIdToBytes32(request.getCarrierHash()),
                    entityIdToBytes32(request.getSourceWhHash()),
                    entityIdToBytes32(request.getTargetWhHash()),
                    BigInteger.valueOf(request.getValidUntil())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("创建物流委托单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "提货确认", description = "物流提货确认上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/pickup")
    public Result<String> pickup(
            @Parameter(description = "物流提货请求信息", required = true)
            @RequestBody LogisticsPickupRequest request) {
        try {
            TransactionReceipt receipt = logisticsContractService.pickup(
                    request.getVoucherNo(),
                    BigInteger.valueOf(request.getQuantity())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("提货确认失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "到货增加数量", description = "物流到货增加数量上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/arrive-add")
    public Result<String> arriveAndAddQuantity(
            @Parameter(description = "物流到达新增请求信息", required = true)
            @RequestBody LogisticsArriveAddRequest request) {
        try {
            TransactionReceipt receipt = logisticsContractService.arriveAndAddQuantity(
                    request.getVoucherNo(),
                    request.getTargetReceiptId(),
                    BigInteger.valueOf(request.getQuantity())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("到货增加数量失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "到货创建仓单", description = "物流到货后创建新仓单上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/arrive-create")
    public Result<String> arriveAndCreateReceipt(
            @Parameter(description = "物流到达创建仓单请求信息", required = true)
            @RequestBody LogisticsArriveCreateRequest request) {
        try {
            // 【修复】ownerHash/warehouseHash 为 null 时使用 "0" 占位
            String ownerHash = request.getOwnerHash() != null && !request.getOwnerHash().isEmpty()
                ? request.getOwnerHash() : "0";
            String warehouseHash = request.getWarehouseHash() != null && !request.getWarehouseHash().isEmpty()
                ? request.getWarehouseHash() : "0";

            TransactionReceipt receipt = logisticsContractService.arriveAndCreateReceipt(
                    request.getVoucherNo(),
                    request.getNewReceiptId() != null ? request.getNewReceiptId() : "0",
                    request.getWeight() != null ? BigInteger.valueOf(request.getWeight()) : null,
                    request.getUnit(),
                    entityIdToBytes32(ownerHash),
                    entityIdToBytes32(warehouseHash)
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("到货创建仓单失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "分配承运人", description = "为物流委托单分配承运人上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "分配成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/assign-carrier")
    public Result<String> assignCarrier(
            @Parameter(description = "物流承运方指派请求信息", required = true)
            @RequestBody LogisticsAssignCarrierRequest request) {
        try {
            // 【YX-01修复】carrierHash是entity ID，应使用entityIdToBytes32
            TransactionReceipt receipt = logisticsContractService.assignCarrier(
                    request.getVoucherNo(),
                    entityIdToBytes32(request.getCarrierHash())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("分配承运人失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "确认交付", description = "确认物流交付上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/confirm-delivery")
    public Result<String> confirmDelivery(
            @Parameter(description = "物流交付确认请求信息", required = true)
            @RequestBody LogisticsConfirmDeliveryRequest request) {
        try {
            TransactionReceipt receipt = logisticsContractService.confirmDelivery(
                    request.getVoucherNo(),
                    request.getAction(),
                    request.getTargetReceiptId()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("确认交付失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新物流状态", description = "更新物流委托单状态上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/update-status")
    public Result<String> updateLogisticsStatus(
            @Parameter(description = "物流状态更新请求信息", required = true)
            @RequestBody LogisticsUpdateStatusRequest request) {
        try {
            TransactionReceipt receipt = logisticsContractService.updateStatus(
                    request.getVoucherNo(),
                    request.getNewStatus()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("更新物流状态失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取物流轨迹", description = "查询物流委托单的区块链轨迹记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/logistics/track/{voucherNo}")
    public Result<List<BigInteger>> getLogisticsTrack(@PathVariable String voucherNo) {
        try {
            List<BigInteger> track = logisticsContractService.getLogisticsTrack(voucherNo);
            return Result.success(track);
        } catch (Exception e) {
            logger.error("获取物流轨迹失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "验证物流委托", description = "验证物流委托单在区块链上是否有效。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/logistics/valid/{voucherNo}")
    public Result<Boolean> validateLogisticsDelegate(@PathVariable String voucherNo) {
        try {
            Boolean valid = logisticsContractService.validateLogisticsDelegate(voucherNo);
            return Result.success(valid);
        } catch (Exception e) {
            logger.error("验证物流委托失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "使物流委托单失效", description = "使物流委托单失效上链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/logistics/invalidate")
    public Result<String> invalidateLogistics(
            @Parameter(description = "物流委派单作废请求信息", required = true)
            @RequestBody LogisticsInvalidateRequest request) {
        try {
            TransactionReceipt receipt = logisticsContractService.invalidate(request.getVoucherNo());
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("使物流委托单失效失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    // ==================== 贷款操作 ====================

    @Operation(summary = "创建贷款", description = "在区块链上创建贷款记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/create")
    public Result<String> createLoan(
            @Parameter(description = "贷款创建请求信息", required = true)
            @RequestBody LoanCreateRequest request) {
        try {
            // Handle null pledgeAmount - it's not used in blockchain contract, only in DB
            BigInteger pledgeAmount = request.getPledgeAmount() != null
                    ? BigInteger.valueOf(request.getPledgeAmount())
                    : BigInteger.ZERO;

            TransactionReceipt receipt = loanContractService.createLoan(
                    request.getLoanNo(),
                    request.getBorrowerHash(),
                    request.getFinanceEntHash(),
                    BigInteger.valueOf(request.getInterestRate()),
                    BigInteger.valueOf(request.getAmount()),
                    BigInteger.valueOf(request.getLoanDays()),
                    request.getReceiptId(),
                    pledgeAmount
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("创建贷款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "审批贷款", description = "在区块链上审批贷款，记录审批金额、利率和期限。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审批成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/approve")
    public Result<String> approveLoan(
            @Parameter(description = "贷款审批请求信息", required = true)
            @RequestBody LoanApproveRequest request) {
        try {
            TransactionReceipt receipt = loanContractService.approveLoan(
                    request.getLoanNo(),
                    BigInteger.valueOf(request.getApprovedAmount()),
                    BigInteger.valueOf(request.getInterestRate().longValue()),
                    BigInteger.valueOf(request.getLoanDays())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("审批贷款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "取消贷款", description = "在区块链上取消贷款记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "取消成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/cancel")
    public Result<String> cancelLoan(
            @Parameter(description = "贷款取消请求信息", required = true)
            @RequestBody LoanCancelRequest request) {
        try {
            TransactionReceipt receipt = loanContractService.cancelLoan(
                    request.getLoanNo(),
                    request.getReason()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("取消贷款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "放款", description = "在区块链上执行放款操作，记录仓单与贷款的关联。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "放款成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/disburse")
    public Result<String> disburseLoan(
            @Parameter(description = "贷款放款请求信息", required = true)
            @RequestBody LoanDisburseRequest request) {
        try {
            TransactionReceipt receipt = loanContractService.disburseLoan(
                    request.getLoanNo(),
                    request.getReceiptId()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("放款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "记录还款", description = "在区块链上记录贷款还款信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "记录成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/repay")
    public Result<String> recordLoanRepayment(
            @Parameter(description = "贷款还款请求信息", required = true)
            @RequestBody LoanRepayRequest request) {
        try {
            // 处理 null 的 interestAmount（向后兼容）
            BigInteger interestAmount = request.getInterestAmount() != null
                    ? BigInteger.valueOf(request.getInterestAmount())
                    : BigInteger.ZERO;

            TransactionReceipt receipt = loanContractService.recordRepayment(
                    request.getLoanNo(),
                    BigInteger.valueOf(request.getAmount()),
                    interestAmount,
                    BigInteger.valueOf(request.getInstallmentIndex())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("记录还款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "标记逾期", description = "在区块链上标记贷款逾期信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标记成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/mark-overdue")
    public Result<String> markOverdue(
            @Parameter(description = "贷款标记逾期请求信息", required = true)
            @RequestBody LoanMarkOverdueRequest request) {
        try {
            TransactionReceipt receipt = loanContractService.markOverdue(
                    request.getLoanNo(),
                    BigInteger.valueOf(request.getOverdueDays()),
                    BigInteger.valueOf(request.getPenaltyRate().longValue()),
                    BigInteger.valueOf(request.getPenaltyAmount())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("标记逾期失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "标记违约", description = "在区块链上标记贷款违约信息及处置方式。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "标记成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/mark-defaulted")
    public Result<String> markDefaulted(
            @Parameter(description = "贷款标记违约请求信息", required = true)
            @RequestBody LoanMarkDefaultedRequest request) {
        try {
            TransactionReceipt receipt = loanContractService.markDefaulted(
                    request.getLoanNo(),
                    request.getDisposalMethod(),
                    BigInteger.valueOf(request.getDisposalAmount())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("标记违约失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "设置仓单-贷款关联", description = "在区块链上设置仓单与贷款的关联关系。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "设置成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/set-receipt")
    public Result<String> setReceiptLoanId(
            @Parameter(description = "贷款设置仓单请求信息", required = true)
            @RequestBody LoanSetReceiptRequest request) {
        try {
            TransactionReceipt receipt = loanContractService.setReceiptLoanId(
                    request.getReceiptId(),
                    request.getLoanNo(),
                    BigInteger.valueOf(request.getPledgeAmount())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("设置仓单-贷款关联失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新仓单-贷款关联", description = "在区块链上更新仓单与贷款的关联关系。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/loan/update-receipt")
    public Result<String> updateReceiptLoanId(
            @Parameter(description = "贷款更新仓单请求信息", required = true)
            @RequestBody LoanUpdateReceiptRequest request) {
        try {
            TransactionReceipt receipt = loanContractService.updateReceiptLoanId(
                    request.getReceiptId(),
                    request.getNewLoanNo()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("更新仓单-贷款关联失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取贷款核心信息", description = "根据贷款编号查询区块链上的贷款核心信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/loan/core/{loanNo}")
    public Result<String> getLoanCore(@PathVariable String loanNo) {
        try {
            String info = loanContractService.getLoanCore(loanNo);
            return Result.success(info);
        } catch (Exception e) {
            logger.error("获取贷款核心信息失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "获取贷款状态", description = "根据贷款编号查询区块链上的贷款状态。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Integer.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/loan/status/{loanNo}")
    public Result<Integer> getLoanStatus(@PathVariable String loanNo) {
        try {
            BigInteger status = loanContractService.getLoanStatus(loanNo);
            return Result.success(status.intValue());
        } catch (Exception e) {
            logger.error("获取贷款状态失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "获取仓单关联的贷款", description = "根据仓单ID查询关联的贷款编号。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/loan/by-receipt/{receiptId}")
    public Result<String> getLoanByReceipt(@PathVariable String receiptId) {
        try {
            String loanNo = loanContractService.getLoanByReceipt(receiptId);
            return Result.success(loanNo);
        } catch (Exception e) {
            logger.error("获取仓单关联的贷款失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "检查贷款是否存在", description = "检查指定贷款编号在区块链上是否存在。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/loan/exists/{loanNo}")
    public Result<Boolean> loanExists(@PathVariable String loanNo) {
        try {
            Boolean exists = loanContractService.exists(loanNo);
            return Result.success(exists);
        } catch (Exception e) {
            logger.error("检查贷款是否存在失败", e);
            return Result.error(500, "操作失败");
        }
    }

    // ==================== 应收账款操作 ====================

    @Operation(summary = "创建应收款", description = "在区块链上创建应收账款记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/create")
    public Result<String> createReceivable(
            @Parameter(description = "应收账款创建请求信息", required = true)
            @RequestBody ReceivableCreateRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.createReceivable(
                    request.getReceivableId(),
                    BigInteger.valueOf(request.getInitialAmount()),
                    BigInteger.valueOf(request.getDueDate()),
                    hexStringToBytes(request.getBuyerSellerPairHash()),
                    hexStringToBytes(request.getInvoiceHash()),
                    hexStringToBytes(request.getContractHash()),
                    hexStringToBytes(request.getGoodsDetailHash()),
                    BigInteger.valueOf(request.getBusinessScene())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("创建应收款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "确认应收款", description = "在区块链上确认应收账款。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/confirm")
    public Result<String> confirmReceivable(
            @Parameter(description = "应收账款确认请求信息", required = true)
            @RequestBody ReceivableConfirmRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.confirmReceivable(
                    request.getReceivableId(),
                    hexStringToBytes(request.getSignature())
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("确认应收款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "调整应收款", description = "在区块链上调整应收账款金额。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "调整成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/adjust")
    public Result<String> adjustReceivable(
            @Parameter(description = "应收账款调整请求信息", required = true)
            @RequestBody ReceivableAdjustRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.adjustReceivable(
                    request.getReceivableId(),
                    BigInteger.valueOf(request.getAdjustedAmount()),
                    request.getReason()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("调整应收款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "应收款融资", description = "在区块链上执行应收账款融资操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "融资成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/finance")
    public Result<String> financeReceivable(
            @Parameter(description = "应收账款融资请求信息", required = true)
            @RequestBody ReceivableFinanceRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.financeReceivable(
                    request.getReceivableId(),
                    BigInteger.valueOf(request.getFinanceAmount()),
                    request.getFinanceEntity()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("应收款融资失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "应收款结算", description = "在区块链上执行应收账款结算操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "结算成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/settle")
    public Result<String> settleReceivable(
            @Parameter(description = "应收账款结清请求信息", required = true)
            @RequestBody ReceivableSettleRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.settleReceivable(request.getReceivableId());
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("应收款结算失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新应收款余额", description = "扣减应收款余额，用于还款操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/update-balance")
    public Result<String> updateBalance(
            @Parameter(description = "应收账款余额更新请求信息", required = true)
            @RequestBody UpdateBalanceRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.updateBalance(
                    request.getReceivableId(),
                    BigInteger.valueOf(request.getAmount()),
                    request.getIsFull()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("应收款余额更新失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取应收款状态", description = "根据应收款ID查询区块链上的应收账款状态。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Integer.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receivable/status/{receivableId}")
    public Result<Integer> getReceivableStatus(@PathVariable String receivableId) {
        try {
            BigInteger status = receivableContractService.getReceivableStatus(receivableId);
            return Result.success(status.intValue());
        } catch (Exception e) {
            logger.error("获取应收款状态失败", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "获取应收款链上余额", description = "查询区块链上应收款的实际未还余额")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receivable/balance/{receivableId}")
    public Result<Long> getBalanceUnpaid(@PathVariable String receivableId) {
        try {
            BigInteger balance = receivableContractService.getBalanceUnpaid(receivableId);
            return Result.success(balance != null ? balance.longValue() : 0L);
        } catch (Exception e) {
            logger.error("获取应收款余额失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "记录还款", description = "在区块链上记录应收账款还款信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "记录成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/record-repayment")
    public Result<String> recordReceivableRepayment(
            @Parameter(description = "应收账款还款记录请求信息", required = true)
            @RequestBody ReceivableRecordRepaymentRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.recordRepayment(
                    request.getReceivableId(),
                    BigInteger.valueOf(request.getRepaymentAmount()),
                    request.getPaymentMethod()
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("记录还款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "记录全额还款", description = "在区块链上记录应收账款全额还款。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "记录成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/record-full-repayment")
    public Result<String> recordFullRepayment(
            @Parameter(description = "应收账款全额还款请求信息", required = true)
            @RequestBody ReceivableFullRepaymentRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.recordFullRepayment(request.getReceivableId());
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("记录全额还款失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "以物抵债", description = "在区块链上执行以物抵债操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/offset-debt")
    public Result<String> offsetDebtWithCollateral(
            @Parameter(description = "债务抵消请求信息", required = true)
            @RequestBody OffsetDebtRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.offsetDebtWithCollateral(
                    request.getReceivableId(),
                    request.getReceiptId(),
                    BigInteger.valueOf(request.getOffsetAmount()),
                    request.getSignatureHash() != null && !request.getSignatureHash().isEmpty()
                        ? hexStringToBytes(request.getSignatureHash())
                        : new byte[32]
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("以物抵债失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "仓单抵债", description = "用仓单作为抵押物抵消应收款债务。仓单必须处于已质押(Pledged)状态。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "抵债成功，返回交易哈希", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误或仓单未质押"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receivable/offset-debt-with-receipt")
    public Result<String> offsetDebtWithWarehouseReceipt(
            @Parameter(description = "仓单抵债请求信息", required = true)
            @RequestBody OffsetDebtWithReceiptRequest request) {
        try {
            TransactionReceipt receipt = receivableContractService.offsetDebtWithWarehouseReceipt(
                    request.getReceivableId(),
                    request.getReceiptId(),
                    BigInteger.valueOf(request.getOffsetAmount()),
                    request.getReason() != null ? request.getReason() : "warehouse_offset"
            );
            return Result.success(receipt != null ? receipt.getTransactionHash() : null);
        } catch (Exception e) {
            logger.error("仓单抵债失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }

    // ==================== 签名服务 ====================

    @Autowired
    private SignatureService signatureService;

    @Operation(summary = "数据签名", description = "使用 FISCO BCOS SDK 的 CryptoSuite.sign() 方法对数据进行签名，返回签名哈希。\n\n" +
            "**用途**：用于替代前端的 UUID 生成 signatureHash，增强签名安全性和可追溯性。\n\n" +
            "**签名算法**：根据配置的加密类型（ECDSA 或 SM2）自动选择。\n\n" +
            "**注意**：此接口需要认证（Authorization 头）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "签名成功", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：数据为空"),
        @ApiResponse(responseCode = "401", description = "未授权：无效或缺失 Token"),
        @ApiResponse(responseCode = "500", description = "服务端异常：区块链功能未启用或签名失败")
    })
    @PostMapping("/sign")
    public Result<String> sign(
            @Parameter(description = "签名请求：包含要签名的数据", required = true)
            @RequestBody SignRequest request) {
        try {
            if (request.getData() == null || request.getData().isEmpty()) {
                return Result.error(400, "签名数据不能为空");
            }

            String signatureHash = signatureService.sign(request.getData());
            logger.info("签名成功，数据长度: {}, 签名: {}", request.getData().length(), signatureHash);
            return Result.success(signatureHash);
        } catch (IllegalStateException e) {
            logger.error("签名服务不可用: {}", e.getMessage());
            return Result.error(500, "签名服务不可用: " + e.getMessage());
        } catch (Exception e) {
            logger.error("签名失败: {}", e.getMessage(), e);
            return Result.error(500, "签名失败: " + e.getMessage());
        }
    }

    @Operation(summary = "验证签名", description = "验证签名是否正确。\n\n" +
            "**注意**：此接口需要认证（Authorization 头）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证成功", content = @Content),
        @ApiResponse(responseCode = "400", description = "参数错误：数据或签名为空"),
        @ApiResponse(responseCode = "401", description = "未授权：无效或缺失 Token"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/sign/verify")
    public Result<Boolean> verifySign(
            @Parameter(description = "验证请求：包含原始数据和签名", required = true)
            @RequestBody SignVerifyRequest request) {
        try {
            if (request.getData() == null || request.getData().isEmpty()) {
                return Result.error(400, "签名数据不能为空");
            }
            if (request.getSignature() == null || request.getSignature().isEmpty()) {
                return Result.error(400, "签名不能为空");
            }

            boolean valid = signatureService.verify(request.getData(), request.getSignature());
            return Result.success(valid);
        } catch (Exception e) {
            logger.error("签名验证失败: {}", e.getMessage(), e);
            return Result.error(500, "验证失败: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Convert entity ID string to bytes32 for blockchain storage.
     * 【YX-01修复】统一entity ID转换格式：entity ID为decimal数字字符串（如"123456"），
     * 应作为decimal数字解析而非hex字符串，与LoanContractService.entityIdToBytes32保持一致。
     *
     * Example: entityId="123456" → bytes32 representing decimal 123456
     *          (not hex interpretation which would give bytes32[0x12, 0x34, 0x56])
     */
    private byte[] entityIdToBytes32(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            throw new IllegalArgumentException("entity ID 不能为空");
        }
        try {
            java.math.BigInteger bigInt = new java.math.BigInteger(entityId);
            byte[] valueBytes = bigInt.toByteArray();
            byte[] result = new byte[32];
            // 【诊断】调试日志
            logger.info("【entityIdToBytes32诊断】input='{}', valueBytes长度={}, valueBytes[0]={}, bigInt={}",
                entityId, valueBytes.length, valueBytes[0], bigInt);
            // BigInteger.toByteArray() returns sign+magnitude
            // If the first byte is 0, it means the actual data starts from index 1
            int start = (valueBytes[0] == 0) ? 1 : 0;
            int len = valueBytes.length - start;
            // Copy to the rightmost position of the 32-byte array
            if (len > 0) {
                System.arraycopy(valueBytes, start, result, 32 - len, len);
            }
            // 【诊断】调试日志
            logger.info("【entityIdToBytes32诊断】start={}, len={}, result首字节={}, result末字节={}",
                start, len, result[0], result[31]);
            return result;
        } catch (NumberFormatException e) {
            // entityId 不是有效数字，再次检查是否为空（防止空字符串）
            if (entityId.trim().isEmpty()) {
                throw new IllegalArgumentException("entity ID 不能为空");
            }
            // If not a valid decimal number, treat as hex string
            return hexStringToBytes(entityId);
        }
    }

    private byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            // G4: 空 hex 字符串返回全零会导致链上 ID 异常（如仓单 ID 为全零）
            // 修复：抛出异常而非静默返回全零
            throw new IllegalArgumentException("hex 字符串不能为空");
        }

        // 检查是否是有效的十六进制字符串
        boolean isValidHex = hex.startsWith("0x")
            ? hex.substring(2).chars().allMatch(c -> Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))
            : hex.chars().allMatch(c -> Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));

        byte[] data;
        if (isValidHex && (hex.startsWith("0x") ? hex.length() > 2 : hex.length() > 0)) {
            // 作为十六进制字符串解析
            if (hex.startsWith("0x")) {
                hex = hex.substring(2);
            }
            int len = hex.length();
            if (len % 2 != 0) {
                // 奇数长度的 hex 字符串，说明格式错误，抛出异常
                throw new IllegalArgumentException("hex 字符串长度必须为偶数");
            }
            data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                        + Character.digit(hex.charAt(i + 1), 16));
            }
        } else {
            // 非十六进制字符串（如中文货物名称），直接用 UTF-8 编码
            data = hex.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        // FISCO SDK v3 Bytes32 要求恰好 32 字节，左侧补 0 至 32 字节
        if (data.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(data, 0, padded, 32 - data.length, data.length);
            return padded;
        } else if (data.length > 32) {
            // 超过 32 字节，截断前面的数据（保留右侧 32 字节）
            byte[] truncated = new byte[32];
            System.arraycopy(data, data.length - 32, truncated, 0, 32);
            return truncated;
        }
        return data;
    }

    // ==================== Request DTOs ====================

    // Enterprise requests
    @io.swagger.v3.oas.annotations.media.Schema(description = "企业注册请求")
    public static class EnterpriseRegisterRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "企业区块链地址", example = "0x7a9b6d564d5d191093a29b7c760dd6af931cae73")
        private String enterpriseAddress;
        @io.swagger.v3.oas.annotations.media.Schema(description = "统一社会信用代码", example = "91110000MA00XXXX00")
        private String creditCode;
        @io.swagger.v3.oas.annotations.media.Schema(description = "企业角色 0-核心企业 1-供应商 2-经销商", example = "0")
        private Integer role;
        @io.swagger.v3.oas.annotations.media.Schema(description = "企业注册信息哈希", example = "0xabc123...")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "企业状态变更请求")
    public static class EnterpriseStatusRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "企业区块链地址", example = "0x7a9b6d564d5d191093a29b7c760dd6af931cae73")
        private String enterpriseAddress;
        @io.swagger.v3.oas.annotations.media.Schema(description = "新状态 0-待审核 1-正常 2-冻结 3-注销", example = "1")
        private Integer newStatus;

        public String getEnterpriseAddress() { return enterpriseAddress; }
        public void setEnterpriseAddress(String v) { this.enterpriseAddress = v; }
        public Integer getNewStatus() { return newStatus; }
        public void setNewStatus(Integer v) { this.newStatus = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "企业信用评级变更请求")
    public static class EnterpriseCreditRatingRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "企业区块链地址", example = "0x7a9b6d564d5d191093a29b7c760dd6af931cae73")
        private String enterpriseAddress;
        @io.swagger.v3.oas.annotations.media.Schema(description = "新信用评级", example = "AAA")
        private Integer newRating;

        public String getEnterpriseAddress() { return enterpriseAddress; }
        public void setEnterpriseAddress(String v) { this.enterpriseAddress = v; }
        public Integer getNewRating() { return newRating; }
        public void setNewRating(Integer v) { this.newRating = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "企业信用额度变更请求")
    public static class EnterpriseCreditLimitRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "企业区块链地址", example = "0x7a9b6d564d5d191093a29b7c760dd6af931cae73")
        private String enterpriseAddress;
        @io.swagger.v3.oas.annotations.media.Schema(description = "新信用额度", example = "100000000")
        private Long newLimit;

        public String getEnterpriseAddress() { return enterpriseAddress; }
        public void setEnterpriseAddress(String v) { this.enterpriseAddress = v; }
        public Long getNewLimit() { return newLimit; }
        public void setNewLimit(Long v) { this.newLimit = v; }
    }

    // Warehouse receipt requests
    @io.swagger.v3.oas.annotations.media.Schema(description = "仓单开立请求")
    public static class ReceiptIssueRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "货主哈希", example = "0xabc123...")
        private String ownerHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓库哈希", example = "0xdef456...")
        private String warehouseHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "货物详情哈希", example = "0xghi789...")
        private String goodsDetailHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "现场照片哈希", example = "0xjkl012...")
        private String locationPhotoHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "合同哈希", example = "0xmno345...")
        private String contractHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "重量", example = "1000")
        private Long weight;
        @io.swagger.v3.oas.annotations.media.Schema(description = "重量单位", example = "kg")
        private String unit;
        @io.swagger.v3.oas.annotations.media.Schema(description = "数量", example = "100")
        private Long quantity;
        @io.swagger.v3.oas.annotations.media.Schema(description = "入库日期（时间戳）", example = "1711526400000")
        private Long storageDate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "有效期（时间戳）", example = "1714118400000")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "仓单背书请求")
    public static class EndorsementRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "转让人哈希", example = "0xabc123...")
        private String fromHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "受让人哈希", example = "0xdef456...")
        private String toHash;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getFromHash() { return fromHash; }
        public void setFromHash(String v) { this.fromHash = v; }
        public String getToHash() { return toHash; }
        public void setToHash(String v) { this.toHash = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "仓单拆分请求")
    public static class SplitReceiptRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "原仓单编号", example = "WH202603270001")
        private String originalReceiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "新仓单编号列表", example = "[\"WH202603270002\", \"WH202603270003\"]")
        private List<String> newReceiptIds;
        @io.swagger.v3.oas.annotations.media.Schema(description = "各新仓单重量列表", example = "[500, 500]")
        private List<Long> weights;
        @io.swagger.v3.oas.annotations.media.Schema(description = "各新仓单货主哈希列表", example = "[\"0xabc123\", \"0xdef456\"]")
        private List<String> ownerHashes;
        @io.swagger.v3.oas.annotations.media.Schema(description = "各新仓单仓库哈希列表", example = "[\"0xabc123\", \"0xdef456\"]")
        private List<String> warehouseHashes;
        @io.swagger.v3.oas.annotations.media.Schema(description = "重量单位", example = "kg")
        private String unit;

        public String getOriginalReceiptId() { return originalReceiptId; }
        public void setOriginalReceiptId(String v) { this.originalReceiptId = v; }
        public List<String> getNewReceiptIds() { return newReceiptIds; }
        public void setNewReceiptIds(List<String> v) { this.newReceiptIds = v; }
        public List<Long> getWeights() { return weights; }
        public void setWeights(List<Long> v) { this.weights = v; }
        public List<String> getOwnerHashes() { return ownerHashes; }
        public void setOwnerHashes(List<String> v) { this.ownerHashes = v; }
        public List<String> getWarehouseHashes() { return warehouseHashes; }
        public void setWarehouseHashes(List<String> v) { this.warehouseHashes = v; }
        public String getUnit() { return unit; }
        public void setUnit(String v) { this.unit = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "仓单合并请求")
    public static class MergeReceiptRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "源仓单编号列表", example = "[\"WH202603270002\", \"WH202603270003\"]")
        private List<String> sourceReceiptIds;
        @io.swagger.v3.oas.annotations.media.Schema(description = "目标仓单编号", example = "WH202603270001")
        private String targetReceiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "目标货主哈希", example = "0xabc123...")
        private String targetOwnerHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "重量单位", example = "kg")
        private String unit;
        @io.swagger.v3.oas.annotations.media.Schema(description = "总重量", example = "1000")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "仓单操作请求")
    public static class ReceiptOperationRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "仓单注销请求")
    public static class BurnReceiptRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "签名哈希", example = "0xabc123...")
        private String signatureHash;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String v) { this.signatureHash = v; }
    }

    // Logistics requests
    @io.swagger.v3.oas.annotations.media.Schema(description = "物流委派单创建请求")
    public static class LogisticsCreateRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "委派单编号", example = "L202603270001")
        private String voucherNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "业务场景 0-采购 1-销售", example = "0")
        private Integer businessScene;
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "运输数量", example = "1000")
        private Long transportQuantity;
        @io.swagger.v3.oas.annotations.media.Schema(description = "单位", example = "kg")
        private String unit;
        @io.swagger.v3.oas.annotations.media.Schema(description = "货主哈希", example = "0xabc123...")
        private String ownerHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "承运方哈希", example = "0xdef456...")
        private String carrierHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "起仓库哈希", example = "0xghi789...")
        private String sourceWhHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "目标仓库哈希", example = "0xjkl012...")
        private String targetWhHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "有效期截止时间戳", example = "1714118400000")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "物流提货请求")
    public static class LogisticsPickupRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "委派单编号", example = "L202603270001")
        private String voucherNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "提货数量", example = "1000")
        private Long quantity;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public Long getQuantity() { return quantity; }
        public void setQuantity(Long v) { this.quantity = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "物流到达新增请求")
    public static class LogisticsArriveAddRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "委派单编号", example = "L202603270001")
        private String voucherNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "目标仓单编号", example = "WH202603270002")
        private String targetReceiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "数量", example = "1000")
        private Long quantity;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public String getTargetReceiptId() { return targetReceiptId; }
        public void setTargetReceiptId(String v) { this.targetReceiptId = v; }
        public Long getQuantity() { return quantity; }
        public void setQuantity(Long v) { this.quantity = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "物流承运方指派请求")
    public static class LogisticsAssignCarrierRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "委派单编号", example = "L202603270001")
        private String voucherNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "承运方哈希", example = "0xdef456...")
        private String carrierHash;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public String getCarrierHash() { return carrierHash; }
        public void setCarrierHash(String v) { this.carrierHash = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "物流交付确认请求")
    public static class LogisticsConfirmDeliveryRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "委派单编号", example = "L202603270001")
        private String voucherNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "操作 1-确认收货", example = "1")
        private Integer action;
        @io.swagger.v3.oas.annotations.media.Schema(description = "目标仓单编号", example = "WH202603270002")
        private String targetReceiptId;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public Integer getAction() { return action; }
        public void setAction(Integer v) { this.action = v; }
        public String getTargetReceiptId() { return targetReceiptId; }
        public void setTargetReceiptId(String v) { this.targetReceiptId = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "物流状态更新请求")
    public static class LogisticsUpdateStatusRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "委派单编号", example = "L202603270001")
        private String voucherNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "新状态", example = "2")
        private Integer newStatus;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
        public Integer getNewStatus() { return newStatus; }
        public void setNewStatus(Integer v) { this.newStatus = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "物流委派单作废请求")
    public static class LogisticsInvalidateRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "委派单编号", example = "L202603270001")
        private String voucherNo;

        public String getVoucherNo() { return voucherNo; }
        public void setVoucherNo(String v) { this.voucherNo = v; }
    }

    // Loan requests
    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款创建请求")
    public static class LoanCreateRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "借款方哈希", example = "0xabc123...")
        private String borrowerHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "借款方哈希", example = "0xabc123...")
        private String financeEntHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款利率(基点)", example = "500")
        private Long interestRate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款金额", example = "1000000")
        private Long amount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款天数", example = "30")
        private Integer loanDays;
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "质押金额", example = "800000")
        private Long pledgeAmount;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getBorrowerHash() { return borrowerHash; }
        public void setBorrowerHash(String v) { this.borrowerHash = v; }
        public String getFinanceEntHash() { return financeEntHash; }
        public void setFinanceEntHash(String v) { this.financeEntHash = v; }
        public Long getInterestRate() { return interestRate; }
        public void setInterestRate(Long v) { this.interestRate = v; }
        public Long getAmount() { return amount; }
        public void setAmount(Long v) { this.amount = v; }
        public Integer getLoanDays() { return loanDays; }
        public void setLoanDays(Integer v) { this.loanDays = v; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public Long getPledgeAmount() { return pledgeAmount; }
        public void setPledgeAmount(Long v) { this.pledgeAmount = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款审批请求")
    public static class LoanApproveRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "批准金额", example = "1000000")
        private Long approvedAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "利率", example = "0.05")
        private Double interestRate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款天数", example = "30")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款取消请求")
    public static class LoanCancelRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "取消原因", example = "审核未通过")
        private String reason;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款放款请求")
    public static class LoanDisburseRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款还款请求")
    public static class LoanRepayRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "还款本金金额", example = "100000")
        private Long amount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "还款利息金额", example = "1000")
        private Long interestAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "期次索引", example = "0")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款标记逾期请求")
    public static class LoanMarkOverdueRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "逾期天数", example = "7")
        private Integer overdueDays;
        @io.swagger.v3.oas.annotations.media.Schema(description = "罚息利率", example = "0.001")
        private Double penaltyRate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "罚息金额", example = "1000")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款标记坏账请求")
    public static class LoanMarkDefaultedRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "处置方式", example = "质押物变现")
        private String disposalMethod;
        @io.swagger.v3.oas.annotations.media.Schema(description = "处置金额", example = "800000")
        private Long disposalAmount;

        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public String getDisposalMethod() { return disposalMethod; }
        public void setDisposalMethod(String v) { this.disposalMethod = v; }
        public Long getDisposalAmount() { return disposalAmount; }
        public void setDisposalAmount(Long v) { this.disposalAmount = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款设置仓单请求")
    public static class LoanSetReceiptRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "贷款编号", example = "LN202603270001")
        private String loanNo;
        @io.swagger.v3.oas.annotations.media.Schema(description = "质押金额", example = "800000")
        private Long pledgeAmount;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getLoanNo() { return loanNo; }
        public void setLoanNo(String v) { this.loanNo = v; }
        public Long getPledgeAmount() { return pledgeAmount; }
        public void setPledgeAmount(Long v) { this.pledgeAmount = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "贷款更新仓单请求")
    public static class LoanUpdateReceiptRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "新贷款编号", example = "LN202603270002")
        private String newLoanNo;

        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public String getNewLoanNo() { return newLoanNo; }
        public void setNewLoanNo(String v) { this.newLoanNo = v; }
    }

    // Receivable requests
    @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款创建请求")
    public static class ReceivableCreateRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "初始金额", example = "1000000")
        private Long initialAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "到期日期时间戳", example = "1714118400000")
        private Long dueDate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "买卖方对哈希", example = "0xabc123...")
        private String buyerSellerPairHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "发票哈希", example = "0xdef456...")
        private String invoiceHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "合同哈希", example = "0xghi789...")
        private String contractHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "货物详情哈希", example = "0xjkl012...")
        private String goodsDetailHash;
        @io.swagger.v3.oas.annotations.media.Schema(description = "业务场景 0-采购 1-销售", example = "0")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款确认请求")
    public static class ReceivableConfirmRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "签名", example = "0xabc123...")
        private String signature;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public String getSignature() { return signature; }
        public void setSignature(String v) { this.signature = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款调整请求")
    public static class ReceivableAdjustRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "调整后金额", example = "900000")
        private Long adjustedAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "调整原因", example = "adjust")
        private String reason;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public Long getAdjustedAmount() { return adjustedAmount; }
        public void setAdjustedAmount(Long v) { this.adjustedAmount = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款融资请求")
    public static class ReceivableFinanceRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "融资金额", example = "800000")
        private Long financeAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "融资机构", example = "0xfinance123...")
        private String financeEntity;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public Long getFinanceAmount() { return financeAmount; }
        public void setFinanceAmount(Long v) { this.financeAmount = v; }
        public String getFinanceEntity() { return financeEntity; }
        public void setFinanceEntity(String v) { this.financeEntity = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款结清请求")
    public static class ReceivableSettleRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款还款记录请求")
    public static class ReceivableRecordRepaymentRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "还款金额", example = "100000")
        private Long repaymentAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "还款方式", example = "CASH")
        private String paymentMethod;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public Long getRepaymentAmount() { return repaymentAmount; }
        public void setRepaymentAmount(Long v) { this.repaymentAmount = v; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String v) { this.paymentMethod = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款全额还款请求")
    public static class ReceivableFullRepaymentRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "债务抵消请求")
    public static class OffsetDebtRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "应收账款编号", example = "AR202603270001")
        private String receivableId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "抵消金额", example = "500000")
        private Long offsetAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "签名哈希", example = "0xabc123...")
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

    @io.swagger.v3.oas.annotations.media.Schema(description = "仓单抵债请求")
    public static class OffsetDebtWithReceiptRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "源应收款编号", example = "AR202603270001")
        private String receivableId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "仓单编号", example = "WH202603270001")
        private String receiptId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "抵消金额", example = "500000")
        private Long offsetAmount;
        @io.swagger.v3.oas.annotations.media.Schema(description = "抵债原因", example = "warehouse_offset")
        private String reason;

        public String getReceivableId() { return receivableId; }
        public void setReceivableId(String v) { this.receivableId = v; }
        public String getReceiptId() { return receiptId; }
        public void setReceiptId(String v) { this.receiptId = v; }
        public Long getOffsetAmount() { return offsetAmount; }
        public void setOffsetAmount(Long v) { this.offsetAmount = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }
    }

    // ==================== Signature Request DTOs ====================

    @io.swagger.v3.oas.annotations.media.Schema(description = "签名请求")
    public static class SignRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "要签名的数据（字符串）", example = "receiptId:WH202603270001:transferor:123456:transferee:789012")
        private String data;

        public SignRequest() {}

        public SignRequest(String data) {
            this.data = data;
        }

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "签名验证请求")
    public static class SignVerifyRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "原始数据", example = "receiptId:WH202603270001:transferor:123456:transferee:789012")
        private String data;
        @io.swagger.v3.oas.annotations.media.Schema(description = "签名（十六进制字符串，带 0x 前缀）", example = "0xaabbccdd...")
        private String signature;

        public SignVerifyRequest() {}

        public SignVerifyRequest(String data, String signature) {
            this.data = data;
            this.signature = signature;
        }

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
    }
}
