/* 门店业务接口 */
package com.freshmart.service;

public interface StoreService {

    /**
     * 设置门店营业状态
     * @param status
     */
    void setStatus(Integer status);

    /**
     * 获取门店的营业状态
     * @return
     */
    Integer getStatus();
}
