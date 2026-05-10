/* 商品规格数据访问 */
package com.freshmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmart.entity.ProductSpec;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductSpecMapper extends BaseMapper<ProductSpec> {

    /**
     * 批量插入规格数据（MyBatis-Plus BaseMapper 未提供批量插入，保留一条自定义 SQL）
     * @param specs
     */
    @Insert("<script>" +
            "insert into product_spec (product_id, name, value) values " +
            "<foreach collection='specs' item='df' separator=','>" +
            "(#{df.productId}, #{df.name}, #{df.value})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("specs") List<ProductSpec> specs);
}
