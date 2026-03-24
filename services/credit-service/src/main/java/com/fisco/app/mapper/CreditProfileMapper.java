package com.fisco.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.EnterpriseCreditProfile;

/**
 * 企业信用档案 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface CreditProfileMapper extends BaseMapper<EnterpriseCreditProfile> {

    /**
     * 根据企业ID查询信用档案
     *
     * @param entId 企业ID
     * @return 信用档案
     */
    @Select("SELECT * FROM t_enterprise_credit_profile WHERE ent_id = #{entId}")
    EnterpriseCreditProfile selectByEntId(@Param("entId") Long entId);

    /**
     * 根据企业ID更新信用档案
     *
     * @param profile 信用档案
     * @return 影响行数
     */
    @Update("UPDATE t_enterprise_credit_profile SET credit_score = #{creditScore}, " +
            "credit_level = #{creditLevel}, available_limit = #{availableLimit}, " +
            "used_limit = #{usedLimit}, overdue_count = #{overdueCount}, " +
            "last_eval_time = #{lastEvalTime}, update_time = NOW() " +
            "WHERE ent_id = #{entId}")
    int updateByEntId(EnterpriseCreditProfile profile);
}
