package dev.findfirst;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

import dev.findfirst.security.util.KeyGenerator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication
@Slf4j
@EnableScheduling
public class FindFirstApplication {

  public static void main(String[] args) {
    try {
      KeyGenerator.generateKeys("app.key", "app.pub");
      SpringApplication.run(FindFirstApplication.class, args);
    } catch (ApplicationContextException | NoSuchAlgorithmException | IOException ude) {
      log.error("""


          ==============================================
          Check that the app.key, app.pub are generated.
          To generate call ./scripts/createServerKeys.sh
          """);
    }
  }

  // Fix the CORS errors
  @Bean
  public FilterRegistrationBean<CorsFilter> simpleCorsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    // *** URL below needs to match the Vue client URL and port ***
    // Local host and 127.0.0.1 are the same
    config.setAllowedOriginPatterns(Arrays.asList("https://localhost:3000", "http://localhost:3000",
        "https://findfirst.dev", "http://localhost", "http://127.0.0.1",
        "chrome-extension://*", "moz-extension://*"));
    config.setAllowedMethods(Collections.singletonList("*"));
    config.setAllowedHeaders(Collections.singletonList("*"));
    source.registerCorsConfiguration("/**", config);
    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
}
