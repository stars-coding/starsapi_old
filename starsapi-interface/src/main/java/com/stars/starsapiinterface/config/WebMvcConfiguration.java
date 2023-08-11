package com.stars.starsapiinterface.config;

import com.stars.starsapiinterface.interceptor.WebInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 网站MVC配置
 *
 * @author stars
 */

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    /**
     * 添加拦截器
     *
     * @param registry InterceptorRegistry对象，用于注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WebInterceptor()).addPathPatterns("/**");
    }
}
