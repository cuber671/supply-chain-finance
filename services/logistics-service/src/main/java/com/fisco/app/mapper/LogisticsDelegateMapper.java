package com.fisco.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.LogisticsDelegate;

/**
 * 电子物流委派单 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface LogisticsDelegateMapper extends BaseMapper<LogisticsDelegate> {

    @Select("SELECT * FROM t_logistics_delegate WHERE voucher_no = #{voucherNo}")
    LogisticsDelegate selectByVoucherNo(@Param("voucherNo") String voucherNo);

    @Select("SELECT * FROM t_logistics_delegate WHERE owner_ent_id = #{ownerEntId} ORDER BY create_time DESC")
    List<LogisticsDelegate> selectByOwnerEntId(@Param("ownerEntId") Long ownerEntId);

    @Select("SELECT * FROM t_logistics_delegate WHERE carrier_ent_id = #{carrierEntId} ORDER BY create_time DESC")
    List<LogisticsDelegate> selectByCarrierEntId(@Param("carrierEntId") Long carrierEntId);

    // 优化：单次查询支持 owner 或 carrier + 可选业务场景和状态过滤
    @Select("<script>" +
            "SELECT * FROM t_logistics_delegate " +
            "WHERE (owner_ent_id = #{entId} OR carrier_ent_id = #{entId}) " +
            "<if test='businessScene != null'> AND business_scene = #{businessScene} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<LogisticsDelegate> selectByEntIdWithFilters(
        @Param("entId") Long entId,
        @Param("businessScene") Integer businessScene,
        @Param("status") Integer status
    );
}
