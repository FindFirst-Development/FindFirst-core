package dev.findfirst.security.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

import dev.findfirst.security.conditions.OAuthClientsCondition;
import dev.findfirst.security.filters.CookieAuthenticationFilter;
import dev.findfirst.security.jwt.AuthEntryPointJwt;
import dev.findfirst.security.jwt.UserAuthenticationToken;
import dev.findfirst.security.oauth2client.handlers.Oauth2LoginSuccessHandler;
import dev.findfirst.security.userauth.service.UserDetailsServiceImpl;
import dev.findfirst.security.userauth.utils.Constants;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecSecurityConfig {

  @Value("${jwt.public.key}")
  private RSAPublicKey key;

  @Value("${jwt.private.key}")
  private RSAPrivateKey priv;

  private final UserDetailsServiceImpl userDetailsService;

  private final AuthEntryPointJwt unauthorizedHandler;

  private Oauth2LoginSuccessHandler oauth2Success;

  @Autowired(required = false)
  public void setOauth2(Oauth2LoginSuccessHandler oauth2LoginSuccessHandler) {
    this.oauth2Success = oauth2LoginSuccessHandler;
  }

  @Bean
  public CookieAuthenticationFilter cookieJWTAuthFilter() {
    return new CookieAuthenticationFilter();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());

    return authProvider;
  }

  @Bean
  @Order(1)
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.securityMatcher("/user/**", "/api/**")
        .authorizeHttpRequests(auth -> auth.requestMatchers("/").denyAll())
        .authorizeHttpRequests(authorize -> authorize.requestMatchers("/user/user-info")
            .authenticated().requestMatchers("/user/**").permitAll().anyRequest().authenticated());

    // stateless cookie app
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(
            rs -> rs.jwt(jwt -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(token -> {
              Number userId = token.getClaim(Constants.USER_ID_CLAIM);
              Number roleId = token.getClaim(Constants.ROLE_ID_CLAIM);
              String roleName = token.getClaim(Constants.ROLE_NAME_CLAIM);
              return new UserAuthenticationToken(token.getSubject(), roleId.intValue(),
                  Collections.singletonList(new SimpleGrantedAuthority(roleName)),
                  userId.intValue());
            })));

    http.httpBasic(
        httpBasicCustomizer -> httpBasicCustomizer.authenticationEntryPoint(unauthorizedHandler))

        // use this exeception only for /user/signin
        .exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(unauthorizedHandler,
                new AntPathRequestMatcher("/user/signin"))
            .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))

        .authenticationProvider(authenticationProvider())

        // filters
        .addFilterBefore(cookieJWTAuthFilter(), UsernamePasswordAuthenticationFilter.class);

    // wrap it all up.
    return http.build();
  }

  @Bean
  @Order(2)
  @Conditional(OAuthClientsCondition.class)
  public SecurityFilterChain oauth2ClientsFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/oauth2/**", "/login/**", "/error/**", "/*") // Apply only for OAuth paths
        .oauth2Login(oauth -> oauth.successHandler(oauth2Success));
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(this.key).build();
  }

  @Bean
  JwtEncoder jwtEncoder() {
    JWK jwk = new RSAKey.Builder(this.key).privateKey(this.priv).build();
    JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jwks);
  }
}
