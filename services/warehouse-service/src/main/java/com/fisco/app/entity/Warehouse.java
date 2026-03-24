package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 仓库实体类
 *
 * 对应数据库表: t_warehouse
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_warehouse")
public class Warehouse {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ent_id")
    private Long entId;

    private String name;

    private String address;

    @TableField("contact_user")
    private String contactUser;

    @TableField("contact_phone")
    private String contactPhone;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 状态常量
    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_SUSPENDED = 2;
    public static final int STATUS_CLOSED = 3;
}
