package com.explore.platform.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;//元对象处理器，用于在 MyBatis-Plus 中自动填充字段值
import org.apache.ibatis.reflection.MetaObject;//元对象，用于反射操作数据库记录
import org.springframework.stereotype.Component;//组件注解，用于将类标记为 Spring 组件，自动扫描并管理

import java.time.LocalDateTime;//日期时间类，用于表示日期和时间

/**
 * MyBatis-Plus 字段自动填充处理器
 * <p>
 * 插入时自动填充 createTime / updateTime，更新时自动填充 updateTime，
 * 与实体上的 @TableField(fill = FieldFill.INSERT / INSERT_UPDATE) 配合使用。
 *
 * @author explore-platform
 */
@Component
//新增、修改数据时，MP 在执行 SQL 之前自动给字段赋值，不用你在业务代码手动 setCreateTime()、setCreateBy()。
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
