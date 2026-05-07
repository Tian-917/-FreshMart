/* 商品参数 */
package com.freshmart.dto;

import com.freshmart.entity.ProductSpec;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDTO implements Serializable {

    private Long id;
    //商品名称
    private String name;
    //商品类目id
    private Long categoryId;
    //商品价格
    private BigDecimal price;
    //可售库存
    private Integer stock;
    //图片
    private String image;
    //描述信息
    private String description;
    //0 下架 1 上架
    private Integer status;
    //规格
    private List<ProductSpec> specs = new ArrayList<>();

}
