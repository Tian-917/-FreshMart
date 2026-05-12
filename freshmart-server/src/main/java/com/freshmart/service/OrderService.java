/* 订单业务接口 */
package com.freshmart.service;

import com.freshmart.dto.*;
import com.freshmart.result.PageResult;
import com.freshmart.vo.OrderPaymentVO;
import com.freshmart.vo.OrderStatisticsVO;
import com.freshmart.vo.OrderSubmitVO;
import com.freshmart.vo.OrderVO;

public interface OrderService {
    /**
     * 用户下单
     * @param orderSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrderSubmitDTO orderSubmitDTO);

    /**
     * 查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQueryUser(int page, int pageSize, Integer status);

    /**
     * 查询订单详细
     * @param id
     * @return
     */
    OrderVO detail(Long id);

    /**
     * 模拟支付成功，更新订单状态并推送新单播报
     * @param orderPaymentDTO
     * @return
     */
    OrderPaymentVO pay(OrderPaymentDTO orderPaymentDTO);

    /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id);

    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 订单搜索
     * @param orderPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrderPageQueryDTO orderPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     * @param orderConfirmDTO
     */
    void confirm(OrderConfirmDTO orderConfirmDTO);

    /**
     * 拒单
     * @param orderRejectDTO
     */
    void rejection(OrderRejectDTO orderRejectDTO);

    /**
     * 取消订单
     * @param orderCancelDTO
     */
    void concel(OrderCancelDTO orderCancelDTO);

    /**
     * 配送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);

    /**
     * 催单
     * @param id
     */
    void reminder(Long id);

    /**
     * 超时未支付关单（定时任务调用）：条件更新 + 回滚库存
     * @param orderId
     */
    void cancelTimeoutOrder(Long orderId);

    /**
     * 长时间停留在配送中的订单自动完结（定时任务调用）
     * @param orderId
     */
    void finishDeliveryOrder(Long orderId);
}
