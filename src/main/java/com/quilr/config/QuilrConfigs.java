package com.quilr.config;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Log4j2
@NoArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "config")
public class QuilrConfigs {
    @Value("${spring.application.name}")
    private String appName;
    private String identifier = "Quilr";
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
