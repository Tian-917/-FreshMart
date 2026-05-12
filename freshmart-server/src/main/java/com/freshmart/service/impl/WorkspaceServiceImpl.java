/* 工作台业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.freshmart.constant.StatusConstant;
import com.freshmart.entity.Product;
import com.freshmart.entity.TradeOrder;
import com.freshmart.entity.Bundle;
import com.freshmart.entity.User;
import com.freshmart.mapper.ProductMapper;
import com.freshmart.mapper.OrderMapper;
import com.freshmart.mapper.BundleMapper;
import com.freshmart.mapper.UserMapper;
import com.freshmart.service.WorkspaceService;
import com.freshmart.vo.BusinessDataVO;
import com.freshmart.vo.ProductOverViewVO;
import com.freshmart.vo.OrderOverViewVO;
import com.freshmart.vo.BundleOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private BundleMapper bundleMapper;

    /**
     * 统计今日营业数据（当天 00:00:00 ~ 23:59:59）
     * @return
     */
    @Override
    public BusinessDataVO getTodayBusinessData() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        return getBusinessData(begin, end);
    }

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 销售额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：销售额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        //查询总订单数
        Integer totalOrderCount = getOrderCount(begin, end, null);

        //销售额（已完成订单的总金额）
        Double gmv = getGmv(begin, end, TradeOrder.COMPLETED);

        //有效订单数（已完成订单数量）
        Integer validOrderCount = getOrderCount(begin, end, TradeOrder.COMPLETED);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0 && validOrderCount != 0) {
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = BigDecimal.valueOf(gmv / validOrderCount)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        //新增用户数
        Integer newUsers = getNewUserCount(begin, end);

        return BusinessDataVO.builder()
                .gmv(gmv)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 统计指定时间区间内的订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        LambdaQueryWrapper<TradeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(TradeOrder::getOrderTime, begin)
                .lt(TradeOrder::getOrderTime, end)
                .eq(status != null, TradeOrder::getStatus, status);
        Long count = orderMapper.selectCount(queryWrapper);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 统计指定时间区间内已完成订单的销售额合计
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Double getGmv(LocalDateTime begin, LocalDateTime end, Integer status) {
        List<Object> objs = orderMapper.selectObjs(
                new QueryWrapper<TradeOrder>()
                        .select("ifnull(sum(amount), 0)")
                        .gt("order_time", begin)
                        .lt("order_time", end)
                        .eq(status != null, "status", status));
        if (objs == null || objs.isEmpty() || objs.get(0) == null) {
            return 0.0;
        }
        return ((Number) objs.get(0)).doubleValue();
    }

    /**
     * 统计指定时间区间内新增用户数量
     * @param begin
     * @param end
     * @return
     */
    private Integer getNewUserCount(LocalDateTime begin, LocalDateTime end) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .gt(User::getCreateTime, begin)
                        .le(User::getCreateTime, end));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);

        //待拣货
        Integer waitingOrders = getOrderCountAfter(begin, TradeOrder.TO_BE_CONFIRMED);

        //已接单（待配送）
        Integer deliveredOrders = getOrderCountAfter(begin, TradeOrder.CONFIRMED);

        //已完成
        Integer completedOrders = getOrderCountAfter(begin, TradeOrder.COMPLETED);

        //已取消
        Integer cancelledOrders = getOrderCountAfter(begin, TradeOrder.CANCELLED);

        //全部订单
        Integer allOrders = getOrderCountAfter(begin, null);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 统计当天指定状态（可为空）的订单数量
     * @param begin
     * @param status
     * @return
     */
    private Integer getOrderCountAfter(LocalDateTime begin, Integer status) {
        LambdaQueryWrapper<TradeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(TradeOrder::getOrderTime, begin)
                .eq(status != null, TradeOrder::getStatus, status);
        Long count = orderMapper.selectCount(queryWrapper);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 查询商品总览
     *
     * @return
     */
    public ProductOverViewVO getProductOverView() {
        Long sold = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, StatusConstant.ENABLE));
        Long discontinued = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, StatusConstant.DISABLE));

        return ProductOverViewVO.builder()
                .sold(sold == null ? 0 : sold.intValue())
                .discontinued(discontinued == null ? 0 : discontinued.intValue())
                .build();
    }

    /**
     * 查询组合商品总览
     *
     * @return
     */
    public BundleOverViewVO getBundleOverView() {
        Long sold = bundleMapper.selectCount(
                new LambdaQueryWrapper<Bundle>().eq(Bundle::getStatus, StatusConstant.ENABLE));
        Long discontinued = bundleMapper.selectCount(
                new LambdaQueryWrapper<Bundle>().eq(Bundle::getStatus, StatusConstant.DISABLE));

        return BundleOverViewVO.builder()
                .sold(sold == null ? 0 : sold.intValue())
                .discontinued(discontinued == null ? 0 : discontinued.intValue())
                .build();
    }
}
