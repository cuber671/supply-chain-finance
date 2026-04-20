package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 到货入库请求DTO
 *
 * 支持直接移库场景的全量交付和部分交付：
 * - action=1 全量交付：调用区块链arriveAndCreateReceipt创建新仓单
 * - action=2 部分交付：记录到货信息到arrive_records，后续confirmDelivery调用splitReceipt
 *
 * 注意：除voucherNo外均可省略，后端自动从委派单获取默认值：
 * - warehouseId 默认为 targetWhId（目标仓库）
 * - arrivedWeight 默认为 transportQuantity（运输数量）
 * - actionType 根据 arrivedWeight 与 transportQuantity 的比较自动判断
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "到货入库请求")
public class ArriveRequest {

    @Parameter(description = "委派单编号", required = true, example = "DPDO20260418697B0A32")
    @NotBlank(message = "委派单编号不能为空")
    private String voucherNo;

    @Parameter(description = "到货重量，不传则默认使用委派单的运输数量。全量交付时与运输数量相同，部分交付时小于运输数量", example = "100")
    private java.math.BigDecimal arrivedWeight;

    @Parameter(description = "目标仓库ID（Warehouse.id），不传则默认使用委派单的目标仓库", example = "5")
    private Long warehouseId;

    @Parameter(description = "动作类型：1=全量交付（调用区块链创建新仓单），2=部分交付（记录到货信息）。不传则自动判断：到货重量=运输数量为1，否则为2", example = "1")
    private Integer actionType;

    @Parameter(description = "目标仓单ID（部分交付时可指定，合并到目标仓单）", example = "2042829397927989200")
    private Long targetReceiptId;
}
