package app.bottlenote.user.fake;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class FakeBCryptPasswordEncoder extends BCryptPasswordEncoder {

  @Override
  public String encode(CharSequence rawPassword) {
    return "fake-encoded-" + rawPassword;
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return ("fake-encoded-" + rawPassword).equals(encodedPassword);
  }
}
