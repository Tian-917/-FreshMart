/* 订单管理接口 */
package com.freshmart.controller.admin;


import com.freshmart.dto.OrderCancelDTO;
import com.freshmart.dto.OrderConfirmDTO;
import com.freshmart.dto.OrderPageQueryDTO;
import com.freshmart.dto.OrderRejectDTO;
import com.freshmart.result.PageResult;
import com.freshmart.result.Result;
import com.freshmart.service.OrderService;
import com.freshmart.vo.OrderStatisticsVO;
import com.freshmart.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminOrderController")
@RequestMapping("/admin/order")
public class OrderController {

    @Autowired
    private  OrderService orderService;

    /**
     * 订单搜索
     * @param orderPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrderPageQueryDTO orderPageQueryDTO){
        log.info("订单搜索");
        PageResult pageResult = orderService.conditionSearch(orderPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(){
        log.info("各个状态的订单数量统计");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> details(@PathVariable Long id){
        log.info("订单详情");
        OrderVO orderVO = orderService.detail(id);
        return  Result.success(orderVO);
    }

    /**
     * 接单
     * @param orderConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    public Result confirm(@RequestBody OrderConfirmDTO orderConfirmDTO){
        log.info("接单");
        orderService.confirm(orderConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     * @param orderRejectDTO
     * @return
     */
    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrderRejectDTO orderRejectDTO){
        log.info("拒单");
        orderService.rejection(orderRejectDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @param orderCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    public Result cancel(@RequestBody OrderCancelDTO orderCancelDTO){
        log.info("取消订单");
        orderService.concel(orderCancelDTO);
        return Result.success();
    }

    /**
     * 配送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    public Result delivery(@PathVariable Long id){
        log.info("配送订单");
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @return
     */
    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable("id") Long id) {
        log.info("完成订单");
        orderService.complete(id);
        return Result.success();
    }
}
