/* 购物车业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freshmart.context.BaseContext;
import com.freshmart.dto.CartDTO;
import com.freshmart.entity.Product;
import com.freshmart.entity.Bundle;
import com.freshmart.entity.Cart;
import com.freshmart.mapper.ProductMapper;
import com.freshmart.mapper.BundleMapper;
import com.freshmart.mapper.CartMapper;
import com.freshmart.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private BundleMapper bundleMapper;

    /**
     * 添加购物车
     * @param cartDTO
     */
    @Override
    public void add(CartDTO cartDTO) {
        //判断当前加入到购物车中的商品是否存在了
        Cart cart = new Cart();
        BeanUtils.copyProperties(cartDTO, cart);
        Long userId = BaseContext.getCurrentId();
        cart.setUserId(userId);

        List<Cart> list = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(cartDTO.getProductId() != null, Cart::getProductId, cartDTO.getProductId())
                        .eq(cartDTO.getBundleId() != null, Cart::getBundleId, cartDTO.getBundleId())
                        .eq(cartDTO.getSpecText() != null, Cart::getSpecText, cartDTO.getSpecText()));

        //如果存在了，只需要将数量+1
        if (list != null && list.size() > 0) {
            Cart existCart = list.get(0);
            Cart updateCart = new Cart();
            updateCart.setId(existCart.getId());
            updateCart.setNumber(existCart.getNumber() + 1);
            cartMapper.updateById(updateCart);
        } else {
            //如果不存在，则需要插入一条购物车数据
            //判断本次添加到购物车的是商品还是组合商品
            Long productId = cartDTO.getProductId();
            Long bundleId = cartDTO.getBundleId();
            if (productId != null) {
                //本次添加到购物车的是商品
                Product product = productMapper.selectById(productId);
                cart.setName(product.getName());
                cart.setImage(product.getImage());
                cart.setAmount(product.getPrice());
            } else {
                //本次添加到购物车的是组合商品
                Bundle bundle = bundleMapper.selectById(bundleId);
                cart.setName(bundle.getName());
                cart.setImage(bundle.getImage());
                cart.setAmount(bundle.getPrice());
            }
            cart.setNumber(1);
            cart.setCreateTime(LocalDateTime.now());
            cartMapper.insert(cart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<Cart> showCart() {
        //获取当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        return cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByAsc(Cart::getCreateTime));
    }

    /**
     * 清空购物车
     */
    @Override
    public void cleanCart() {
        //获取当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    /**
     * 删除购物车中的一个商品
     */
    @Override
    public void sub(CartDTO cartDTO) {
        //查询当前用户购物车中商品中类型：商品 组合商品
        Cart cart = new Cart();
        BeanUtils.copyProperties(cartDTO, cart);
        //获取当前微信用的id
        Long userId = BaseContext.getCurrentId();
        cart.setUserId(userId);

        List<Cart> list = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(cartDTO.getProductId() != null, Cart::getProductId, cartDTO.getProductId())
                        .eq(cartDTO.getBundleId() != null, Cart::getBundleId, cartDTO.getBundleId())
                        .eq(cartDTO.getSpecText() != null, Cart::getSpecText, cartDTO.getSpecText()));

        //查看商品是否存在
        if (list != null && list.size() > 0) {
            Cart existCart = list.get(0);
            //获取当前商品的数量
            Integer number = existCart.getNumber();
            //如果num=1，则直接从购物车里删除
            if (number == 1) {
                cartMapper.deleteById(existCart.getId());
            } else {
                //当前购物车中该商品的num>1，则修改数量
                Cart updateCart = new Cart();
                updateCart.setId(existCart.getId());
                updateCart.setNumber(number - 1);
                cartMapper.updateById(updateCart);
            }
        }
    }
}
