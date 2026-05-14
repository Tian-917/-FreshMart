/* 分类接口（会员端） */
package com.freshmart.controller.user;


import com.freshmart.entity.Category;
import com.freshmart.result.Result;
import com.freshmart.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController("userCategoryController")
@RequestMapping("/user/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询类目
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type){
        log.info("查询类目：{}",type);
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
}
