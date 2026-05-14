/* 商品接口（会员端） */
package com.freshmart.controller.user;

import com.freshmart.result.Result;
import com.freshmart.service.ProductService;
import com.freshmart.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/user/product")
@RestController("userProductController")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 根据id类目查询商品（缓存读写逻辑在 service 层完成）
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<ProductVO>> list(Long categoryId){
        log.info("根据id类目查询商品:{}",categoryId);
        List<ProductVO> list = productService.listWithSpec(categoryId);
        return Result.success(list);
    }

    /**
     * 查询商品详情（含规格），用于商品详情页
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id){
        log.info("查询商品详情:{}", id);
        return Result.success(productService.getByIdWithSpec(id));
    }
}
