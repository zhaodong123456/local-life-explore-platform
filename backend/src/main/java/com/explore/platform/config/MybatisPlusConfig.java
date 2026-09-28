package com.explore.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置（分页插件）
 *
 * @author explore-platform
 */
@Configuration
//这是 MyBatis-Plus 的分页插件配置类，没有这个配置，MP 的分页 Page 对象不会自动生成 limit 分页 SQL，分页会失效（查全表）
public class MybatisPlusConfig {

    @Bean
    //MybatisPlusInterceptor 是 MP 的分页插件，用于在 MP 中使用分页查询
    //PaginationInnerInterceptor 是 MP 的分页插件，用于在 MP 中使用分页查询
    //DbType.MYSQL 表示数据库类型为 MySQL
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    // MybatisPlusInterceptor
// MyBatis-Plus 的统一插件入口（主拦截器）
// MP 所有插件（分页、乐观锁、防全表更新、多租户等）都要加到这个拦截器里面，链式添加 addInnerInterceptor。
}
