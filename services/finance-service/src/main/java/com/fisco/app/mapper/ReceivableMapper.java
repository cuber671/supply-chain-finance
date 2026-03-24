package com.fisco.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.Receivable;

/**
 * 电子应收款项 Mapper接口
 */
@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    @Select("SELECT * FROM t_receivable WHERE receivable_no = #{receivableNo}")
    Receivable selectByReceivableNo(@Param("receivableNo") String receivableNo);

    @Select("SELECT * FROM t_receivable WHERE source_voucher_id = #{sourceVoucherId} ORDER BY create_time DESC")
    List<Receivable> selectBySourceVoucherId(@Param("sourceVoucherId") Long sourceVoucherId);

    @Select("SELECT * FROM t_receivable WHERE creditor_ent_id = #{creditorEntId} ORDER BY create_time DESC")
    List<Receivable> selectByCreditorEntId(@Param("creditorEntId") Long creditorEntId);

    @Select("SELECT * FROM t_receivable WHERE debtor_ent_id = #{debtorEntId} ORDER BY create_time DESC")
    List<Receivable> selectByDebtorEntId(@Param("debtorEntId") Long debtorEntId);

    @Update("UPDATE t_receivable SET status = #{status}, update_time = NOW() WHERE receivable_no = #{receivableNo}")
    int updateStatusByReceivableNo(@Param("receivableNo") String receivableNo, @Param("status") Integer status);

    @Update("UPDATE t_receivable SET adjusted_amount = #{adjustedAmount}, balance_unpaid = #{balanceUnpaid}, update_time = NOW() WHERE receivable_no = #{receivableNo}")
    int updateAmountByReceivableNo(@Param("receivableNo") String receivableNo,
                                    @Param("adjustedAmount") java.math.BigDecimal adjustedAmount,
                                    @Param("balanceUnpaid") java.math.BigDecimal balanceUnpaid);

    @Update("UPDATE t_receivable SET collected_amount = #{collectedAmount}, balance_unpaid = #{balanceUnpaid}, update_time = NOW() WHERE receivable_no = #{receivableNo}")
    int updateCollectedAmount(@Param("receivableNo") String receivableNo,
                              @Param("collectedAmount") java.math.BigDecimal collectedAmount,
                              @Param("balanceUnpaid") java.math.BigDecimal balanceUnpaid);
}
