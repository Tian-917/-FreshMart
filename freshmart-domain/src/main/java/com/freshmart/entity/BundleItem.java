/* 组合装明细实体 */
package com.freshmart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 组合商品与商品的关联关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bundle_item")
public class BundleItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    //组合商品id
    private Long bundleId;

    //商品id
    private Long productId;

    //商品名称 （冗余字段）
    private String name;

    //商品原价
    private BigDecimal price;

    //份数
    private Integer copies;
}
