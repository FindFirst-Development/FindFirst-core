package dev.findfirst.security.userauth.context;

import dev.findfirst.security.jwt.UserAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContext {

  public int getUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof UserAuthenticationToken uat) {
      return uat.getUserId();
    }
    throw new IllegalStateException("Unexpected authentication type: " + auth.getClass());
  }
}
