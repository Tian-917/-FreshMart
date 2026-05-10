/* 订单状态定时任务 */
package com.freshmart.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freshmart.entity.TradeOrder;
import com.freshmart.mapper.OrderMapper;
import com.freshmart.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 */
@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderService orderService;

    /**
     * 处理超时未支付的订单（下单超过15分钟）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeOutOrder() {
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<TradeOrder> tradeOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getStatus, TradeOrder.PENDING_PAYMENT)
                        .lt(TradeOrder::getOrderTime, time));
        if (tradeOrders == null || tradeOrders.isEmpty()) {
            return;
        }
        log.info("定时处理超时未支付订单，共 {} 笔：{}", tradeOrders.size(), LocalDateTime.now());
        for (TradeOrder tradeOrder : tradeOrders) {
            try {
                //关单和库存回滚在同一个事务里，状态条件更新保证只关一次
                orderService.cancelTimeoutOrder(tradeOrder.getId());
            } catch (Exception e) {
                log.error("超时关单失败, orderId={}", tradeOrder.getId(), e);
            }
        }
    }

    /**
     * 处理长时间处于派送中的订单（每天凌晨1点）
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<TradeOrder> tradeOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getStatus, TradeOrder.DELIVERY_IN_PROGRESS)
                        .lt(TradeOrder::getOrderTime, time));
        if (tradeOrders == null || tradeOrders.isEmpty()) {
            return;
        }
        log.info("定时处理配送中的订单，共 {} 笔：{}", tradeOrders.size(), LocalDateTime.now());
        for (TradeOrder tradeOrder : tradeOrders) {
            try {
                orderService.finishDeliveryOrder(tradeOrder.getId());
            } catch (Exception e) {
                log.error("自动完结失败, orderId={}", tradeOrder.getId(), e);
            }
        }
    }
}
