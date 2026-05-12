/* 商品业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmart.constant.MessageConstant;
import com.freshmart.constant.StatusConstant;
import com.freshmart.dto.ProductDTO;
import com.freshmart.dto.ProductPageQueryDTO;
import com.freshmart.entity.Category;
import com.freshmart.entity.Product;
import com.freshmart.entity.ProductSpec;
import com.freshmart.entity.Bundle;
import com.freshmart.entity.BundleItem;
import com.freshmart.exception.DeletionNotAllowedException;
import com.freshmart.mapper.CategoryMapper;
import com.freshmart.mapper.ProductSpecMapper;
import com.freshmart.mapper.ProductMapper;
import com.freshmart.mapper.BundleItemMapper;
import com.freshmart.mapper.BundleMapper;
import com.freshmart.result.PageResult;
import com.freshmart.service.ProductService;
import com.freshmart.service.StockService;
import com.freshmart.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSpecMapper specTextMapper;
    @Autowired
    private BundleItemMapper bundleItemMapper;
    @Autowired
    private BundleMapper bundleMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StockService stockService;

    /**
     * 新增商品
     * @param productDTO
     */
    @Transactional
    public void saveWithSpec(ProductDTO productDTO) {
        //向商品表插入1条数据
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        //运营端新增商品没有库存字段，默认给100件
        if (product.getStock() == null) {
            product.setStock(100);
        }
        productMapper.insert(product);

        //获取商品id,insert语句生成的主键值
        Long productId = product.getId();

        //向规格表插入n条数据
        List<ProductSpec> specs = productDTO.getSpecs();
        if (specs != null && specs.size() > 0) {
            specs.forEach(specText -> {
                specText.setProductId(productId);
            });
            //向规格表中插入n条数据
            specTextMapper.insertBatch(specs);
        }

        //清理缓存数据
        cleanCache("freshmart:product:*");
        //写入库存缓存
        stockService.refresh(productId);
    }

    /**
     * 商品分页查询
     * @param productPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO) {
        Page<Product> page = new Page<>(productPageQueryDTO.getPage(), productPageQueryDTO.getPageSize());

        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(productPageQueryDTO.getName() != null && !"".equals(productPageQueryDTO.getName()),
                        Product::getName, productPageQueryDTO.getName())
                .eq(productPageQueryDTO.getCategoryId() != null, Product::getCategoryId, productPageQueryDTO.getCategoryId())
                .eq(productPageQueryDTO.getStatus() != null, Product::getStatus, productPageQueryDTO.getStatus())
                .orderByDesc(Product::getCreateTime);

        productMapper.selectPage(page, queryWrapper);

        //批量查询类目名称
        Map<Long, String> categoryNameMap = getCategoryNameMap(page.getRecords());

        List<ProductVO> productVOList = page.getRecords().stream().map(product -> {
            ProductVO productVO = new ProductVO();
            BeanUtils.copyProperties(product, productVO);
            productVO.setCategoryName(categoryNameMap.get(product.getCategoryId()));
            return productVO;
        }).collect(Collectors.toList());

        //批量查询商品规格
        List<Long> productIds = productVOList.stream().map(ProductVO::getId).collect(Collectors.toList());
        if (!productIds.isEmpty()) {
            Map<Long, List<ProductSpec>> specMap = specTextMapper.selectList(
                            new LambdaQueryWrapper<ProductSpec>().in(ProductSpec::getProductId, productIds))
                    .stream().collect(Collectors.groupingBy(ProductSpec::getProductId));
            productVOList.forEach(vo -> vo.setSpecs(specMap.getOrDefault(vo.getId(), Collections.emptyList())));
        }

        return new PageResult(page.getTotal(), productVOList);
    }

    /**
     * 批量查询类目名称
     * @param productList
     * @return
     */
    private Map<Long, String> getCategoryNameMap(List<Product> productList) {
        List<Long> categoryIds = productList.stream()
                .map(Product::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    /**
     * 商品批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前商品是否能够删除--是否存在上架中的商品？
        for (Long id : ids) {
            Product product = productMapper.selectById(id);
            if (product != null && product.getStatus() == StatusConstant.ENABLE) {
                //当前商品处于上架中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.PRODUCT_ON_SALE);
            }
        }

        //判断当前商品是否能够删除--是否组合商品关联了？
        List<Long> bundleIds = getBundleIdByProductIds(ids);
        if (bundleIds != null && bundleIds.size() > 0) {
            //当前商品被组合商品关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_BUNDLE);
        }

        //根据商品id集合批量删除商品数据
        productMapper.deleteBatchIds(ids);

        //根据商品id集合批量删除商品关联的规格数据
        specTextMapper.delete(new LambdaQueryWrapper<ProductSpec>().in(ProductSpec::getProductId, ids));

        //清理缓存数据
        cleanCache("freshmart:product:*");
    }

    /**
     * 根据商品id集合查询关联的组合商品id
     * @param productIds
     * @return
     */
    private List<Long> getBundleIdByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> objs = bundleItemMapper.selectObjs(
                new QueryWrapper<BundleItem>()
                        .select("bundle_id")
                        .in("product_id", productIds));
        return objs.stream().map(obj -> ((Number) obj).longValue()).collect(Collectors.toList());
    }

    /**
     * 根据id查询商品和对应的规格数据
     * @param id
     * @return
     */
    @Override
    public ProductVO getByIdWithSpec(Long id) {
        //根据id查询商品数据
        Product product = productMapper.selectById(id);

        //根据商品id查询规格数据
        List<ProductSpec> specTexts = specTextMapper.selectList(
                new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, id));

        //将查询到的数据封装到ProductVO中
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        productVO.setSpecs(specTexts);
        //库存取实时值
        productVO.setStock(stockService.getStock(id));
        return productVO;
    }

    /**
     * 根据id修改商品和对应的规格数据
     * @param productDTO
     */
    @Override
    @Transactional
    public void updateWithSpec(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        //修改商品表基本信息
        productMapper.updateById(product);

        //删除原有的规格数据
        specTextMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, product.getId()));

        //重新插入规格数据
        List<ProductSpec> specs = productDTO.getSpecs();
        if (specs != null && specs.size() > 0) {
            specs.forEach(specText -> {
                specText.setProductId(productDTO.getId());
            });
            //向规格表中插入n条数据
            specTextMapper.insertBatch(specs);
        }

        //清理缓存数据
        cleanCache("freshmart:product:*");
        //刷新库存缓存
        stockService.refresh(productDTO.getId());
    }

    /**
     * 根据类目id查询商品（用户端，带redis缓存）
     * @param categoryId
     * @return
     */
    @Override
    public List<ProductVO> listWithSpec(Long categoryId) {
        //构造redis中的key，规则 product_类目Id
        String key = "freshmart:product:category:" + categoryId;

        //查询redis中是否存在商品数据
        List<ProductVO> list = (List<ProductVO>) redisTemplate.opsForValue().get(key);

        //如果存在，直接返回
        if (list != null && list.size() > 0) {
            return fillStock(list);
        }

        //如果不存在，查询数据库，并存入redis中
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getCategoryId, categoryId)
                .eq(Product::getStatus, StatusConstant.ENABLE)
                .orderByDesc(Product::getCreateTime);
        List<Product> productList = productMapper.selectList(queryWrapper);

        list = new ArrayList<>();
        for (Product d : productList) {
            ProductVO productVO = new ProductVO();
            BeanUtils.copyProperties(d, productVO);
            //根据商品id查询对应的规格
            List<ProductSpec> specs = specTextMapper.selectList(
                    new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, d.getId()));
            productVO.setSpecs(specs);
            list.add(productVO);
        }
        //库存不放进缓存，下单后库存会变
        list.forEach(vo -> vo.setStock(null));
        redisTemplate.opsForValue().set(key, list);
        return fillStock(list);
    }

    /**
     * 给商品列表补上实时库存
     */
    private List<ProductVO> fillStock(List<ProductVO> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        for (ProductVO vo : list) {
            vo.setStock(stockService.getStock(vo.getId()));
        }
        return list;
    }

    /**
     * 根据类目id查询商品（管理端）
     * @param categoryId
     * @return
     */
    @Override
    public List<Product> list(Long categoryId) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(Product::getStatus, StatusConstant.ENABLE)
                .orderByDesc(Product::getCreateTime);
        return productMapper.selectList(queryWrapper);
    }

    /**
     * 商品上下架
     * @param status
     * @param id
     */
    @Override
    public void starOrStop(Integer status, Long id) {
        //如果是下架操作，检查该商品是否被上架中的组合商品使用
        if (status == StatusConstant.DISABLE) {
            List<Long> bundleIds = getBundleIdByProductIds(Arrays.asList(id));
            if (bundleIds != null && bundleIds.size() > 0) {
                for (Long bundleId : bundleIds) {
                    Bundle bundle = bundleMapper.selectById(bundleId);
                    if (bundle != null && bundle.getStatus() == StatusConstant.ENABLE) {
                        //当前商品被上架中的组合商品关联了，不能下架
                        throw new DeletionNotAllowedException(MessageConstant.PRODUCT_BE_RELATED_BY_ACTIVE_BUNDLE);
                    }
                }
            }
        }

        Product product = Product.builder()
                .id(id)
                .status(status)
                .build();
        productMapper.updateById(product);

        //清理缓存数据
        cleanCache("freshmart:product:*");
    }

    /**
     * 清理缓存数据
     * @param pattern
     */
    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
