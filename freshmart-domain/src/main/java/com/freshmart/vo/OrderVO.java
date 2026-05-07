/* 订单返回结果 */
package com.freshmart.vo;

import com.freshmart.entity.TradeOrderItem;
import com.freshmart.entity.TradeOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends TradeOrder implements Serializable {

    //订单商品摘要（商品名*数量 拼接）
    private String productSummary;

    //订单详情
    private List<TradeOrderItem> orderItemList;

}
