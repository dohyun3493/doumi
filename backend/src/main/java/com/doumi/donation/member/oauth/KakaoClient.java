package com.doumi.donation.member.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 카카오 OAuth(Authorization Code) 연동 클라이언트.
 * 1) 인가코드 → 카카오 액세스토큰 교환 (kauth.kakao.com)
 * 2) 액세스토큰 → 카카오 사용자 정보 조회 (kapi.kakao.com)
 *
 * 이메일은 비즈니스 검수가 필요해 받지 않고, 카카오 고유 id + 닉네임만 사용한다.
 */
@Component
public class KakaoClient {

    private final RestClient authClient = RestClient.create("https://kauth.kakao.com");
    private final RestClient apiClient = RestClient.create("https://kapi.kakao.com");

    private final String restApiKey;

    public KakaoClient(@Value("${kakao.rest-api-key}") String restApiKey) {
        this.restApiKey = restApiKey;
    }

    /** 인가코드로 카카오 액세스토큰을 발급받는다. redirectUri는 프론트가 인가요청에 쓴 값과 동일해야 한다. */
    public String getAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", restApiKey);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        JsonNode res = authClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (res == null || res.path("access_token").isMissingNode()) {
            throw new IllegalStateException("카카오 토큰 발급에 실패했습니다.");
        }
        return res.path("access_token").asText();
    }

    /** 카카오 액세스토큰으로 사용자 정보(고유 id, 닉네임)를 조회한다. */
    public KakaoUser getUser(String kakaoAccessToken) {
        JsonNode res = apiClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(JsonNode.class);

        if (res == null || res.path("id").isMissingNode()) {
            throw new IllegalStateException("카카오 사용자 정보를 가져오지 못했습니다.");
        }
        long id = res.path("id").asLong();
        String nickname = res.path("properties").path("nickname").asText("");
        return new KakaoUser(id, nickname);
    }

    /** 카카오 사용자 (고유 id + 닉네임) */
    public record KakaoUser(long id, String nickname) {}
}
