/* 组合装返回结果 */
package com.freshmart.vo;

import com.freshmart.entity.BundleItem;
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
public class BundleVO implements Serializable {

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

    //更新时间
    private LocalDateTime updateTime;

    //类目名称
    private String categoryName;

    //组合商品和商品的关联关系
    private List<BundleItem> items = new ArrayList<>();
}
