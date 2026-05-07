/* 库存流水实体 */
package com.freshmart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存流水
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("stock_log")
public class StockLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 变动类型 1下单扣减 2取消回滚
     */
    public static final Integer TYPE_DEDUCT = 1;
    public static final Integer TYPE_RELEASE = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    //订单id
    private Long orderId;

    //商品id
    private Long productId;

    //变动数量（正数）
    private Integer quantity;

    //变动类型 1下单扣减 2取消回滚
    private Integer type;

    private LocalDateTime createTime;
}
