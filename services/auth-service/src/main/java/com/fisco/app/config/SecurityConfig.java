package com.fisco.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

/**
 * 安全配置 - JWT双令牌策略认证
 * Spring Security 5.7+ 兼容版本 (Spring Boot 2.7)
 * 使用 SecurityFilterChain + Lambda DSL 替代已弃用的 WebSecurityConfigurerAdapter
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（前后端分离使用JWT）
            .csrf().disable()
            // 不使用Session
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置拦截规则
            .authorizeRequests(auth -> auth
                // 允许访问认证接口（登录、刷新Token等）
                .antMatchers("/api/v1/auth/login", "/api/v1/auth/admin/login", "/api/v1/auth/refresh", "/api/v1/auth/validate", "/api/v1/auth/users/register").permitAll()
                // 用户管理接口需要认证
                .antMatchers("/api/v1/auth/users/**").authenticated()
                // 允许访问Swagger
                .antMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .antMatchers("/webjars/**").permitAll()
                // 允许健康检查端点
                .antMatchers("/health", "/", "/error").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated())
            // 添加JWT认证过滤器（在FilterSecurityInterceptor之前运行）
            .addFilterBefore(jwtAuthenticationFilter, FilterSecurityInterceptor.class);

        return http.build();
    }

    /**
     * 密码编码器 Bean
     * 用于密码加密和验证
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}