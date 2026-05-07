/* 商品规格实体 */
package com.freshmart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品规格
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_spec")
public class ProductSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    //商品id
    private Long productId;

    //规格名称
    private String name;

    //规格数据list
    private String value;

}
