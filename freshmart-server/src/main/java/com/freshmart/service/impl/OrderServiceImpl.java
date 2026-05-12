/* 订单业务实现 */
package com.freshmart.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmart.constant.MessageConstant;
import com.freshmart.context.BaseContext;
import com.freshmart.dto.*;
import com.freshmart.entity.AddressBook;
import com.freshmart.entity.BundleItem;
import com.freshmart.entity.Cart;
import com.freshmart.entity.TradeOrder;
import com.freshmart.entity.TradeOrderItem;
import com.freshmart.exception.AddressBookBusinessException;
import com.freshmart.exception.CartBusinessException;
import com.freshmart.exception.OrderBusinessException;
import com.freshmart.mapper.AddressBookMapper;
import com.freshmart.mapper.BundleItemMapper;
import com.freshmart.mapper.CartMapper;
import com.freshmart.mapper.OrderMapper;
import com.freshmart.mapper.TradeOrderItemMapper;
import com.freshmart.result.PageResult;
import com.freshmart.service.OrderService;
import com.freshmart.service.StockService;
import com.freshmart.vo.OrderPaymentVO;
import com.freshmart.vo.OrderStatisticsVO;
import com.freshmart.vo.OrderSubmitVO;
import com.freshmart.vo.OrderVO;
import com.freshmart.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private TradeOrderItemMapper orderItemMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private BundleItemMapper bundleItemMapper;
    @Autowired
    private StockService stockService;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param orderSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrderSubmitDTO orderSubmitDTO) {

        String idempotentToken = orderSubmitDTO.getIdempotentToken();

        //幂等校验：同一令牌已经下过单，直接返回那笔订单
        if (StringUtils.isNotBlank(idempotentToken)) {
            TradeOrder exist = orderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                    .eq(TradeOrder::getIdempotentToken, idempotentToken));
            if (exist != null) {
                log.info("重复提交命中幂等令牌，返回已有订单: token={}, orderId={}", idempotentToken, exist.getId());
                return buildSubmitVO(exist);
            }
        }

        //地址簿、购物车校验
        AddressBook addressBook = addressBookMapper.selectById(orderSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        List<Cart> cartList = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        if (cartList == null || cartList.isEmpty()) {
            throw new CartBusinessException(MessageConstant.CART_IS_NULL);
        }

        //汇总要扣减的库存，组合商品按组成拆解到单品
        Map<Long, Integer> stockItems = collectStockItems(cartList);

        //下单前校验库存，不足直接拒绝
        stockService.checkBeforeSubmit(stockItems);

        //向订单表插入1条数据
        TradeOrder tradeOrder = new TradeOrder();
        //打包费和餐具数在实体里是基本类型，不传时补0，避免拷贝空值报错
        if (orderSubmitDTO.getPackAmount() == null) {
            orderSubmitDTO.setPackAmount(0);
        }
        if (orderSubmitDTO.getTablewareNumber() == null) {
            orderSubmitDTO.setTablewareNumber(0);
        }
        BeanUtils.copyProperties(orderSubmitDTO, tradeOrder);
        tradeOrder.setOrderTime(LocalDateTime.now());
        tradeOrder.setPayStatus(TradeOrder.UN_PAID);
        tradeOrder.setStatus(TradeOrder.PENDING_PAYMENT);
        //订单号：FM + 下单时间 + 4位随机数
        tradeOrder.setNumber("FM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        tradeOrder.setPhone(addressBook.getPhone());
        tradeOrder.setConsignee(addressBook.getConsignee());
        tradeOrder.setUserId(userId);
        tradeOrder.setIdempotentToken(idempotentToken);
        try {
            orderMapper.insert(tradeOrder);
        } catch (DuplicateKeyException e) {
            //唯一索引保证并发提交只有一次能成功
            log.warn("重复提交被唯一索引拦下: token={}", idempotentToken);
            throw new OrderBusinessException(MessageConstant.ORDER_DUPLICATE_SUBMIT);
        }

        //向订单明细表插入n条数据
        List<TradeOrderItem> orderItemList = new ArrayList<>();
        for (Cart cart : cartList) {
            TradeOrderItem orderItem = new TradeOrderItem();
            BeanUtils.copyProperties(cart, orderItem);
            orderItem.setOrderId(tradeOrder.getId());
            orderItemList.add(orderItem);
        }
        orderItemMapper.insertBatch(orderItemList);

        //扣减库存并记录流水
        stockService.deduct(tradeOrder.getId(), stockItems);

        //清空购物车
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));

        //刷新库存缓存
        stockService.refreshAll(stockItems.keySet());

        return buildSubmitVO(tradeOrder);
    }

    /**
     * 汇总订单占用的库存，单品直接取数量，组合商品按组成*份数拆解
     */
    private Map<Long, Integer> collectStockItems(List<Cart> cartList) {
        Map<Long, Integer> items = new LinkedHashMap<>();
        for (Cart cart : cartList) {
            if (cart.getProductId() != null) {
                items.merge(cart.getProductId(), cart.getNumber(), Integer::sum);
            } else if (cart.getBundleId() != null) {
                List<BundleItem> bundleItems = bundleItemMapper.selectList(
                        new LambdaQueryWrapper<BundleItem>().eq(BundleItem::getBundleId, cart.getBundleId()));
                for (BundleItem bundleItem : bundleItems) {
                    if (bundleItem.getProductId() != null) {
                        items.merge(bundleItem.getProductId(),
                                bundleItem.getCopies() * cart.getNumber(), Integer::sum);
                    }
                }
            }
        }
        return items;
    }

    private OrderSubmitVO buildSubmitVO(TradeOrder tradeOrder) {
        return OrderSubmitVO.builder()
                .id(tradeOrder.getId())
                .orderTime(tradeOrder.getOrderTime())
                .orderAmount(tradeOrder.getAmount())
                .orderNumber(tradeOrder.getNumber())
                .build();
    }

    /**
     * 查询历史订单
     */
    @Override
    public PageResult pageQueryUser(int pageNum, int pageSize, Integer status) {
        Page<TradeOrder> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<TradeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TradeOrder::getUserId, BaseContext.getCurrentId())
                .eq(status != null, TradeOrder::getStatus, status)
                .orderByDesc(TradeOrder::getOrderTime);

        orderMapper.selectPage(page, queryWrapper);

        List<OrderVO> list = new ArrayList<>();
        if (page.getTotal() > 0) {
            for (TradeOrder tradeOrder : page.getRecords()) {
                List<TradeOrderItem> orderItems = orderItemMapper.selectList(
                        new LambdaQueryWrapper<TradeOrderItem>().eq(TradeOrderItem::getOrderId, tradeOrder.getId()));
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(tradeOrder, orderVO);
                orderVO.setOrderItemList(orderItems);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详细
     */
    @Override
    public OrderVO detail(Long id) {
        TradeOrder tradeOrder = orderMapper.selectById(id);
        if (tradeOrder == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<TradeOrderItem> orderItemList = orderItemMapper.selectList(
                new LambdaQueryWrapper<TradeOrderItem>().eq(TradeOrderItem::getOrderId, id));
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(tradeOrder, orderVO);
        orderVO.setOrderItemList(orderItemList);
        return orderVO;
    }

    /**
     * 用户取消订单
     */
    @Override
    @Transactional
    public void userCancelById(Long id) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, id)
                .in(TradeOrder::getStatus, TradeOrder.PENDING_PAYMENT, TradeOrder.TO_BE_CONFIRMED)
                .set(TradeOrder::getStatus, TradeOrder.CANCELLED)
                .set(TradeOrder::getCancelReason, "用户取消")
                .set(TradeOrder::getCancelTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_CHANGED);
        }
        stockService.release(id);
        log.info("用户取消订单 {}，库存已回滚", id);
    }

    /**
     * 再来一单
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<TradeOrderItem> orderItemList = orderItemMapper.selectList(
                new LambdaQueryWrapper<TradeOrderItem>().eq(TradeOrderItem::getOrderId, id));
        List<Cart> cartList = orderItemList.stream()
                .map(x -> {
                    Cart cart = new Cart();
                    BeanUtils.copyProperties(x, cart);
                    cart.setUserId(userId);
                    cart.setCreateTime(LocalDateTime.now());
                    return cart;
                }).collect(Collectors.toList());
        cartMapper.insertBatch(cartList);
    }

    /**
     * 订单搜索
     */
    @Override
    public PageResult conditionSearch(OrderPageQueryDTO orderPageQueryDTO) {
        Page<TradeOrder> page = new Page<>(orderPageQueryDTO.getPage(), orderPageQueryDTO.getPageSize());

        LambdaQueryWrapper<TradeOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(orderPageQueryDTO.getNumber() != null && !"".equals(orderPageQueryDTO.getNumber()),
                        TradeOrder::getNumber, orderPageQueryDTO.getNumber())
                .like(orderPageQueryDTO.getPhone() != null && !"".equals(orderPageQueryDTO.getPhone()),
                        TradeOrder::getPhone, orderPageQueryDTO.getPhone())
                .eq(orderPageQueryDTO.getUserId() != null, TradeOrder::getUserId, orderPageQueryDTO.getUserId())
                .eq(orderPageQueryDTO.getStatus() != null, TradeOrder::getStatus, orderPageQueryDTO.getStatus())
                .ge(orderPageQueryDTO.getBeginTime() != null, TradeOrder::getOrderTime, orderPageQueryDTO.getBeginTime())
                .le(orderPageQueryDTO.getEndTime() != null, TradeOrder::getOrderTime, orderPageQueryDTO.getEndTime())
                .orderByDesc(TradeOrder::getOrderTime);

        orderMapper.selectPage(page, queryWrapper);

        List<OrderVO> orderVOList = getOrderVOList(page.getRecords());
        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(List<TradeOrder> tradeOrders) {
        List<OrderVO> orderVOList = new ArrayList<>();
        if (tradeOrders != null && !tradeOrders.isEmpty()) {
            for (TradeOrder tradeOrder : tradeOrders) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(tradeOrder, orderVO);
                orderVO.setProductSummary(getProductSummaryStr(tradeOrder));
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取商品信息字符串
     */
    private String getProductSummaryStr(TradeOrder tradeOrder) {
        List<TradeOrderItem> orderItemList = orderItemMapper.selectList(
                new LambdaQueryWrapper<TradeOrderItem>().eq(TradeOrderItem::getOrderId, tradeOrder.getId()));
        List<String> orderProductList = orderItemList.stream()
                .map(x -> x.getName() + "*" + x.getNumber() + ";")
                .collect(Collectors.toList());
        return String.join("", orderProductList);
    }

    /**
     * 各个状态的订单数量统计
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(getCountByStatus(TradeOrder.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(getCountByStatus(TradeOrder.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(getCountByStatus(TradeOrder.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    private Integer getCountByStatus(Integer status) {
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getStatus, status));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 模拟支付成功
     */
    @Override
    public OrderPaymentVO pay(OrderPaymentDTO orderPaymentDTO) {
        TradeOrder tradeOrderDB = orderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getNumber, orderPaymentDTO.getOrderNumber()));
        if (tradeOrderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //已支付的订单重复调用直接返回
        if (tradeOrderDB.getStatus() >= TradeOrder.TO_BE_CONFIRMED
                && !TradeOrder.CANCELLED.equals(tradeOrderDB.getStatus())) {
            log.info("订单 {} 已支付，幂等返回", tradeOrderDB.getNumber());
            return buildPaymentVO();
        }

        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, tradeOrderDB.getId())
                .eq(TradeOrder::getStatus, TradeOrder.PENDING_PAYMENT)
                .set(TradeOrder::getStatus, TradeOrder.TO_BE_CONFIRMED)
                .set(TradeOrder::getPayStatus, TradeOrder.PAID)
                .set(TradeOrder::getCheckoutTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_CHANGED);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 1); //1 新单播报 2 客户催单
        map.put("orderId", tradeOrderDB.getId());
        map.put("content", "订单号:" + tradeOrderDB.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));

        return buildPaymentVO();
    }

    private OrderPaymentVO buildPaymentVO() {
        //模拟支付结果，个人项目没有真实微信支付
        return OrderPaymentVO.builder()
                .nonceStr(String.valueOf(System.currentTimeMillis()))
                .paySign("mock_sign")
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("RSA")
                .packageStr("prepay_id=mock_prepay_id")
                .build();
    }

    /**
     * 接单
     */
    @Override
    public void confirm(OrderConfirmDTO orderConfirmDTO) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, orderConfirmDTO.getId())
                .eq(TradeOrder::getStatus, TradeOrder.TO_BE_CONFIRMED)
                .set(TradeOrder::getStatus, TradeOrder.CONFIRMED));
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_CHANGED);
        }
    }

    /**
     * 拒单
     */
    @Override
    @Transactional
    public void rejection(OrderRejectDTO orderRejectDTO) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, orderRejectDTO.getId())
                .eq(TradeOrder::getStatus, TradeOrder.TO_BE_CONFIRMED)
                .set(TradeOrder::getStatus, TradeOrder.CANCELLED)
                .set(TradeOrder::getRejectionReason, orderRejectDTO.getRejectionReason())
                .set(TradeOrder::getCancelTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_CHANGED);
        }
        stockService.release(orderRejectDTO.getId());
        log.info("拒单 {}，库存已回滚", orderRejectDTO.getId());
    }

    /**
     * 商家取消订单
     */
    @Override
    @Transactional
    public void concel(OrderCancelDTO orderCancelDTO) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, orderCancelDTO.getId())
                .in(TradeOrder::getStatus,
                        TradeOrder.PENDING_PAYMENT, TradeOrder.TO_BE_CONFIRMED,
                        TradeOrder.CONFIRMED, TradeOrder.DELIVERY_IN_PROGRESS)
                .set(TradeOrder::getStatus, TradeOrder.CANCELLED)
                .set(TradeOrder::getCancelReason, orderCancelDTO.getCancelReason())
                .set(TradeOrder::getCancelTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_CHANGED);
        }
        stockService.release(orderCancelDTO.getId());
        log.info("商家取消订单 {}，库存已回滚", orderCancelDTO.getId());
    }

    /**
     * 派送
     */
    @Override
    public void delivery(Long id) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, id)
                .eq(TradeOrder::getStatus, TradeOrder.CONFIRMED)
                .set(TradeOrder::getStatus, TradeOrder.DELIVERY_IN_PROGRESS));
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_CHANGED);
        }
    }

    /**
     * 完成
     */
    @Override
    public void complete(Long id) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, id)
                .eq(TradeOrder::getStatus, TradeOrder.DELIVERY_IN_PROGRESS)
                .set(TradeOrder::getStatus, TradeOrder.COMPLETED)
                .set(TradeOrder::getDeliveryTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_CHANGED);
        }
    }

    /**
     * 超时未支付关单，定时任务调用
     */
    @Override
    @Transactional
    public void cancelTimeoutOrder(Long orderId) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, orderId)
                .eq(TradeOrder::getStatus, TradeOrder.PENDING_PAYMENT)
                .set(TradeOrder::getStatus, TradeOrder.CANCELLED)
                .set(TradeOrder::getCancelReason, "订单超时未支付，系统自动取消")
                .set(TradeOrder::getCancelTime, LocalDateTime.now()));
        if (rows == 0) {
            return;
        }
        stockService.release(orderId);
        log.info("超时关单 {}，库存已回滚", orderId);
    }

    /**
     * 长时间处于派送中的订单自动完结，定时任务调用
     */
    @Override
    public void finishDeliveryOrder(Long orderId) {
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, orderId)
                .eq(TradeOrder::getStatus, TradeOrder.DELIVERY_IN_PROGRESS)
                .set(TradeOrder::getStatus, TradeOrder.COMPLETED)
                .set(TradeOrder::getDeliveryTime, LocalDateTime.now()));
        if (rows > 0) {
            log.info("配送超时单 {} 自动完结", orderId);
        }
    }

    /**
     * 催单
     */
    @Override
    public void reminder(Long id) {
        TradeOrder tradeOrderDB = orderMapper.selectById(id);
        if (tradeOrderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2); //1 新单播报 2 客户催单
        map.put("orderId", id);
        map.put("content", "订单号" + tradeOrderDB.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
