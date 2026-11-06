package com.tuanistack.cranhua.config;

import com.tuanistack.cranhua.auth.CookieSesion;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.WebUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final PathPatternRequestMatcher.Builder RUTA =
      PathPatternRequestMatcher.withDefaults();

  private static final RequestMatcher RUTAS_PUBLICAS =
      new OrRequestMatcher(
          RUTA.matcher(HttpMethod.OPTIONS, "/**"),
          RUTA.matcher(HttpMethod.GET, "/actuator/health"),
          RUTA.matcher(HttpMethod.GET, "/actuator/health/**"),
          RUTA.matcher("/api/auth/**"),
          RUTA.matcher("/api/publico/**"));

  // Todo lo que no es público pide sesión; el rol de cada endpoint se valida con @PreAuthorize
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    HttpStatusEntryPoint responder401 = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    AccessDeniedHandler responder403 =
        (request, response, e) -> response.setStatus(HttpStatus.FORBIDDEN.value());
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(RUTAS_PUBLICAS).permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            jwt ->
                jwt.bearerTokenResolver(tokenDesdeCookie())
                    .authenticationEntryPoint(responder401)
                    .jwt(config -> config.jwtAuthenticationConverter(rolesDelToken())))
        .exceptionHandling(
            errores ->
                errores.authenticationEntryPoint(responder401).accessDeniedHandler(responder403));
    return http.build();
  }

  // En las rutas públicas la cookie se ignora: un token vencido no debe impedir volver a entrar
  private static BearerTokenResolver tokenDesdeCookie() {
    return request -> {
      if (RUTAS_PUBLICAS.matches(request)) {
        return null;
      }
      Cookie cookie = WebUtils.getCookie(request, CookieSesion.NOMBRE);
      return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    };
  }

  private static JwtAuthenticationConverter rolesDelToken() {
    JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName(CookieSesion.CLAIM_ROLES);
    roles.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
    convertidor.setJwtGrantedAuthoritiesConverter(roles);
    return convertidor;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
