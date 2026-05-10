/* 订单明细数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.entity.TradeOrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TradeOrderItemMapper extends BaseMapper<TradeOrderItem> {

    /**
     * 批量插入订单明细数据（BaseMapper 未提供批量插入，保留一条自定义 SQL）
     * @param orderItemList
     */
    @Insert("<script>" +
            "insert into trade_order_item(name, image, order_id, product_id, bundle_id, spec_text, amount) values " +
            "<foreach collection='orderItemList' item='od' separator=','>" +
            "(#{od.name}, #{od.image}, #{od.orderId}, #{od.productId}, #{od.bundleId}, #{od.specText}, #{od.amount})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("orderItemList") List<TradeOrderItem> orderItemList);
}
