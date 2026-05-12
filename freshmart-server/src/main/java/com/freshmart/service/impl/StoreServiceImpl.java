/* 门店业务实现 */
package com.freshmart.service.impl;

import com.freshmart.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StoreServiceImpl implements StoreService {

    public static final String STORE_STATUS_KEY = "STORE_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置门店营业状态
     * @param status
     */
    @Override
    public void setStatus(Integer status) {
        log.info("设置门店营业状态为：{}", status == 1 ? "营业中" : "打样中");
        redisTemplate.opsForValue().set(STORE_STATUS_KEY, status);
    }

    /**
     * 获取门店的营业状态
     * @return
     */
    @Override
    public Integer getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(STORE_STATUS_KEY);
        log.info("获取门店的营业状态为：{}", status != null && status == 1 ? "营业中" : "打样中");
        return status;
    }
}
