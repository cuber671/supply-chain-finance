package com.fisco.app.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "注销审核响应")
public class CancellationAuditResponse {

    @Schema(description = "企业ID", example = "2048966066005692417")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long entId;

    @Schema(description = "审核结果", example = "通过")
    private String action;

    @Schema(description = "新状态", example = "4")
    private Integer newStatus;

    @Schema(description = "链上状态更新交易哈希")
    private String txHash;
}