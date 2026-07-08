package com.example.tt_backend.config;

import com.example.tt_backend.service.UserDetailsServiceImpl;
import com.example.tt_backend.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ✅ S6813 + S3305 — Injection par constructeur, pas @Autowired
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins; // ✅ Injecté, plus d'IP hardcodée

    public SecurityConfig(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ✅ S1130 + S112 — @SuppressWarnings justifié : signature imposée par Spring
    @Bean
    @SuppressWarnings({"java:S112", "java:S1130"})
    // getAuthenticationManager() declares throws Exception — imposed by Spring, unavoidable
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ✅ S3305 — jwtUtils passé directement ici
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils, userDetailsService);
    }

    // ✅ S1130 + S112 — @SuppressWarnings justifié : signature imposée par Spring Security
    @Bean
    @SuppressWarnings({"java:S112", "java:S1130","java:S4502"})// CSRF disabled — stateless JWT API, no session cookies
    // HttpSecurity.build() declares throws Exception — imposed by Spring Security, unavoidable
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
						"img-src 'self' data: blob: http://172.20.36.132:8080 http://172.20.80.148 https://saisonnier.tunisietelecom.tn; " +
                                                "font-src 'self'; " +
						"connect-src 'self' " + String.join(" ", allowedOrigins) + "; " +
                                                "frame-ancestors 'none'; " +
                                                "form-action 'self';"
                                )
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
        			"/auth/**",
				"/api/campagnes/*/publique",
				"/api/structures/campagne/*/publique",
        			"/api/candidatures/parent-by-matricule",
        			"/api/candidatures/depot",
        			"/files/**",
        			"/",
        			"/index.html",
        			"/*.js",
        			"/*.css",
        			"/assets/**",
        			"/favicon.ico",
        			"/*.ico",
        			"/login",
        			"/login/**",
        			"/responsable",
        			"/responsable/**",
        			"/rhregioanl",
        			"/rhregioanl/**",
        			"/superadmin",
        			"/superadmin/dashboard",
        			"/superadmin/user_list",
        			"/home-ge",
        			"/home-ge/**",
        			"/saisonnier/**",
        			"/admin",
        			"/admin/**",
        			"/espace-saisonnier",
        			"/espace-saisonnier/**",
        			"/campagne-expiree",
        			"/campagne-expiree/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/campagnes/**"
                        ).hasAnyAuthority("ADMIN", "SUPERADMIN", "RH_REGIONAL")
                        .requestMatchers(
                                "/api/candidatures/**",
                                "/api/affectations/**",
                                "/api/saisonniers/**",
                                "/api/parents/**",
                                "/api/documents/**",
                                "/api/structures/**",
                                "/v3/api-docs/**",
                                "/structures/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(
                        jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // JWT Filter Class
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtUtils jwtUtils;
        private final UserDetailsService userDetailsService;

        public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
            this.jwtUtils = jwtUtils;
            this.userDetailsService = userDetailsService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (jwtUtils.validateToken(token)) {
                    String email = jwtUtils.getUsernameFromToken(token);
                    var userDetails = userDetailsService.loadUserByUsername(email);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        }
    }
}
