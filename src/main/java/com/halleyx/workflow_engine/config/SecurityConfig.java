package com.halleyx.workflow_engine.config;

import com.halleyx.workflow_engine.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;

@Configuration
@EnableWebSecurity
//security filter chain is used to manage authorization for api
// password encoder for hash the password
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/users/change-password").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/users/*/toggle-active").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/users").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/workflows/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/workflows/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/workflows/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/workflows/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/steps/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/steps/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/steps/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/steps/**").hasRole("ADMIN")

                        .requestMatchers("/api/rules/**").hasRole("ADMIN")
                        .requestMatchers("/api/executions/**").authenticated()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
                )
                .securityContext(ctx -> ctx
                        .securityContextRepository(securityContextRepository())
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
