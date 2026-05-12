/* 组合装业务接口 */
package com.freshmart.service;


import com.freshmart.dto.BundleDTO;
import com.freshmart.dto.BundlePageQueryDTO;
import com.freshmart.entity.Bundle;
import com.freshmart.result.PageResult;
import com.freshmart.vo.ProductItemVO;
import com.freshmart.vo.BundleVO;

import java.util.List;

public interface BundleService {

    /**
     * 条件查询
     * @param bundle
     * @return
     */
    List<Bundle> list(Bundle bundle);

    /**
     * 根据id查询商品选项
     * @param id
     * @return
     */
    List<ProductItemVO> getProductItemById(Long id);

    /**
     * 新增组合商品
     * @param bundleDTO
     */
    void saveWithItems(BundleDTO bundleDTO);

    /**
     * 分页查询
     * @param bundlePageQueryDTO
     * @return
     */
    PageResult pageQuery(BundlePageQueryDTO bundlePageQueryDTO);

    /**
     * 批量删除组合商品
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询组合商品，用于修改页面回显数据
     * @param id
     * @return
     */
    BundleVO getByIdWithItems(Long id);

    /**
     * 修改组合商品
     * @param bundleDTO
     */
    void update(BundleDTO bundleDTO);

    /**
     * 组合商品上下架
     * @param status
     * @param id
     */
    void starOrStop(Integer status, Long id);
}
