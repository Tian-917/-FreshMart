/* 组合装参数 */
package com.freshmart.dto;

import com.freshmart.entity.BundleItem;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BundleDTO implements Serializable {

    private Long id;

    //类目id
    private Long categoryId;

    //组合商品名称
    private String name;

    //组合商品价格
    private BigDecimal price;

    //状态 0:停用 1:启用
    private Integer status;

    //描述信息
    private String description;

    //图片
    private String image;

    //组合商品与商品的关联关系
    private List<BundleItem> items = new ArrayList<>();

}
