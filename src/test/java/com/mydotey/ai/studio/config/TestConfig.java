package com.mydotey.ai.studio.config;

import com.mydotey.ai.studio.filter.JwtAuthenticationFilter;
import com.mydotey.ai.studio.mapper.UserMapper;
import com.mydotey.ai.studio.util.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public JwtAuthenticationFilter testJwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        return new JwtAuthenticationFilter(jwtUtil, userMapper) {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                           jakarta.servlet.http.HttpServletResponse response,
                                           jakarta.servlet.FilterChain chain) {
                try {
                    chain.doFilter(request, response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
