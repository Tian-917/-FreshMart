/* 不允许删除异常 */
package com.freshmart.exception;

public class DeletionNotAllowedException extends BaseException {

    public DeletionNotAllowedException(String msg) {
        super(msg);
    }

}
