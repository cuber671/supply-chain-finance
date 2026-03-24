package com.fisco.app.controller;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.CallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.annotation.RequireRole;
import com.fisco.app.util.Result;
import com.fisco.app.service.BlockchainService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 区块链基础操作 Controller
 *
 * 提供区块链网络的基础操作 API，包括：
 * - 区块链状态查询
 * - 区块/交易信息查询
 * - 账户余额查询
 * - 合约调用
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "区块链基础服务", description = "区块链网络基础操作：状态查询、区块/交易查询、合约调用")
@RestController
@RequestMapping("/api/v1/blockchain")
public class BlockchainController {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainController.class);

    @Autowired
    private BlockchainService blockchainService;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

    @Operation(summary = "获取区块链状态", description = "查询区块链网络连接状态、当前块高、链ID等信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();

        result.put("enabled", fiscoEnabled);
        result.put("connected", blockchainService.isConnected());

        if (blockchainService.isConnected()) {
            try {
                result.put("blockNumber", blockchainService.getBlockNumber());
                result.put("chainId", blockchainService.getChainId());
                result.put("group", blockchainService.getGroupList());
                result.put("accountAddress", blockchainService.getCurrentAccountAddress());
            } catch (Exception e) {
                logger.error("获取区块链状态失败", e);
                result.put("error", e.getMessage());
            }
        }

        return Result.success(result);
    }

    @Operation(summary = "健康检查", description = "检查区块链网关服务健康状态。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", blockchainService.isConnected() ? "UP" : "DOWN");
        return Result.success(result);
    }

    @Operation(summary = "获取当前块高", description = "查询区块链当前区块高度。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/blockNumber")
    public Result<BigInteger> getBlockNumber() {
        try {
            BigInteger blockNumber = blockchainService.getBlockNumber();
            return Result.success(blockNumber);
        } catch (Exception e) {
            logger.error("获取块高失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "根据块号获取区块信息", description = "根据区块号查询对应区块的详细信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数无效"),
        @ApiResponse(responseCode = "404", description = "区块不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/block/{blockNumber}")
    public Result<String> getBlock(
            @Parameter(description = "块号", required = true)
            @PathVariable BigInteger blockNumber) {
        try {
            String blockInfo = blockchainService.getBlockByNumber(blockNumber);
            if (blockInfo == null) {
                return Result.error(404, "区块不存在: " + blockNumber);
            }
            return Result.success(blockInfo);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("获取区块信息失败, blockNumber={}", blockNumber, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "根据块号获取区块哈希", description = "根据区块号查询对应区块的哈希值。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/blockHash/{blockNumber}")
    public Result<String> getBlockHash(
            @Parameter(description = "块号", required = true)
            @PathVariable BigInteger blockNumber) {
        try {
            String blockHash = blockchainService.getBlockHashByNumber(blockNumber);
            return Result.success(blockHash);
        } catch (Exception e) {
            logger.error("获取区块哈希失败, blockNumber={}", blockNumber, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "根据交易哈希获取交易收据", description = "根据交易哈希查询对应交易的收据信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数无效"),
        @ApiResponse(responseCode = "404", description = "交易收据不存在或交易未确认"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/{txHash}")
    public Result<TransactionReceipt> getTransactionReceipt(
            @Parameter(description = "交易哈希", required = true)
            @PathVariable String txHash) {
        try {
            TransactionReceipt receipt = blockchainService.getTransactionReceipt(txHash);
            if (receipt == null) {
                return Result.error(404, "交易收据不存在或交易未确认: " + txHash);
            }
            return Result.success(receipt);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("获取交易收据失败, txHash={}", txHash, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "获取当前系统账户地址", description = "获取FISCO区块链SDK当前使用的系统账户地址。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/account")
    public Result<String> getCurrentAccount() {
        try {
            String address = blockchainService.getCurrentAccountAddress();
            return Result.success(address);
        } catch (Exception e) {
            logger.error("获取当前账户地址失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "查询账户余额", description = "查询指定账户地址的FISCO代币余额。注意：FISCO SDK 3.x不支持余额查询，会返回提示信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/balance/{address}")
    public Result<String> getBalance(
            @Parameter(description = "账户地址", required = true)
            @PathVariable String address) {
        try {
            if (!address.startsWith("0x")) {
                address = "0x" + address;
            }
            String balance = blockchainService.getBalance(address);
            if (balance == null) {
                return Result.success("SDK 3.x 不支持余额查询，请使用合约调用");
            }
            return Result.success(balance);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("查询余额失败, address={}", address, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "调用合约只读方法", description = "调用区块链上合约的只读方法（call），不产生交易。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "调用成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：缺少必要参数"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/call")
    public Result<CallResponse> callContract(
            @Parameter(description = "合约调用信息", required = true) @RequestBody ContractCallRequest request) {
        try {
            if (request.getContractAddress() == null || request.getAbi() == null
                    || request.getMethod() == null) {
                return Result.error(400, "缺少必要参数: contractAddress, abi, method");
            }

            List<Object> params = request.getParams();
            if (params == null) {
                params = List.of();
            }

            CallResponse response = blockchainService.callContract(
                    request.getContractAddress(),
                    request.getAbi(),
                    request.getMethod(),
                    params);

            return Result.success(response);
        } catch (Exception e) {
            logger.error("合约调用失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "发送合约交易", description = "发送区块链合约交易（transaction），产生链上记录。需要ADMIN角色。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "交易发送成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：缺少必要参数"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/transaction")
    public Result<Object> sendTransaction(
            @Parameter(description = "合约交易信息", required = true) @RequestBody ContractCallRequest request) {
        try {
            if (request.getContractAddress() == null || request.getAbi() == null
                    || request.getMethod() == null) {
                return Result.error(400, "缺少必要参数: contractAddress, abi, method");
            }

            List<Object> params = request.getParams();
            if (params == null) {
                params = List.of();
            }

            Object response = blockchainService.sendContractTransaction(
                    request.getContractAddress(),
                    request.getAbi(),
                    request.getMethod(),
                    params);

            return Result.success(response);
        } catch (Exception e) {
            logger.error("发送合约交易失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "获取群组信息", description = "获取FISCO区块链群组详细信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/group")
    public Result<String> getGroupInfo() {
        try {
            String groupInfo = blockchainService.getGroupInfo();
            return Result.success(groupInfo);
        } catch (Exception e) {
            logger.error("获取群组信息失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "获取群组列表", description = "获取FISCO区块链所有可用群组列表。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/groups")
    public Result<List<String>> getGroupList() {
        try {
            List<String> groups = blockchainService.getGroupList();
            return Result.success(groups);
        } catch (Exception e) {
            logger.error("获取群组列表失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "合约调用请求")
    public static class ContractCallRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "合约地址", example = "0x7a9b6d564d5d191093a29b7c760dd6af931cae73")
        private String contractAddress;
        @io.swagger.v3.oas.annotations.media.Schema(description = "合约ABI（JSON格式）", example = "[{\"inputs\":[],\"name\":\"getValue\",\"type\":\"function\"}]")
        private String abi;
        @io.swagger.v3.oas.annotations.media.Schema(description = "调用的方法名", example = "getValue")
        private String method;
        @io.swagger.v3.oas.annotations.media.Schema(description = "方法参数列表")
        private List<Object> params;

        public String getContractAddress() {
            return contractAddress;
        }

        public void setContractAddress(String contractAddress) {
            this.contractAddress = contractAddress;
        }

        public String getAbi() {
            return abi;
        }

        public void setAbi(String abi) {
            this.abi = abi;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public List<Object> getParams() {
            return params;
        }

        public void setParams(List<Object> params) {
            this.params = params;
        }
    }
}
