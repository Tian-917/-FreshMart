/* 分类业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmart.constant.MessageConstant;
import com.freshmart.constant.StatusConstant;
import com.freshmart.dto.CategoryDTO;
import com.freshmart.dto.CategoryPageQueryDTO;
import com.freshmart.entity.Category;
import com.freshmart.entity.Product;
import com.freshmart.entity.Bundle;
import com.freshmart.exception.DeletionNotAllowedException;
import com.freshmart.mapper.CategoryMapper;
import com.freshmart.mapper.ProductMapper;
import com.freshmart.mapper.BundleMapper;
import com.freshmart.result.PageResult;
import com.freshmart.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private BundleMapper bundleMapper;

    /**
     * 新增类目
     * @param categoryDTO
     */
    @Override
    public void save(CategoryDTO categoryDTO) {
        //属性拷贝
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        //类目状态默认为禁用
        category.setStatus(StatusConstant.DISABLE);

        //公共字段（创建时间、修改时间、创建人、修改人）由 MyMetaObjectHandler 自动填充
        categoryMapper.insert(category);
    }

    /**
     * 启用、禁用类目
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        categoryMapper.updateById(category);
    }

    /**
     * 类目分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        Page<Category> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(categoryPageQueryDTO.getName() != null && !"".equals(categoryPageQueryDTO.getName()),
                        Category::getName, categoryPageQueryDTO.getName())
                .eq(categoryPageQueryDTO.getType() != null, Category::getType, categoryPageQueryDTO.getType())
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime);

        categoryMapper.selectPage(page, queryWrapper);

        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 修改类目
     * @param categoryDTO
     */
    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        //公共字段（修改时间、修改人）由 MyMetaObjectHandler 自动填充
        categoryMapper.updateById(category);
    }

    /**
     * 根据id删除类目
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        //查询当前类目是否关联了商品，如果关联了就抛出业务异常
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_PRODUCT);
        }

        //查询当前类目是否关联了组合商品，如果关联了就抛出业务异常
        Long bundleCount = bundleMapper.selectCount(
                new LambdaQueryWrapper<Bundle>().eq(Bundle::getCategoryId, id));
        if (bundleCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_BUNDLE);
        }

        categoryMapper.deleteById(id);
    }

    /**
     * 根据类型查询类目
     * @param type
     */
    @Override
    public List<Category> list(Integer type) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, StatusConstant.ENABLE)
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime);
        return categoryMapper.selectList(queryWrapper);
    }
}
