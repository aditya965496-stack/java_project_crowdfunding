package com.crowdfund.config;

import com.crowdfund.security.RateLimitingFilter;
import com.crowdfund.security.UserDetailsServiceImpl;
import com.crowdfund.ui.common.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Hardened Security Configuration:
 * - Method-level security enabled (@PreAuthorize)
 * - Protected admin endpoints and views
 * - Webhook endpoint CSRF exemption and signature verification
 * - Rate limiting on public and API endpoints
 * - Frame options restricted to same origin
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends VaadinWebSecurity {

    private final UserDetailsServiceImpl userDetailsService;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          RateLimitingFilter rateLimitingFilter) {
        this.userDetailsService = userDetailsService;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Exempt webhooks and servlet endpoints from CSRF
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/webhooks/**", "/api/servlet/**", "/h2-console/**"));

        // Matchers for public APIs, servlets, uploads, and dev console
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/api/servlet/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
        );

        // Add rate-limiting filter
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        // Security headers
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(content -> {})
        );

        super.configure(http);
        setLoginView(http, LoginView.class);
    }
}
