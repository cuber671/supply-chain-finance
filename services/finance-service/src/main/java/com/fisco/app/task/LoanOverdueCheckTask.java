package com.fisco.app.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fisco.app.service.LoanService;

/**
 * 贷款逾期检测定时任务
 *
 * 每天凌晨1点检查已放款但未结清的贷款，将超过到期日的贷款标记为逾期
 */
@Component
public class LoanOverdueCheckTask {

    private static final Logger logger = LoggerFactory.getLogger(LoanOverdueCheckTask.class);

    @Autowired
    private LoanService loanService;

    /**
     * 每天凌晨1点执行逾期检测
     * 使用cron表达式: 秒 分 时 日 月 周
     * 0 0 1 * * ? = 每天1:00:00
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkOverdueLoans() {
        logger.info("开始执行贷款逾期检测任务");
        try {
            int count = loanService.checkAndProcessOverdueLoans();
            logger.info("贷款逾期检测任务完成: 共标记{}笔逾期贷款", count);
        } catch (Exception e) {
            logger.error("贷款逾期检测任务执行失败", e);
        }
    }
}