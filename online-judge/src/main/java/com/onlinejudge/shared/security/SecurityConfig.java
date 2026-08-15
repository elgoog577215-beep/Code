package com.onlinejudge.shared.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TeacherAuthFilter teacherAuthFilter,
                                            SchoolSecurityProperties properties) throws Exception {
        http.addFilterBefore(teacherAuthFilter, AuthorizationFilter.class)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        if (properties.schoolProfile()) {
            CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            repository.setCookiePath("/");
            http.csrf(csrf -> csrf.csrfTokenRepository(repository)
                    .ignoringRequestMatchers("/api/auth/teacher/register", "/api/auth/teacher/login",
                            "/api/auth/student/login", "/api/teacher/auth/**"));
        } else {
            http.csrf(csrf -> csrf.disable());
        }
        return http.build();
    }

    @Bean
    FilterRegistrationBean<TeacherAuthFilter> disableContainerTeacherFilter(TeacherAuthFilter filter) {
        FilterRegistrationBean<TeacherAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
