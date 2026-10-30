package com.leansofx.qaserviceuser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置。
 *
 * <p>仅使用 Spring Security Crypto 的 BCrypt 实现，不引入完整 Spring Security，
 * 避免其默认拦截全部接口带来的额外配置成本。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 密码编码器，强度因子 10。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
