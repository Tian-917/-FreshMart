/* 分类参数 */
package com.freshmart.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {

    //主键
    private Long id;

    //类型 1 商品类目 2 组合商品类目
    private Integer type;

    //类目名称
    private String name;

    //排序
    private Integer sort;

}
