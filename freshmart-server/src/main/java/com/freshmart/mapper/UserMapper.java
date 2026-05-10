/* 会员数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
