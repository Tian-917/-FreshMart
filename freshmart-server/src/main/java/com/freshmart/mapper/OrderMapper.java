/* 订单数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.dto.ProductSalesDTO;
import com.freshmart.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<TradeOrder> {

    /**
     * 统计销量排名top10商品
     * （涉及 trade_order_item 与 tradeOrder 两表关联 + group by + limit，条件构造器不便表达，保留一条自定义 SQL）
     * @param beginTime
     * @param endTime
     * @return
     */
    @Select("select od.name as productName, sum(od.number) as soldCount " +
            "from trade_order_item od, trade_order o " +
            "where od.order_id = o.id and o.status = 5 " +
            "and o.order_time > #{beginTime} and o.order_time < #{endTime} " +
            "group by od.name order by soldCount desc limit 10")
    List<ProductSalesDTO> getTop10Products(@Param("beginTime") LocalDateTime beginTime,
                                      @Param("endTime") LocalDateTime endTime);
}
