/* 购物车参数 */
package com.freshmart.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CartDTO implements Serializable {

    private Long productId;
    private Long bundleId;
    private String specText;

}
