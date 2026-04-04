package com.fisco.app.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.LogisticsAssignHistory;

/**
 * 物流指派历史记录 Mapper接口
 *
 * 【P2-4修复】记录委派单的指派变更历史
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface LogisticsAssignHistoryMapper extends BaseMapper<LogisticsAssignHistory> {

    /**
     * 查询委派单的指派历史记录数
     */
    @Select("SELECT COUNT(*) FROM t_logistics_assign_history WHERE voucher_no = #{voucherNo}")
    long countByVoucherNo(@Param("voucherNo") String voucherNo);

    /**
     * 插入指派历史记录
     */
    @Insert("INSERT INTO t_logistics_assign_history (voucher_no, driver_id, driver_name, vehicle_no, assign_time, assign_type) " +
            "VALUES (#{voucherNo}, #{driverId}, #{driverName}, #{vehicleNo}, #{assignTime}, #{assignType})")
    int insertAssignHistory(@Param("voucherNo") String voucherNo,
                            @Param("driverId") String driverId,
                            @Param("driverName") String driverName,
                            @Param("vehicleNo") String vehicleNo,
                            @Param("assignTime") java.time.LocalDateTime assignTime,
                            @Param("assignType") Integer assignType);
}