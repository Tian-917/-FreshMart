/* 组合装管理接口 */
package com.freshmart.controller.admin;


import com.freshmart.dto.BundleDTO;
import com.freshmart.dto.BundlePageQueryDTO;
import com.freshmart.result.PageResult;
import com.freshmart.result.Result;
import com.freshmart.service.BundleService;
import com.freshmart.vo.BundleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("adminBundleController")
@RequestMapping("/admin/bundle")
public class BundleController {

    @Autowired
    private BundleService bundleService;

    /**
     * 新增组合商品
     * @param bundleDTO
     * @return
     */
    @PostMapping
    @CacheEvict(cacheNames = "bundleCache",key = "#bundleDTO.categoryId")//kty::bundleCache::categoryId
    public Result save(@RequestBody BundleDTO bundleDTO) {
        log.info("新增组合商品:{}",bundleDTO);
        bundleService.saveWithItems(bundleDTO);
        return Result.success();
    }

    /**
     * 分页查询
      * @param bundlePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(BundlePageQueryDTO bundlePageQueryDTO) {
        log.info("分页查询:{}",bundlePageQueryDTO);
        PageResult pageResult = bundleService.pageQuery(bundlePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除组合商品
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(cacheNames = "bundleCache",allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除组合商品：{}",ids);
        bundleService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询组合商品，用于修改页面回显数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<BundleVO> getById(@PathVariable Long id){
        log.info("根据id查询组合商品，用于修改页面回显数据:{}",id);
        BundleVO bundleVO = bundleService.getByIdWithItems(id);
        return Result.success(bundleVO);
    }

    /**
     * 修改组合商品
     * @param bundleDTO
     * @return
     */
    @PutMapping
    @CacheEvict(cacheNames = "bundleCache",allEntries = true)
    public Result update(@RequestBody BundleDTO bundleDTO){
        log.info("修改组合商品：{}",bundleDTO);
        bundleService.update(bundleDTO);
        return Result.success();
    }

    /**
     * 组合商品上下架
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "bundleCache",allEntries = true)
    public Result starOrStop(@PathVariable Integer status,Long id){
        log.info("组合商品上下架：{}",status,id);
        bundleService.starOrStop(status,id);
        return Result.success();
    }
}
