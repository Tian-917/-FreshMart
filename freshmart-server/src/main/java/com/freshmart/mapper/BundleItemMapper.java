/* 组合装明细数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.entity.BundleItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BundleItemMapper extends BaseMapper<BundleItem> {

    /**
     * 批量保存组合商品和商品的关联关系（BaseMapper 未提供批量插入，保留一条自定义 SQL）
     * @param items
     */
    @Insert("<script>" +
            "insert into bundle_item (bundle_id, product_id, name, price, copies) values " +
            "<foreach collection='items' item='sd' separator=','>" +
            "(#{sd.bundleId}, #{sd.productId}, #{sd.name}, #{sd.price}, #{sd.copies})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("items") List<BundleItem> items);
}
