package com.example.demo.config;

import com.example.demo.security.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/api/auth/login", "/api/auth/register").permitAll();
                    authorize.requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers("/api/roles").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers("/api/users/*/toggle-status").hasAuthority("ROLE_VETERINARIO");
                    authorize.requestMatchers(HttpMethod.GET, "/api/users/*").hasAnyAuthority("ROLE_VETERINARIO", "ROLE_RECEPCIONISTA");
                    authorize.requestMatchers("/api/users/*/notes").hasAnyAuthority("ROLE_VETERINARIO", "ROLE_RECEPCIONISTA");
                    authorize.requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated();
                    authorize.requestMatchers("/api/users/me/change-password").authenticated();
                    authorize.anyRequest().authenticated();
                })
                .addFilterBefore(new FirebaseAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}





