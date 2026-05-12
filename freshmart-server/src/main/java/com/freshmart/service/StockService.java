/* 库存业务接口 */
package com.freshmart.service;

import java.util.Collection;
import java.util.Map;

public interface StockService {

    /**
     * 查询库存
     * @param productId
     * @return
     */
    int getStock(Long productId);

    /**
     * 预热库存缓存
     * @param productIds
     */
    void preload(Collection<Long> productIds);

    /**
     * 下单前校验库存
     * @param items
     */
    void checkBeforeSubmit(Map<Long, Integer> items);

    /**
     * 扣减库存
     * @param orderId
     * @param items
     */
    void deduct(Long orderId, Map<Long, Integer> items);

    /**
     * 回滚库存
     * @param orderId
     */
    void release(Long orderId);

    /**
     * 刷新库存缓存
     * @param productId
     */
    void refresh(Long productId);

    /**
     * 批量刷新库存缓存
     * @param productIds
     */
    void refreshAll(Collection<Long> productIds);
}
