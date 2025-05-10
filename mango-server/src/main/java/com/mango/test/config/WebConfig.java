package com.mango.test.config;

import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return container -> {
            container.addContextCustomizers(context -> {
                // 禁用 TLD 扫描
                StandardJarScanner jarScanner = new StandardJarScanner();
                jarScanner.setScanAllDirectories(false);
                jarScanner.setScanClassPath(false);
                context.setJarScanner(jarScanner);
            });
        };
    }
}
