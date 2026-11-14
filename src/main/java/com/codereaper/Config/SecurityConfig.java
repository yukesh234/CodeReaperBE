package com.codereaper.Config;


import com.codereaper.security.GitHubOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GitHubOAuth2UserService gitHubOauth2UserService,
            AuthenticationSuccessHandler oauth2SuccessHandler,
            AuthenticationFailureHandler oauth2FailureHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login-url", "/oauth2/**", "/login/oauth2/**",
                                "/error", "/health").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.userService(gitHubOauth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("codereaper_session"))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(@Value("${app.frontend.url}") String frontendUrl) {
        var h = new SimpleUrlAuthenticationSuccessHandler(frontendUrl + "/auth/callback");
        h.setAlwaysUseDefaultTargetUrl(true);
        return h;
    }

    @Bean
    AuthenticationFailureHandler oauth2FailureHandler(@Value("${app.frontend.url}") String frontendUrl) {
        return new SimpleUrlAuthenticationFailureHandler(frontendUrl + "/login?error=oauth_failed");
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend.url}") String frontendUrl) {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(frontendUrl));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}