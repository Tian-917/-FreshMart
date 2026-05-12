/* 库存业务实现 */
package com.freshmart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freshmart.constant.MessageConstant;
import com.freshmart.entity.Product;
import com.freshmart.entity.StockLog;
import com.freshmart.exception.StockNotEnoughException;
import com.freshmart.mapper.ProductMapper;
import com.freshmart.mapper.StockLogMapper;
import com.freshmart.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 库存业务实现
 */
@Service
@Slf4j
public class StockServiceImpl implements StockService {

    /** 库存缓存过期时间 */
    private static final Duration STOCK_TTL = Duration.ofSeconds(60);

    /** 库存缓存key前缀 */
    private static final String STOCK_KEY_PREFIX = "freshmart:stock:";

    /**
     * 库存校验脚本，返回1足够 0不足 -1未命中
     */
    private static final DefaultRedisScript<Long> CHECK_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('get', KEYS[1]) " +
                    "if v == false then return -1 end " +
                    "if tonumber(v) >= tonumber(ARGV[1]) then return 1 end " +
                    "return 0",
            Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StockLogMapper stockLogMapper;

    private String key(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    @Override
    public int getStock(Long productId) {
        String cached = stringRedisTemplate.opsForValue().get(key(productId));
        if (cached != null) {
            return Integer.parseInt(cached);
        }
        int stock = loadStockFromDb(productId);
        stringRedisTemplate.opsForValue().set(key(productId), String.valueOf(stock), STOCK_TTL);
        return stock;
    }

    @Override
    public void preload(Collection<Long> productIds) {
        if (productIds == null) return;
        productIds.forEach(this::refresh);
    }

    @Override
    public void checkBeforeSubmit(Map<Long, Integer> items) {
        if (items == null || items.isEmpty()) return;
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            int result = checkByLua(productId, quantity);
            if (result == -1) {
                //缓存未命中，回源后重试一次
                refresh(productId);
                result = checkByLua(productId, quantity);
            }
            if (result == 0) {
                //缓存可能偏低，以数据库为准再判断
                int dbStock = loadStockFromDb(productId);
                refresh(productId);
                if (dbStock < quantity) {
                    throw new StockNotEnoughException(
                            MessageConstant.STOCK_NOT_ENOUGH + "：" + productName(productId));
                }
            }
        }
    }

    @Override
    public void deduct(Long orderId, Map<Long, Integer> items) {
        if (items == null || items.isEmpty()) return;
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            //影响行数为0说明库存不足
            int rows = productMapper.deductStock(productId, quantity);
            if (rows == 0) {
                throw new StockNotEnoughException(
                        MessageConstant.STOCK_NOT_ENOUGH + "：" + productName(productId));
            }
            stockLogMapper.insert(StockLog.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .type(StockLog.TYPE_DEDUCT)
                    .createTime(LocalDateTime.now())
                    .build());
        }
        log.info("订单 {} 扣减库存完成: {}", orderId, items);
    }

    @Override
    public void release(Long orderId) {
        List<StockLog> deductLogs = stockLogMapper.selectList(new LambdaQueryWrapper<StockLog>()
                .eq(StockLog::getOrderId, orderId)
                .eq(StockLog::getType, StockLog.TYPE_DEDUCT));
        if (deductLogs == null || deductLogs.isEmpty()) {
            return;
        }
        for (StockLog deductLog : deductLogs) {
            //先插入回滚流水，唯一键冲突说明已经回滚过
            StockLog releaseLog = StockLog.builder()
                    .orderId(orderId)
                    .productId(deductLog.getProductId())
                    .quantity(deductLog.getQuantity())
                    .type(StockLog.TYPE_RELEASE)
                    .createTime(LocalDateTime.now())
                    .build();
            int inserted = stockLogMapper.insertIgnore(releaseLog);
            if (inserted == 0) {
                log.info("库存已回滚过，跳过: orderId={}, productId={}", orderId, deductLog.getProductId());
                continue;
            }
            productMapper.restoreStock(deductLog.getProductId(), deductLog.getQuantity());
            refresh(deductLog.getProductId());
            log.info("订单 {} 回滚库存: productId={}, quantity={}",
                    orderId, deductLog.getProductId(), deductLog.getQuantity());
        }
    }

    @Override
    public void refresh(Long productId) {
        stringRedisTemplate.opsForValue().set(key(productId), String.valueOf(loadStockFromDb(productId)), STOCK_TTL);
    }

    @Override
    public void refreshAll(Collection<Long> productIds) {
        if (productIds == null) return;
        productIds.forEach(this::refresh);
    }

    private int checkByLua(Long productId, Integer quantity) {
        Long result = stringRedisTemplate.execute(
                CHECK_SCRIPT,
                Collections.singletonList(key(productId)),
                String.valueOf(quantity));
        return result == null ? -1 : result.intValue();
    }

    private int loadStockFromDb(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStock() == null) {
            return 0;
        }
        return product.getStock();
    }

    private String productName(Long productId) {
        Product product = productMapper.selectById(productId);
        return product == null ? String.valueOf(productId) : product.getName();
    }
}
