package com.mango.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@MapperScan("com.mango.test.**.mapper")
public class MangoTestApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MangoTestApplication.class, args);
    }
}
