/* 商品管理接口 */
package com.freshmart.controller.admin;

import com.freshmart.dto.ProductDTO;
import com.freshmart.dto.ProductPageQueryDTO;
import com.freshmart.entity.Product;
import com.freshmart.result.PageResult;
import com.freshmart.result.Result;
import com.freshmart.service.ProductService;
import com.freshmart.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("adminProductController")
@RequestMapping("/admin/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 新增商品（缓存清理在 service 层完成）
     * @param productDTO
     * @return
     */
    @PostMapping
    public Result save(@RequestBody ProductDTO productDTO) {
        log.info("新增商品：{}", productDTO);
        productService.saveWithSpec(productDTO);
        return Result.success();
    }

    /**
     * 商品分页查询
     * @param productPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(ProductPageQueryDTO productPageQueryDTO) {
        log.info("商品分页查询:{}", productPageQueryDTO);
        PageResult pageResult = productService.pageQuery(productPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 商品批量删除（缓存清理在 service 层完成）
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids) {
        log.info("商品批量删除:{}", ids);
        productService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询商品和对应的规格数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id){
        log.info("根据id查询商品:{}", id);
        ProductVO productVO = productService.getByIdWithSpec(id);
        return  Result.success(productVO);
    }

    /**
     * 修改商品（缓存清理在 service 层完成）
     * @param productDTO
     * @return
     */
    @PutMapping
    public Result update(@RequestBody ProductDTO productDTO){
        log.info("修改商品:{}", productDTO);
        productService.updateWithSpec(productDTO);
        return Result.success();
    }

    /**
     * 根据类目id查询商品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Product>> list(Long categoryId){
        log.info("根据类目id查询商品:{}",categoryId);
        List<Product> list = productService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 商品上下架（缓存清理在 service 层完成）
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result starOrStop(@PathVariable Integer status, Long id){
        log.info("商品上下架：status={}, id={}", status, id);
        productService.starOrStop(status, id);
        return Result.success();
    }
}
