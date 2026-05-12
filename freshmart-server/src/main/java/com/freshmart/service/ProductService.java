/* 商品业务接口 */
package com.freshmart.service;

import com.freshmart.dto.ProductDTO;
import com.freshmart.dto.ProductPageQueryDTO;
import com.freshmart.entity.Product;
import com.freshmart.result.PageResult;
import com.freshmart.vo.ProductVO;

import java.util.List;

public interface ProductService {

    /**
     * 新增商品
     * @param productDTO
     */
    public void saveWithSpec(ProductDTO productDTO);

    /**
     * 商品分页查询
     * @param productPageQueryDTO
     * @return
     */
    PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO);

    /**
     * 商品批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询商品和对应的规格数据
     * @param id
     * @return
     */
    ProductVO getByIdWithSpec(Long id);

    /**
     * 根据id修改商品和对应的规格数据
     * @param productDTO
     */
    void updateWithSpec(ProductDTO productDTO);

    /**
     * 根据类目id查询商品及其规格（用户端，带缓存）
     * @param categoryId
     * @return
     */
    List<ProductVO> listWithSpec(Long categoryId);

    /**
     * 根据类目id查询商品
     * @param categoryId
     * @return
     */
    List<Product> list(Long categoryId);

    /**
     * 商品上下架
     * @param status
     * @param id
     */
    void starOrStop(Integer status, Long id);
}
