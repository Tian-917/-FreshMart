/* 订单统计返回结果 */
package com.freshmart.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderStatisticsVO implements Serializable {
    //待接单数量
    private Integer toBeConfirmed;

    //待配送数量
    private Integer confirmed;

    //配送中数量
    private Integer deliveryInProgress;
}
