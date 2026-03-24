package com.fisco.app.service;

import java.math.BigDecimal;
import java.util.List;

import com.fisco.app.util.PageResult;
import com.fisco.app.dto.LoanApproveRequest;
import com.fisco.app.dto.LoanApplyRequest;
import com.fisco.app.dto.LoanCalculatorRequest;
import com.fisco.app.dto.RepaymentRequest;
import com.fisco.app.entity.Loan;
import com.fisco.app.entity.LoanInstallment;
import com.fisco.app.entity.LoanRepayment;

/**
 * 质押贷款服务接口
 */
public interface LoanService {

    Loan applyLoan(LoanApplyRequest request);

    PageResult<Loan> listLoans(Integer pageNum, Integer pageSize, Integer status,
                               Long borrowerEntId, Long financeEntId,
                               String startDate, String endDate);

    Loan getLoanById(Long id);

    boolean approveLoan(Long id, LoanApproveRequest request);

    boolean rejectLoan(Long id, String reason);

    boolean cancelLoan(Long id, String reason);

    boolean disburseLoan(Long id, String disbursementVoucher);

    LoanRepayment repayLoan(Long id, RepaymentRequest request);

    List<LoanRepayment> listRepayments(Long loanId);

    List<LoanInstallment> listInstallments(Long loanId);

    LoanCalculatorRequest.LoanCalculatorResult calculator(LoanCalculatorRequest request);

    List<Loan> myLoans(Long borrowerEntId, Integer status);

    PageResult<Loan> myLoans(Long borrowerEntId, Integer status, Integer pageNum, Integer pageSize);

    List<Loan> pendingLoans(Long financeEntId);

    PageResult<Loan> pendingLoans(Long financeEntId, Integer pageNum, Integer pageSize);

    int checkAndProcessOverdueLoans();

    List<LoanInstallment> generateInstallmentPlan(Long loanId, Integer installmentCount);

    BigDecimal calculateTotalRepayable(Loan loan);

    BigDecimal calculateInterest(Loan loan, java.time.LocalDate calculateDate);

    BigDecimal calculatePenalty(Loan loan, BigDecimal penaltyRate);
}
