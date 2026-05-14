/* 组合装接口（会员端） */
package com.freshmart.controller.user;


import com.freshmart.constant.StatusConstant;
import com.freshmart.dto.BundleDTO;
import com.freshmart.entity.Bundle;
import com.freshmart.result.Result;
import com.freshmart.service.BundleService;
import com.freshmart.vo.ProductItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("userBundleController")
@RequestMapping("/user/bundle")
public class BundleController {

    @Autowired
    public BundleService bundleService;

    /**
     * 条件查询
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Cacheable(cacheNames = "bundleCache",key = "#categoryId")//key: bundleCache::categoryId
    public Result<List<Bundle>> list(Long categoryId){
        log.info("根据类目id查询组合商品:{}",categoryId);
        Bundle bundle = new Bundle();
        bundle.setCategoryId(categoryId);
        bundle.setStatus(StatusConstant.ENABLE);

        List<Bundle> list = bundleService.list(bundle);

        return Result.success(list);
    }

    /**
     * 根据组合商品id查询包含的商品列表
     * @param id
     * @return
     */
    @GetMapping("/product/{id}")
    public Result<List<ProductItemVO>> productList(@PathVariable("id") Long id){
        log.info("根据组合商品id查询包含的商品：{}",id);
        List<ProductItemVO> list = bundleService.getProductItemById(id);
        return  Result.success(list);
    }


}
