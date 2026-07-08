package com.example.tt_backend.config;

// src/main/java/com/example/TT_BackEnd/config/WebConfig.java

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // ✅ Servir les fichiers statiques Angular
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
    }

    @Bean
    public FilterRegistrationBean<DirectoryBrowsingBlockFilter> directoryFilter() {
        FilterRegistrationBean<DirectoryBrowsingBlockFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new DirectoryBrowsingBlockFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1);
        return bean;
    }

    // ✅ Filtre qui bloque /vendor.js/ et tout accès "directory style"
    public static class DirectoryBrowsingBlockFilter implements Filter {

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            String uri = request.getRequestURI();

            // Bloquer les URLs type /fichier.js/ ou /fichier.js/quelquechose
            if (uri.matches(".*\\.(js|css|map|json|ts|html)/.*")
                    || uri.matches(".*\\.(js|css|map|json|ts|html)/")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
                return;
            }

            chain.doFilter(req, res);
        }
    }
}