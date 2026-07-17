package com.doumi.donation.config.auth;

import com.doumi.donation.member.model.dto.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    public Member getMember() {
        return this.member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 권한 객체를 담을 표준 리스트 생성
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 회원의 memberType (INDIVIDUAL 또는 ORGANIZATION) 획득
        String memberType = member.getMemberType();

        // ROLE_ 접두사 추가
        String roleName = "ROLE_" + memberType;

        // 시큐리티 권한 객체를 생성하여 추가
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);
        authorities.add(authority);

        return authorities;
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
