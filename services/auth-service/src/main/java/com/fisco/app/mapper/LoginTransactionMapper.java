package com.fisco.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.LoginTransaction;

/**
 * 登录事务Mapper - TCC模式
 */
@Mapper
public interface LoginTransactionMapper extends BaseMapper<LoginTransaction> {

    /**
     * 根据UUID查询事务
     */
    @Select("SELECT * FROM t_login_transaction WHERE tx_uuid = #{txUuid}")
    LoginTransaction selectByUuid(@Param("txUuid") String txUuid);

    /**
     * 更新事务状态为已确认
     */
    @Update("UPDATE t_login_transaction SET status = 'CONFIRMED', confirm_time = NOW(), " +
            "enterprise_ent_id = #{enterpriseEntId}, enterprise_session_id = #{enterpriseSessionId} " +
            "WHERE tx_uuid = #{txUuid}")
    int updateStatusToConfirmed(@Param("txUuid") String txUuid,
                                @Param("enterpriseEntId") Long enterpriseEntId,
                                @Param("enterpriseSessionId") String enterpriseSessionId);

    /**
     * 更新事务状态为已取消
     */
    @Update("UPDATE t_login_transaction SET status = 'CANCELLED', cancel_time = NOW(), " +
            "error_msg = #{errorMsg} WHERE tx_uuid = #{txUuid}")
    int updateStatusToCancelled(@Param("txUuid") String txUuid, @Param("errorMsg") String errorMsg);

    /**
     * 更新事务状态为失败
     */
    @Update("UPDATE t_login_transaction SET status = 'FAILED', error_msg = #{errorMsg} " +
            "WHERE tx_uuid = #{txUuid}")
    int updateStatusToFailed(@Param("txUuid") String txUuid, @Param("errorMsg") String errorMsg);
}
