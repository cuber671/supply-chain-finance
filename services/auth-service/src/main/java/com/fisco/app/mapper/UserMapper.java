package com.fisco.app.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fisco.app.entity.User;

/**
 * 用户 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 直接更新最后登录时间（避免read-modify-write并发问题）
     */
    @Update("UPDATE t_user SET last_login_time = #{lastLoginTime} WHERE user_id = #{userId}")
    int updateLastLoginTimeDirect(@Param("userId") Long userId, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    /**
     * 分页查询企业下的用户列表
     */
    @Select("SELECT * FROM t_user WHERE enterprise_id = #{enterpriseId} ORDER BY create_time DESC")
    IPage<User> selectUserPage(Page<User> page, @Param("enterpriseId") Long enterpriseId);
}
