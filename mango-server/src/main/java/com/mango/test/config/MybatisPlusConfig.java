package com.mango.test.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.test.config.mybatis.JsonTypeHandler;
import com.mango.test.common.util.UuidUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 */
@Configuration
@MapperScan("com.mango.test.**.mapper")
public class MybatisPlusConfig {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    
    /**
     * 注册自定义的JsonTypeHandler
     */
    @Bean
    public JsonTypeHandler jsonTypeHandler() {
        return new JsonTypeHandler();
    }

    /**
     * 自定义ID生成器
     */
    @Bean
    public IdentifierGenerator idGenerator() {
        return new IdentifierGenerator() {
            @Override
            public Number nextId(Object entity) {
                // 不使用数字ID
                return null;
            }

            @Override
            public String nextUUID(Object entity) {
                // 使用UUID作为ID
                return UuidUtil.generateUuidWithHyphen();
            }
        };
    }

    /**
     * 类型处理器配置
     */
    @Bean
    public com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler jacksonTypeHandler() {
        return new com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler(Object.class);
    }
}
