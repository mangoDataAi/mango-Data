package com.mango.test.config;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.util.Arrays;

/**
 * Jackson配置类
 * 用于处理Java 8日期时间类型的序列化
 */
@Configuration
public class JacksonConfig {

    private static final Logger logger = LoggerFactory.getLogger(JacksonConfig.class);

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        
        // 注册LocalDateTime的自定义反序列化器，支持ISO格式带时区的日期时间
        SimpleModule dateTimeModule = new SimpleModule();
        dateTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        objectMapper.registerModule(dateTimeModule);
        
        // 注册String类型字段的自定义反序列化器，支持数组到字符串的转换
        SimpleModule stringArrayModule = new SimpleModule();
        stringArrayModule.addDeserializer(String.class, new StringArrayDeserializer());
        objectMapper.registerModule(stringArrayModule);
        
        // 配置其他选项
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 配置MyBatisPlus的JacksonTypeHandler静态ObjectMapper
        JacksonTypeHandler.setObjectMapper(objectMapper);

        logger.info("Configured ObjectMapper with JavaTimeModule and String array support");
        return objectMapper;
    }

    /**
     * 自定义LocalDateTime反序列化器
     * 支持ISO格式的日期时间，包括带时区信息的格式
     */
    private static class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateString = p.getValueAsString();
            if (dateString == null || dateString.isEmpty()) {
                return null;
            }
            
            try {
                // 首先尝试直接解析为LocalDateTime
                return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e) {
                try {
                    // 如果失败，尝试解析为ZonedDateTime并转换为LocalDateTime
                    return ZonedDateTime.parse(dateString).toLocalDateTime();
                } catch (DateTimeParseException e2) {
                    logger.warn("无法解析日期时间: {}", dateString, e2);
                    return null;
                }
            }
        }
    }
    
    /**
     * 自定义字符串反序列化器，支持数组到逗号分隔字符串的转换
     */
    private static class StringArrayDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {
        
        private boolean shouldHandleArray = false;
        private String fieldName;
        
        // 默认构造函数
        public StringArrayDeserializer() {
            this.shouldHandleArray = false;
        }
        
        // 带有字段信息的构造函数
        public StringArrayDeserializer(String fieldName, boolean shouldHandleArray) {
            this.fieldName = fieldName;
            this.shouldHandleArray = shouldHandleArray;
        }
        
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            // 针对特定字段启用数组到字符串的转换
            if (property != null) {
                String name = property.getName();
                boolean handle = name != null && (
                    name.equals("validationTypes") || 
                    name.equals("checksumFields") || 
                    name.equals("sampleFields") ||
                    name.equals("weekDays") ||
                    name.equals("monthDays") ||
                    name.equals("retryConditions") ||
                    name.equals("retryActions") ||
                    name.equals("selectedColumns")
                );
                
                return new StringArrayDeserializer(name, handle);
            }
            
            return this;
        }
        
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // 如果不是需要特殊处理的字段，直接返回字符串值
            if (!shouldHandleArray) {
                return p.getValueAsString();
            }
            
            // 检查是否是数组开始
            if (p.currentToken() == JsonToken.START_ARRAY) {
                StringBuilder result = new StringBuilder();
                boolean first = true;
                
                // 迭代数组元素
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append(p.getValueAsString());
                    first = false;
                }
                
                logger.debug("转换字段 {} 的JSON数组为逗号分隔字符串: {}", fieldName, result);
                return result.toString();
            }
            
            // 如果不是数组，直接返回字符串值
            return p.getValueAsString();
        }
    }
}