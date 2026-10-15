package com.farm.portal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/error").permitAll()
                .requestMatchers("/dashboard").hasRole("ADMIN")
                .requestMatchers("/produce/new", "/produce/*/edit").hasAnyRole("FARMER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/produce").hasAnyRole("FARMER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/produce/*/advance").hasAnyRole("DISTRIBUTOR", "RETAILER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/produce/*/status").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/produce", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Demo users kept in memory (passwords are stored as BCrypt hashes)
    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("farmer").password(encoder.encode("farmer123")).roles("FARMER").build(),
            User.withUsername("distributor").password(encoder.encode("distributor123")).roles("DISTRIBUTOR").build(),
            User.withUsername("retailer").password(encoder.encode("retailer123")).roles("RETAILER").build(),
            User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build());
    }
}
