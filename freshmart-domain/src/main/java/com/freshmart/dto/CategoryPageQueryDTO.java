/* 分类分页查询参数 */
package com.freshmart.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryPageQueryDTO implements Serializable {

    //页码
    private int page;

    //每页记录数
    private int pageSize;

    //类目名称
    private String name;

    //类目类型 1商品类目  2组合商品类目
    private Integer type;

}
