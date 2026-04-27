package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "金融机构资格校验")
public class FinancialInstitutionCheckDTO {

    @Schema(description = "企业ID", example = "1001")
    private Long entId;

    @Schema(description = "是否为金融机构", example = "true")
    private Boolean isFinancialInstitution;

    @Schema(description = "企业角色名称", example = "FINANCIAL_INSTITUTION")
    private String entRoleName;
}