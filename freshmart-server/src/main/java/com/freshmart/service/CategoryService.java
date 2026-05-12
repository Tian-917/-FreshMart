/* 分类业务接口 */
package com.freshmart.service;

import com.freshmart.dto.CategoryDTO;
import com.freshmart.dto.CategoryPageQueryDTO;
import com.freshmart.entity.Category;
import com.freshmart.result.PageResult;

import java.util.List;

public interface CategoryService {

    /**
     * 新增员工
     * @param categoryDTO
     */
    void save(CategoryDTO categoryDTO);


    /**
     * 启用、禁用类目
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 类目分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 修改类目
     * @param categoryDTO
     */
    void update(CategoryDTO categoryDTO);

    /**
     * 根据id删除类目
     * @param id
     */
    void deleteById(Long id);

    /**
     * 根据类型查询类目
     * @param type
     */
    List<Category> list(Integer type);
}