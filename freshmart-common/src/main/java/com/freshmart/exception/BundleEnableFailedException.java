/* 组合装启用失败异常 */
package com.freshmart.exception;

/**
 * 组合商品启用失败异常
 */
public class BundleEnableFailedException extends BaseException {

    public BundleEnableFailedException(){}

    public BundleEnableFailedException(String msg){
        super(msg);
    }
}
