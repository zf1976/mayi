package com.zf1976.mayi.auth;

import com.zf1976.mayi.auth.feign.RemoteUserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author mac
 * @date 2021/2/10
 **/
@SpringBootApplication(scanBasePackages = "com.zf1976")
@EnableDiscoveryClient
@EnableAsync
@EnableFeignClients(basePackageClasses = RemoteUserService.class)
@EnableConfigurationProperties
public class OAuth2AuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OAuth2AuthorizationServerApplication.class, args);
    }

}