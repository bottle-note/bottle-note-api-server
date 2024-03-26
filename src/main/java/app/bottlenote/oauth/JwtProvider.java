package main.java.app.bottlenote.oauth;

import java.util.Base64;

import javax.annotation.PostConstruct;

public class JwtProvider {

    private static String SECRET_KEY = "secretKeyDummy";
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 15;
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;
    public static final String KEY_ROLES = "roles";
    
}
