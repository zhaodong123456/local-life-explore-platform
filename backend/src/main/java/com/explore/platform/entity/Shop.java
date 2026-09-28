package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商铺实体（对应 tb_shop）
 *
 * @author explore-platform
 */
@Data
@TableName("tb_shop")
public class Shop implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String name;

    private Integer typeId;

    private String typeName;

    private String province;

    private String city;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String coverImg;

    private String images;

    private String description;

    private BigDecimal avgPrice;

    /**
     * 0待审核 1营业 2已下架
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
