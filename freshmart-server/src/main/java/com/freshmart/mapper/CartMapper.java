/* 购物车数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.entity.Cart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 批量插入购物车数据（BaseMapper 未提供批量插入，保留一条自定义 SQL）
     * @param cartList
     */
    @Insert("<script>" +
            "insert into cart (name, image, user_id, product_id, bundle_id, spec_text, number, amount, create_time) values " +
            "<foreach collection='cartList' item='cart' separator=','>" +
            "(#{cart.name}, #{cart.image}, #{cart.userId}, #{cart.productId}, " +
            "#{cart.bundleId}, #{cart.specText}, #{cart.number}, #{cart.amount}, " +
            "#{cart.createTime})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("cartList") List<Cart> cartList);
}
