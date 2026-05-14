/* 订单接口（会员端） */
package com.freshmart.controller.user;


import com.freshmart.dto.OrderPaymentDTO;
import com.freshmart.dto.OrderSubmitDTO;
import com.freshmart.result.PageResult;
import com.freshmart.result.Result;
import com.freshmart.service.OrderService;
import com.freshmart.vo.OrderPaymentVO;
import com.freshmart.vo.OrderSubmitVO;
import com.freshmart.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("userOrderController")
@RequestMapping("/user/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param orderSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        log.info("用户下单：{}",orderSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(orderSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/historyTradeOrder")
    public Result<PageResult> page(int page,int pageSize,Integer status){
        log.info("历史订单查询");
        PageResult pageResult = orderService.pageQueryUser(page,pageSize,status);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @GetMapping("/orderItem/{id}")
    public Result<OrderVO> details(@PathVariable Long id){
        OrderVO orderVO = orderService.detail(id);
        return Result.success(orderVO);
    }

    /**
     * 模拟支付
     * @param orderPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    public Result<OrderPaymentVO> pay(@RequestBody OrderPaymentDTO orderPaymentDTO){
        log.info("模拟支付：{}", orderPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.pay(orderPaymentDTO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 用户取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id){
        orderService.userCancelById(id);
        return  Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable Long id){
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 催单
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    public Result reminder(@PathVariable Long id){
        orderService.reminder(id);
        return Result.success();
    }
}
