package com.example.demo.config;

import com.example.demo.security.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/api/auth/login", "/api/auth/register").permitAll();
                    authorize.requestMatchers(HttpMethod.POST, "/api/users/create-with-roles").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers("/api/roles").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers("/api/users/*/toggle-status").hasAuthority("ROLE_VETERINARIO");
                   authorize.requestMatchers(HttpMethod.GET, "/api/users/*").hasAnyAuthority("ROLE_VETERINARIO", "ROLE_RECEPCIONISTA");
                    authorize.requestMatchers("/api/users/*/notes").hasAnyAuthority("ROLE_VETERINARIO", "ROLE_RECEPCIONISTA");
                    authorize.requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated();
                    authorize.requestMatchers("/api/users/me/change-password").authenticated();
                    authorize.requestMatchers("/api/pets").authenticated();
                    authorize.anyRequest().authenticated();
                })
                .addFilterBefore(new FirebaseAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
    @Bean
    WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("http://localhost:3000");
            }
        };
    }

}





