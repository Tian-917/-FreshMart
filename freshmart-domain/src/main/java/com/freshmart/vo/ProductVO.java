/* 商品返回结果 */
package com.freshmart.vo;

import com.freshmart.entity.ProductSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO implements Serializable {

    private Long id;
    //商品名称
    private String name;
    //商品类目id
    private Long categoryId;
    //商品价格
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;
    //0 下架 1 上架
    private Integer status;
    //可售库存（实时值，不写入缓存）
    private Integer stock;
    //更新时间
    private LocalDateTime updateTime;
    //类目名称
    private String categoryName;
    //商品关联的规格
    private List<ProductSpec> specs = new ArrayList<>();

    //private Integer copies;
}
