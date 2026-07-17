package com.doumi.donation.api;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

/**
 * 컨트롤러 테스트 공용 헬퍼.
 *
 * <p>실제 JWT 필터 대신 SecurityContext에 인증 주체(memberId)를 직접 넣어
 * {@code @AuthenticationPrincipal Long memberId}를 채운다.
 * 사용한 테스트는 @AfterEach에서 {@link SecurityContextHolder#clearContext()} 호출.</p>
 */
public final class ApiTestSupport {

    private ApiTestSupport() {
    }

    /** memberId=1 로 로그인한 척: mvc.perform(post(...).with(로그인())) */
    public static RequestPostProcessor 로그인() {
        return 로그인(1L);
    }

    /** 특정 memberId 로 로그인한 척 */
    public static RequestPostProcessor 로그인(long memberId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(memberId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_INDIVIDUAL"))));
            return request;
        };
    }
}
