/* 订单概览返回结果 */
package com.freshmart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单概览数据（门店工作台卡片）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOverViewVO implements Serializable {
    //待拣货数量
    private Integer waitingOrders;

    //已接单（待配送）数量
    private Integer deliveredOrders;

    //已完成数量
    private Integer completedOrders;

    //已取消数量
    private Integer cancelledOrders;

    //全部订单
    private Integer allOrders;
}
