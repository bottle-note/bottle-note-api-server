package main.java.app.bottlenote.oauth;

public class JwtDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class JwtDto {
        private String accessToken;
        private String refreshToken;
    }
}
