package com.crowdfund.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Maps /uploads/** URL paths to the local uploads directory.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${crowdfund.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String resourceLocation = "file:" + uploadPath.toString().replace('\\', '/') + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.web.servlet.ServletRegistrationBean<com.crowdfund.servlet.PaymentReceiptServlet> paymentReceiptServletRegistration(
            com.crowdfund.servlet.PaymentReceiptServlet paymentReceiptServlet) {
        org.springframework.boot.web.servlet.ServletRegistrationBean<com.crowdfund.servlet.PaymentReceiptServlet> registration =
                new org.springframework.boot.web.servlet.ServletRegistrationBean<>(paymentReceiptServlet, "/api/servlet/receipt");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
