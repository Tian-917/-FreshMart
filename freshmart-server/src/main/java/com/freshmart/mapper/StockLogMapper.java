/* 库存流水数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.entity.StockLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {

    /**
     * 插入库存流水，唯一键冲突时忽略
     */
    @Insert("insert ignore into stock_log(order_id, product_id, quantity, type, create_time) " +
            "values(#{orderId}, #{productId}, #{quantity}, #{type}, now())")
    int insertIgnore(StockLog stockLog);
}
