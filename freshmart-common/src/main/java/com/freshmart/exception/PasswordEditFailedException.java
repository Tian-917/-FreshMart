/* 修改密码失败异常 */
package com.freshmart.exception;

/**
 * 密码修改失败异常
 */
public class PasswordEditFailedException extends BaseException{

    public PasswordEditFailedException(String msg){
        super(msg);
    }

}
