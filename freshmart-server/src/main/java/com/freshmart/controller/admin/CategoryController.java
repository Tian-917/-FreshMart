/* 分类管理接口 */
package com.freshmart.controller.admin;

import com.freshmart.dto.CategoryDTO;
import com.freshmart.dto.CategoryPageQueryDTO;
import com.freshmart.entity.Category;
import com.freshmart.result.PageResult;
import com.freshmart.result.Result;

import com.freshmart.service.CategoryService;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController("adminCategoryController")
@RequestMapping("/admin/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增类目
     * @param categoryDTO
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增类目：{}",categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }


    /**
     * 类目分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("类目分页查询:{}",categoryPageQueryDTO);
        PageResult pageResult= categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 启用禁用类目
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String>  startOrStop(@PathVariable Integer status,Long id){
        log.info("启用禁用类目：{}",status);
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 修改类目
     * @param categoryDTO
     * @return
     */
    @PutMapping
    public Result<String> update(@RequestBody CategoryDTO categoryDTO){
        log.info("修改类目:{}",categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success();

    }

    /**
     * 根据id删除类目（关联校验逻辑在 service 层完成）
     * @param id
     * @return
     */
    @DeleteMapping
    public Result<String> deleteById(Long id){
        log.info("根据id删除类目：{}",id);
        categoryService.deleteById(id);
        return Result.success();
    }


    /**
     * 根据类型查询类目
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type){
        log.info("根据类型查询类目：{}",type);
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }

}
