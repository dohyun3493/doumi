package com.doumi.donation.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.doumi.donation.config.auth.CustomUserDetails;
import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.member.model.dto.MemberLoginRequest;
import com.doumi.donation.member.service.MemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JWTUtil jwtUtil;
    private final MemberService memberService;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, MemberService memberService) {
        setAuthenticationManager(authenticationManager);
        this.jwtUtil = jwtUtil;
        this.memberService = memberService;
        
        // 로그인 인증을 시도할 주소를 지정합니다.
        setFilterProcessesUrl("/api/v1/auth/login");
    }

    // 1. 로그인 데이터를 받아 인증 매니저에게 처리를 전가합니다.
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            // JSON 변환기 생성
            ObjectMapper objectMapper = new ObjectMapper();

            // Request의 InputStream에서 데이터를 꺼내 로그인 DTO로 매핑
            MemberLoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), MemberLoginRequest.class);

            // 아이디(이메일)와 패스워드 추출
            String email = loginRequest.getEmail();
            String password = loginRequest.getPassword();

            // 시큐리티용 임시 인증 토큰 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);

            // 시큐리티 인증 매니저에게 로그인 검사 대리 수행 요청
            Authentication authenticationResult = getAuthenticationManager().authenticate(authToken);
            return authenticationResult;

        } catch (IOException e) {
            throw new RuntimeException("로그인 데이터 분석 중 에러 발생", e);
        }
    }

    // 2. 인증 성공 시 토큰을 생성하여 JSON 응답을 전송합니다.
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        Map<String, String> result = new HashMap<>();

        // 1. 인증이 끝난 유저 정보(Principal) 획득
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
        Member member = userDetails.getMember();

        // 2. Access Token 생성 및 결과 Map에 삽입
        String accessToken = jwtUtil.createAccessToken(member);
        result.put("accessToken", accessToken);

        // 3. Refresh Token 생성
        String refreshToken = jwtUtil.createRefreshToken(member);

        // 4. Member DTO에 리프레시 토큰 주입 및 DB 저장
        member.setRefresh(refreshToken);
        memberService.updateRefreshToken(member.getEmail(), refreshToken);
        result.put("refreshToken", refreshToken);

        // 5. 결과 전송 도우미 메소드 호출
        handleResult(response, result, HttpStatus.OK);
    }

    // 3. 인증 실패 시 401 Unauthorized 에러 출력
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", "이메일 또는 비밀번호가 올바르지 않습니다.");

        ObjectMapper mapper = new ObjectMapper();
        String jsonResult = mapper.writeValueAsString(errorData);

        PrintWriter writer = response.getWriter();
        writer.write(jsonResult);
        writer.flush();
    }

    // 결과 데이터를 클라이언트에게 직접 JSON으로 전송하는 도우미 메소드
    private void handleResult(HttpServletResponse response, Map<String, String> result, HttpStatus status) 
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectMapper mapper = new ObjectMapper();
        String jsonResult = mapper.writeValueAsString(result);

        PrintWriter writer = response.getWriter();
        writer.write(jsonResult);
        writer.flush();
    }
}
