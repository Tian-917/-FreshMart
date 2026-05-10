/* 组合装数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.entity.Bundle;
import com.freshmart.vo.ProductItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BundleMapper extends BaseMapper<Bundle> {

    /**
     * 根据组合商品id查询包含的商品选项
     * （bundle_item 与 product 两表关联，条件构造器不便表达，保留一条自定义 SQL）
     * @param bundleId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from bundle_item sd left join product d on sd.product_id = d.id " +
            "where sd.bundle_id = #{bundleId}")
    List<ProductItemVO> getProductItemByBundleId(Long bundleId);
}
