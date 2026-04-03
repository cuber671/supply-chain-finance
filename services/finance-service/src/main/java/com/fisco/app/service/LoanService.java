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
 *
 * 提供仓单质押贷款的完整生命周期管理，包括贷款申请、审批、放款、
 * 还款、逾期处理等核心功能。
 */
public interface LoanService {

    /**
     * 申请质押贷款
     *
     * @param request 贷款申请请求，包含借款人信息、贷款金额、期限等
     * @return 创建的贷款记录
     * @throws IllegalArgumentException 参数不合法或仓单状态不支持质押
     */
    Loan applyLoan(LoanApplyRequest request);

    /**
     * 分页查询贷款列表（管理员/金融机构视角）
     *
     * @param pageNum 页码，从1开始
     * @param pageSize 每页记录数
     * @param status 贷款状态筛选，可为空
     * @param borrowerEntId 借款企业ID筛选，可为空
     * @param financeEntId 金融机构ID筛选，可为空
     * @param startDate 起始日期筛选，格式yyyy-MM-dd，可为空
     * @param endDate 结束日期筛选，格式yyyy-MM-dd，可为空
     * @return 分页结果
     */
    PageResult<Loan> listLoans(Integer pageNum, Integer pageSize, Integer status,
                               Long borrowerEntId, Long financeEntId,
                               String startDate, String endDate);

    /**
     * 根据ID查询贷款详情
     *
     * @param id 贷款记录ID
     * @return 贷款记录，不存在返回null
     */
    Loan getLoanById(Long id);

    /**
     * 审批通过贷款
     *
     * @param id 贷款记录ID
     * @param request 审批通过请求，包含批款金额、年利率、贷款期限等
     * @return 审批是否成功
     * @throws IllegalArgumentException 贷款不存在或状态不允许审批
     */
    boolean approveLoan(Long id, LoanApproveRequest request);

    /**
     * 拒绝贷款申请
     *
     * @param id 贷款记录ID
     * @param reason 拒绝原因
     * @return 拒绝操作是否成功
     * @throws IllegalArgumentException 贷款不存在或状态不允许拒绝
     */
    boolean rejectLoan(Long id, String reason);

    /**
     * 取消贷款（借款人主动取消或放款前取消）
     *
     * @param id 贷款记录ID
     * @param reason 取消原因
     * @return 取消操作是否成功
     * @throws IllegalArgumentException 贷款不存在或状态不允许取消
     */
    boolean cancelLoan(Long id, String reason);

    /**
     * 放款（将贷款金额发放给借款人）
     *
     * @param id 贷款记录ID
     * @param disbursementVoucher 放款凭证（图片URL或凭证号）
     * @return 放款是否成功
     * @throws IllegalArgumentException 贷款不存在、状态不允许放款或仓单锁定失败
     */
    boolean disburseLoan(Long id, String disbursementVoucher);

    /**
     * 还款（按期还款或提前还款）
     *
     * @param id 贷款记录ID
     * @param request 还款请求，包含还款金额、还款方式等
     * @return 还款记录
     * @throws IllegalArgumentException 贷款不存在或还款金额超过待还金额
     */
    LoanRepayment repayLoan(Long id, RepaymentRequest request);

    /**
     * 查询贷款的还款记录列表
     *
     * @param loanId 贷款记录ID
     * @return 还款记录列表，按还款时间倒序
     */
    List<LoanRepayment> listRepayments(Long loanId);

    /**
     * 查询贷款的分期还款计划
     *
     * @param loanId 贷款记录ID
     * @return 分期计划列表
     */
    List<LoanInstallment> listInstallments(Long loanId);

    /**
     * 贷款计算器 - 计算给定条件下的还款计划
     *
     * @param request 计算请求，包含贷款金额、利率、期限、分期数等
     * @return 计算结果，包含每期还款额、总利息、总还款额等
     */
    LoanCalculatorRequest.LoanCalculatorResult calculator(LoanCalculatorRequest request);

    /**
     * 查询借款企业的所有贷款（借款人视角）
     *
     * @param borrowerEntId 借款企业ID
     * @param status 贷款状态筛选，可为空表示全部
     * @return 贷款列表
     */
    List<Loan> myLoans(Long borrowerEntId, Integer status);

    /**
     * 分页查询借款企业的所有贷款（借款人视角）
     *
     * @param borrowerEntId 借款企业ID
     * @param status 贷款状态筛选，可为空表示全部
     * @param pageNum 页码，从1开始
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    PageResult<Loan> myLoans(Long borrowerEntId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询金融机构待审批的贷款列表
     *
     * @param financeEntId 金融机构企业ID
     * @return 待审批的贷款列表
     */
    List<Loan> pendingLoans(Long financeEntId);

    /**
     * 分页查询金融机构待审批的贷款列表
     *
     * @param financeEntId 金融机构企业ID
     * @param pageNum 页码，从1开始
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    PageResult<Loan> pendingLoans(Long financeEntId, Integer pageNum, Integer pageSize);

    /**
     * 检查并处理逾期贷款
     *
     * 遍历所有处于"还款中"状态的贷款，将已过到期日的贷款标记为逾期，
     * 并生成相应的逾期罚息。可由定时任务触发。
     *
     * @return 处理逾期的贷款数量
     */
    int checkAndProcessOverdueLoans();

    /**
     * 生成分期还款计划
     *
     * @param loanId 贷款记录ID
     * @param installmentCount 分期数
     * @return 分期计划列表
     * @throws IllegalArgumentException 贷款不存在或分期数不合法
     */
    List<LoanInstallment> generateInstallmentPlan(Long loanId, Integer installmentCount);

    /**
     * 计算贷款到期总还款额
     *
     * @param loan 贷款记录
     * @return 到期应还总金额（含本金+利息+罚息）
     */
    BigDecimal calculateTotalRepayable(Loan loan);

    /**
     * 计算指定日期的贷款利息
     *
     * @param loan 贷款记录
     * @param calculateDate 计算日期
     * @return 截至该日期的累计利息
     */
    BigDecimal calculateInterest(Loan loan, java.time.LocalDate calculateDate);

    /**
     * 计算贷款罚息
     *
     * @param loan 贷款记录
     * @param penaltyRate 日罚息率（通常为正常日利率的1.5倍）
     * @return 累计罚息金额
     */
    BigDecimal calculatePenalty(Loan loan, BigDecimal penaltyRate);
}