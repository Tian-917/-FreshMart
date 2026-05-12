/* 工作台业务接口 */
package com.freshmart.service;

import com.freshmart.vo.BusinessDataVO;
import com.freshmart.vo.ProductOverViewVO;
import com.freshmart.vo.OrderOverViewVO;
import com.freshmart.vo.BundleOverViewVO;
import java.time.LocalDateTime;

public interface WorkspaceService {

    /**
     * 统计今日营业数据
     * @return
     */
    BusinessDataVO getTodayBusinessData();

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据
     * @return
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询商品总览
     * @return
     */
    ProductOverViewVO getProductOverView();

    /**
     * 查询组合商品总览
     * @return
     */
    BundleOverViewVO getBundleOverView();

}
