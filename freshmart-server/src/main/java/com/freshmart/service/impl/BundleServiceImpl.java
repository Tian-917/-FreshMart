/* 组合装业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmart.constant.MessageConstant;
import com.freshmart.constant.StatusConstant;
import com.freshmart.dto.BundleDTO;
import com.freshmart.dto.BundlePageQueryDTO;
import com.freshmart.entity.Category;
import com.freshmart.entity.Product;
import com.freshmart.entity.Bundle;
import com.freshmart.entity.BundleItem;
import com.freshmart.exception.DeletionNotAllowedException;
import com.freshmart.mapper.CategoryMapper;
import com.freshmart.mapper.ProductMapper;
import com.freshmart.mapper.BundleItemMapper;
import com.freshmart.mapper.BundleMapper;
import com.freshmart.result.PageResult;
import com.freshmart.service.BundleService;
import com.freshmart.vo.ProductItemVO;
import com.freshmart.vo.BundleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BundleServiceImpl implements BundleService {

    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private BundleItemMapper bundleItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 条件查询
     * @param bundle
     * @return
     */
    @Override
    public List<Bundle> list(Bundle bundle) {
        LambdaQueryWrapper<Bundle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(bundle.getName() != null, Bundle::getName, bundle.getName())
                .eq(bundle.getCategoryId() != null, Bundle::getCategoryId, bundle.getCategoryId())
                .eq(bundle.getStatus() != null, Bundle::getStatus, bundle.getStatus());
        return bundleMapper.selectList(queryWrapper);
    }

    /**
     * 根据id查询商品选项
     * @param id
     * @return
     */
    @Override
    public List<ProductItemVO> getProductItemById(Long id) {
        return bundleMapper.getProductItemByBundleId(id);
    }

    /**
     * 新增组合商品
     * @param bundleDTO
     */
    @Override
    @Transactional
    public void saveWithItems(BundleDTO bundleDTO) {
        Bundle bundle = new Bundle();
        BeanUtils.copyProperties(bundleDTO, bundle);
        //向组合商品表插入数据
        bundleMapper.insert(bundle);
        //获取生成的组合商品Id
        Long bundleId = bundle.getId();

        List<BundleItem> items = bundleDTO.getItems();
        items.forEach(bundleItem -> {
            bundleItem.setBundleId(bundleId);
        });
        //保存组合商品和商品的关联关系
        bundleItemMapper.insertBatch(items);
    }

    /**
     * 分页查询
     * @param bundlePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(BundlePageQueryDTO bundlePageQueryDTO) {
        Page<Bundle> page = new Page<>(bundlePageQueryDTO.getPage(), bundlePageQueryDTO.getPageSize());

        LambdaQueryWrapper<Bundle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(bundlePageQueryDTO.getName() != null && !"".equals(bundlePageQueryDTO.getName()),
                        Bundle::getName, bundlePageQueryDTO.getName())
                .eq(bundlePageQueryDTO.getStatus() != null, Bundle::getStatus, bundlePageQueryDTO.getStatus())
                .eq(bundlePageQueryDTO.getCategoryId() != null, Bundle::getCategoryId, bundlePageQueryDTO.getCategoryId())
                .orderByDesc(Bundle::getCreateTime);

        bundleMapper.selectPage(page, queryWrapper);

        //批量查询类目名称
        List<Long> categoryIds = page.getRecords().stream()
                .map(Bundle::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<BundleVO> bundleVOList = page.getRecords().stream().map(bundle -> {
            BundleVO bundleVO = new BundleVO();
            BeanUtils.copyProperties(bundle, bundleVO);
            bundleVO.setCategoryName(categoryNameMap.get(bundle.getCategoryId()));
            return bundleVO;
        }).collect(Collectors.toList());

        //批量查询组合商品明细
        List<Long> bundleIds = bundleVOList.stream().map(BundleVO::getId).collect(Collectors.toList());
        if (!bundleIds.isEmpty()) {
            Map<Long, List<BundleItem>> itemMap = bundleItemMapper.selectList(
                            new LambdaQueryWrapper<BundleItem>().in(BundleItem::getBundleId, bundleIds))
                    .stream().collect(Collectors.groupingBy(BundleItem::getBundleId));
            bundleVOList.forEach(vo -> vo.setItems(itemMap.getOrDefault(vo.getId(), Collections.emptyList())));
        }

        return new PageResult(page.getTotal(), bundleVOList);
    }

    /**
     * 批量删除组合商品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断是否为上架状态
        ids.forEach(id -> {
            Bundle bundle = bundleMapper.selectById(id);
            if (bundle != null && bundle.getStatus() == StatusConstant.ENABLE) {
                //上架中不能被删除
                throw new DeletionNotAllowedException(MessageConstant.BUNDLE_ON_SALE);
            }
        });
        ids.forEach(bundleId -> {
            //删除组合商品中的数据
            bundleMapper.deleteById(bundleId);
            //删除组合商品与商品的关联关系表中的数据
            bundleItemMapper.delete(new LambdaQueryWrapper<BundleItem>().eq(BundleItem::getBundleId, bundleId));
        });
    }

    /**
     * 根据id查询组合商品和组合商品与商品的关联关系
     * @param id
     * @return
     */
    @Override
    public BundleVO getByIdWithItems(Long id) {
        Bundle bundle = bundleMapper.selectById(id);
        List<BundleItem> items = bundleItemMapper.selectList(
                new LambdaQueryWrapper<BundleItem>().eq(BundleItem::getBundleId, id));

        BundleVO bundleVO = new BundleVO();
        BeanUtils.copyProperties(bundle, bundleVO);
        bundleVO.setItems(items);
        return bundleVO;
    }

    /**
     * 修改组合商品
     * @param bundleDTO
     */
    @Override
    @Transactional
    public void update(BundleDTO bundleDTO) {
        Bundle bundle = new Bundle();
        BeanUtils.copyProperties(bundleDTO, bundle);

        //修改组合商品表
        bundleMapper.updateById(bundle);

        //获取组合商品id
        Long bundleId = bundle.getId();

        //删除组合商品和商品的关联关系，操作bundle_item表，执行delete
        bundleItemMapper.delete(new LambdaQueryWrapper<BundleItem>().eq(BundleItem::getBundleId, bundleId));

        //更新组合商品和商品的关联关系，操作bundle_item表，执行insertBatch
        List<BundleItem> items = bundleDTO.getItems();
        items.forEach(bundleItem -> {
            bundleItem.setBundleId(bundleId);
        });
        //重新插入组合商品和商品的关联关系，操作bundle_item表，执行insert
        bundleItemMapper.insertBatch(items);
    }

    /**
     * 组合商品上下架
     * @param status
     * @param id
     */
    @Override
    public void starOrStop(Integer status, Long id) {
        //上架组合商品时，判断组合商品内是否有下架商品，有下架商品提示"组合商品内包含未上架商品，无法上架"
        if (status == StatusConstant.ENABLE) {
            //select a.* from product a left join bundle_item b on a.id = b.product_id where b.bundle_id = ?
            List<BundleItem> items = bundleItemMapper.selectList(
                    new LambdaQueryWrapper<BundleItem>().eq(BundleItem::getBundleId, id));
            List<Long> productIds = items.stream()
                    .map(BundleItem::getProductId)
                    .distinct()
                    .collect(Collectors.toList());
            if (!productIds.isEmpty()) {
                List<Product> productList = productMapper.selectBatchIds(productIds);
                productList.forEach(product -> {
                    if (product.getStatus() == StatusConstant.DISABLE) {
                        throw new DeletionNotAllowedException(MessageConstant.BUNDLE_ENABLE_FAILED);
                    }
                });
            }
        }
        Bundle bundle = Bundle.builder()
                .id(id)
                .status(status)
                .build();
        bundleMapper.updateById(bundle);
    }
}
