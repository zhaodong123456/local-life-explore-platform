package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体（对应 sys_role）
 *
 * @author explore-platform
 */
@Data
@TableName("sys_role")
public class Role implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码 user/merchant/admin
     */
    private String roleCode;

    private String roleName;

    private String description;

    /**
     * 1启用 0停用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
