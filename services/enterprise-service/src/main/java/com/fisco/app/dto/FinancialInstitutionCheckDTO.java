package com.fisco.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialInstitutionCheckDTO {
    private Long entId;
    private Boolean isFinancialInstitution;
    private String entRoleName;
}
