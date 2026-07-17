package com.doumi.donation.config.auth;

import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberDao memberDao;

    public CustomUserDetailsService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // [중요] 패스워드 해시값이 들어있는 DB 정보를 가져오기 위해 DAO를 직접 호출합니다.
        Member member = memberDao.findByEmail(email);

        if (member == null) {
            throw new UsernameNotFoundException("가입되지 않은 이메일입니다: " + email);
        }

        CustomUserDetails userDetails = new CustomUserDetails(member);
        return userDetails;
    }
}
