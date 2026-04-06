package com.fisco.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.LogisticsDeviationCreditRecord;

/**
 * 物流偏航信用扣分记录 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface LogisticsDeviationCreditRecordMapper extends BaseMapper<LogisticsDeviationCreditRecord> {

    @Select("SELECT * FROM t_logistics_deviation_credit_record WHERE status = 0 AND retry_count < 3 ORDER BY create_time ASC")
    List<LogisticsDeviationCreditRecord> selectPendingRecords();

    @Select("SELECT * FROM t_logistics_deviation_credit_record WHERE ent_id = #{entId} AND logistics_order_id = #{logisticsOrderId} AND status = 0 LIMIT 1")
    LogisticsDeviationCreditRecord selectPendingByEntIdAndOrderId(@Param("entId") Long entId, @Param("logisticsOrderId") String logisticsOrderId);
}