package com.fisco.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.RepaymentRecord;

/**
 * 还款记录 Mapper接口
 */
@Mapper
public interface RepaymentRecordMapper extends BaseMapper<RepaymentRecord> {

    @Select("SELECT * FROM t_repayment_record WHERE repayment_no = #{repaymentNo}")
    RepaymentRecord selectByRepaymentNo(@Param("repaymentNo") String repaymentNo);

    @Select("SELECT * FROM t_repayment_record WHERE receivable_id = #{receivableId} ORDER BY repayment_time DESC")
    List<RepaymentRecord> selectByReceivableId(@Param("receivableId") Long receivableId);

    @Select("SELECT * FROM t_repayment_record WHERE repayment_type = #{repaymentType} ORDER BY repayment_time DESC")
    List<RepaymentRecord> selectByRepaymentType(@Param("repaymentType") Integer repaymentType);

    @Select("SELECT * FROM t_repayment_record WHERE receipt_id = #{receiptId} ORDER BY repayment_time DESC")
    List<RepaymentRecord> selectByReceiptId(@Param("receiptId") Long receiptId);
}
