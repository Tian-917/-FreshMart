/* 组合装概览返回结果 */
package com.freshmart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 组合商品总览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleOverViewVO implements Serializable {
    // 已上架数量
    private Integer sold;

    // 已下架数量
    private Integer discontinued;
}
