package com.fisco.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.InvitationCode;

/**
 * 邀请码 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface InvitationCodeMapper extends BaseMapper<InvitationCode> {

    /**
     * 原子递增使用次数（解决并发超用问题）
     * 返回影响行数：1=成功，0=并发冲突/已用尽/已禁用
     */
    @Update("UPDATE t_invitation_code SET used_count = used_count + 1, " +
            "status = CASE WHEN used_count + 1 >= max_uses THEN #{exhaustedStatus} ELSE status END " +
            "WHERE code = #{code} AND status = #{enabledStatus} AND used_count < max_uses")
    int incrementUsedCountAtomically(@Param("code") String code,
                                     @Param("enabledStatus") int enabledStatus,
                                     @Param("exhaustedStatus") int exhaustedStatus);
}
