/* 商品销量参数 */
package com.freshmart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSalesDTO implements Serializable {
    //商品名称
    private String productName;

    //动销件数
    private Integer soldCount;
}
