package com.fisco.app.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 物流轨迹记录实体类
 *
 * 对应数据库表: t_logistics_track
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_logistics_track")
public class LogisticsTrack {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("voucher_no")
    private String voucherNo;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("location_name")
    private String locationName;

    @TableField("location_desc")
    private String locationDesc;

    @TableField("status")
    private Integer status;

    @TableField("deviation_distance")
    private BigDecimal deviationDistance;

    @TableField("is_deviation")
    private Integer isDeviation;

    @TableField("event_time")
    private LocalDateTime eventTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ==================== 状态常量 ====================

    public static final int STATUS_PICKED_UP = 1;
    public static final int STATUS_IN_TRANSIT = 2;
    public static final int STATUS_ARRIVED = 3;

    // ==================== 偏航常量 ====================

    public static final int DEVIATION_NO = 0;
    public static final int DEVIATION_YES = 1;

    // ==================== 便捷方法 ====================

    public boolean isDeviation() {
        return this.isDeviation != null && DEVIATION_YES == this.isDeviation;
    }

    public String getStatusDesc() {
        if (status == null) return "未知";
        switch (this.status) {
            case STATUS_PICKED_UP: return "已提货";
            case STATUS_IN_TRANSIT: return "运输中";
            case STATUS_ARRIVED: return "已到达";
            default: return "未知";
        }
    }
}
