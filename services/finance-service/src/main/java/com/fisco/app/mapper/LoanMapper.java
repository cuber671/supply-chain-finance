package com.fisco.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.Loan;

/**
 * 质押贷款 Mapper接口
 */
@Mapper
public interface LoanMapper extends BaseMapper<Loan> {

    @Select("SELECT * FROM t_loan WHERE loan_no = #{loanNo}")
    Loan selectByLoanNo(@Param("loanNo") String loanNo);

    @Select("SELECT * FROM t_loan WHERE borrower_ent_id = #{borrowerEntId} ORDER BY create_time DESC")
    List<Loan> selectByBorrowerEntId(@Param("borrowerEntId") Long borrowerEntId);

    @Select("SELECT * FROM t_loan WHERE finance_ent_id = #{financeEntId} ORDER BY create_time DESC")
    List<Loan> selectByFinanceEntId(@Param("financeEntId") Long financeEntId);

    @Select("SELECT * FROM t_loan WHERE receipt_id = #{receiptId} ORDER BY create_time DESC")
    List<Loan> selectByReceiptId(@Param("receiptId") Long receiptId);

    @Select("SELECT * FROM t_loan WHERE status = #{status} ORDER BY create_time DESC")
    List<Loan> selectByStatus(@Param("status") Integer status);

    @Select("SELECT * FROM t_loan WHERE finance_ent_id = #{financeEntId} AND status = 1 ORDER BY create_time ASC")
    List<Loan> selectPendingByFinanceEntId(@Param("financeEntId") Long financeEntId);

    @Update("UPDATE t_loan SET status = #{status}, update_time = NOW() WHERE loan_no = #{loanNo}")
    int updateStatusByLoanNo(@Param("loanNo") String loanNo, @Param("status") Integer status);

    @Update("UPDATE t_loan SET " +
            "repaid_principal = #{repaidPrincipal}, " +
            "repaid_interest = #{repaidInterest}, " +
            "repaid_penalty = #{repaidPenalty}, " +
            "outstanding_principal = #{outstandingPrincipal}, " +
            "outstanding_interest = #{outstandingInterest}, " +
            "status = #{status}, " +
            "update_time = NOW() " +
            "WHERE loan_no = #{loanNo}")
    int updateRepaymentInfo(@Param("loanNo") String loanNo,
                            @Param("repaidPrincipal") java.math.BigDecimal repaidPrincipal,
                            @Param("repaidInterest") java.math.BigDecimal repaidInterest,
                            @Param("repaidPenalty") java.math.BigDecimal repaidPenalty,
                            @Param("outstandingPrincipal") java.math.BigDecimal outstandingPrincipal,
                            @Param("outstandingInterest") java.math.BigDecimal outstandingInterest,
                            @Param("status") Integer status);

    @Select("SELECT * FROM t_loan WHERE status = 5 AND loan_end_date < CURDATE() ORDER BY loan_end_date ASC")
    List<Loan> selectOverdueLoans();

    @Select("SELECT * FROM t_loan WHERE borrower_ent_id = #{borrowerEntId} AND status IN (4, 5, 6, 8) ORDER BY create_time DESC")
    List<Loan> selectActiveLoansByBorrower(@Param("borrowerEntId") Long borrowerEntId);
}
