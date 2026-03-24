package com.fisco.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.LoanRepayment;

/**
 * 贷款还款记录 Mapper接口
 */
@Mapper
public interface LoanRepaymentMapper extends BaseMapper<LoanRepayment> {

    @Select("SELECT * FROM t_loan_repayment WHERE repayment_no = #{repaymentNo}")
    LoanRepayment selectByRepaymentNo(@Param("repaymentNo") String repaymentNo);

    @Select("SELECT * FROM t_loan_repayment WHERE loan_id = #{loanId} ORDER BY create_time DESC")
    List<LoanRepayment> selectByLoanId(@Param("loanId") Long loanId);

    @Select("SELECT * FROM t_loan_repayment WHERE repayment_type = #{repaymentType} ORDER BY create_time DESC")
    List<LoanRepayment> selectByRepaymentType(@Param("repaymentType") String repaymentType);

    @Select("SELECT * FROM t_loan_repayment WHERE loan_id = #{loanId} AND status = 2 ORDER BY repayment_time ASC")
    List<LoanRepayment> selectConfirmedByLoanId(@Param("loanId") Long loanId);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_loan_repayment WHERE loan_id = #{loanId} AND status = 2")
    java.math.BigDecimal sumTotalAmountByLoanId(@Param("loanId") Long loanId);

    @Select("SELECT * FROM t_loan_repayment WHERE offset_receipt_id = #{offsetReceiptId} ORDER BY create_time DESC")
    List<LoanRepayment> selectByOffsetReceiptId(@Param("offsetReceiptId") Long offsetReceiptId);

    @Select("SELECT * FROM t_loan_repayment WHERE offset_receivable_id = #{offsetReceivableId} ORDER BY create_time DESC")
    List<LoanRepayment> selectByOffsetReceivableId(@Param("offsetReceivableId") Long offsetReceivableId);
}
