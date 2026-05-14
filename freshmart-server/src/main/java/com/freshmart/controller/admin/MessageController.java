/* 消息接口 */
package com.freshmart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freshmart.entity.TradeOrder;
import com.freshmart.mapper.OrderMapper;
import com.freshmart.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/messages")
public class MessageController {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 查询未读消息数
     * @return
     */
    @GetMapping("/countUnread")
    public Result<Integer> countUnread() {
        //未读消息数即待拣货订单数
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getStatus, TradeOrder.TO_BE_CONFIRMED));
        return Result.success(count == null ? 0 : count.intValue());
    }
}
