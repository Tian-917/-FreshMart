/* 库存不足异常 */
package com.freshmart.exception;

/**
 * 库存不足
 */
public class StockNotEnoughException extends BaseException {

    public StockNotEnoughException() {
    }

    public StockNotEnoughException(String msg) {
        super(msg);
    }
}
