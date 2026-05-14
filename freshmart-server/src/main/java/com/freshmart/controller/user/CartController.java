/* 购物车接口（会员端） */
package com.freshmart.controller.user;


import com.freshmart.dto.CartDTO;
import com.freshmart.entity.Cart;
import com.freshmart.result.Result;
import com.freshmart.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     * @param cartDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody CartDTO cartDTO){
        log.info("添加购物车，信息为：{}",cartDTO);
        cartService.add(cartDTO);
        return Result.success();
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public Result<List<Cart>> list(){
        log.info("查看购物车");
        List<Cart> carts = cartService.showCart();
        return Result.success(carts);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public Result clean(){
        log.info("清空购物车");
        cartService.cleanCart();
        return Result.success();
    }

    /**
     * 删除购物车中的一个商品
     * @return
     */
    @PostMapping("/sub")
    public Result sub(@RequestBody CartDTO cartDTO){
        log.info("删除购物车中的一个商品，商品为：{}",cartDTO);
        cartService.sub(cartDTO);
        return Result.success();
    }
}
