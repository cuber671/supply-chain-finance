package com.fisco.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.LoanInstallment;

/**
 * 贷款分期计划 Mapper接口
 */
@Mapper
public interface LoanInstallmentMapper extends BaseMapper<LoanInstallment> {

    @Select("SELECT * FROM t_loan_installment WHERE loan_id = #{loanId} ORDER BY installment_no ASC")
    List<LoanInstallment> selectByLoanId(@Param("loanId") Long loanId);

    @Select("SELECT * FROM t_loan_installment WHERE loan_id = #{loanId} AND installment_no = #{installmentNo}")
    LoanInstallment selectByLoanIdAndNo(@Param("loanId") Long loanId, @Param("installmentNo") Integer installmentNo);

    @Select("SELECT * FROM t_loan_installment WHERE status = 1 AND due_date < CURDATE() ORDER BY due_date ASC")
    List<LoanInstallment> selectOverdueInstallments();

    @Update("UPDATE t_loan_installment SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE t_loan_installment SET repaid_amount = #{repaidAmount}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateRepaidAmount(@Param("id") Long id, @Param("repaidAmount") java.math.BigDecimal repaidAmount, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM t_loan_installment WHERE loan_id = #{loanId} AND status = 1")
    int countPendingByLoanId(@Param("loanId") Long loanId);
}
