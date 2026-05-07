/* 组合装分页查询参数 */
package com.freshmart.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BundlePageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    private String name;

    //类目id
    private Integer categoryId;

    //状态 0表示禁用 1表示启用
    private Integer status;

}
