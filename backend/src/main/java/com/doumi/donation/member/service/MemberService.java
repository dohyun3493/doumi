package com.doumi.donation.member.service;

import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.member.model.dto.MemberJoinRequest;
import com.doumi.donation.member.model.dto.MemberPageResponse;
import com.doumi.donation.member.model.dto.MemberUpdateRequest;
import com.doumi.donation.member.model.dto.MyDonationDto;

import java.util.List;

public interface MemberService {
    void join(MemberJoinRequest req);
    void delete(String email);
    void update(String email, MemberUpdateRequest req);
    Member findByEmail(String email);
    Member findByMemberId(long memberId);

    /** 가입 여부 조회 (예외 없이 boolean 반환) — 인증코드 발송 전 중복/존재 확인용 */
    boolean existsByEmail(String email);

    /** 비밀번호 재설정: 새 비밀번호를 암호화해 저장한다. (이메일 인증코드 검증은 호출 측 책임) */
    void resetPassword(String email, String newPassword);

    /** 카카오 로그인: 카카오 고유 id로 회원을 찾고 없으면 자동 가입한 뒤 회원을 반환한다. */
    Member findOrCreateKakaoMember(long kakaoId, String nickname);

    List<MyDonationDto> findDonationsByEmail(String email);
    void updateRefreshToken(String email, String refresh);

    // 단체 승인 워크플로 (관리자)
    List<Member> getOrganizations(String status);
    void approveOrganization(long memberId);
    void rejectOrganization(long memberId, String reason);

    // 회원 관리 (관리자)
    MemberPageResponse getAllMembers(String type, String keyword, int page, int size);
    void forceWithdraw(long memberId);
}
