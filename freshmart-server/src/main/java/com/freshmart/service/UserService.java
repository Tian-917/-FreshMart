/* 会员业务接口 */
package com.freshmart.service;


import com.freshmart.dto.UserLoginDTO;
import com.freshmart.entity.User;

public interface UserService {

    /**
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
