/* 购物车业务接口 */
package com.freshmart.service;

import com.freshmart.dto.CartDTO;
import com.freshmart.entity.Cart;

import java.util.List;

public interface CartService {

    /**
     * 添加购物车
     * @param cartDTO
     */
    void add(CartDTO cartDTO);

    /**
     * 查看购物车
     * @return
     */
    List<Cart> showCart();

    /**
     * 清空购物车
     */
    void cleanCart();

    /**
     * 删除购物车中的一个商品
     */
    void sub(CartDTO cartDTO);
}
