/* 公共字段自动填充 */
package com.freshmart.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.freshmart.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共字段自动填充处理器
 * 代替原 @AutoFill 切面，在 insert/update 时自动填充创建时间、更新时间、创建人、修改人
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时的自动填充
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        if (metaObject.hasSetter("createTime")) {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        }
        if (metaObject.hasSetter("updateTime")) {
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        }
        if (metaObject.hasSetter("createUser")) {
            this.strictInsertFill(metaObject, "createUser", Long.class, currentId);
        }
        if (metaObject.hasSetter("updateUser")) {
            this.strictInsertFill(metaObject, "updateUser", Long.class, currentId);
        }
    }

    /**
     * 更新时的自动填充
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updateTime")) {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }
        if (metaObject.hasSetter("updateUser")) {
            this.strictUpdateFill(metaObject, "updateUser", Long.class, BaseContext.getCurrentId());
        }
    }
}
