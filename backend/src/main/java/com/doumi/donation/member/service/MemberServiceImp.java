package com.doumi.donation.member.service;

import com.doumi.donation.campaigns.model.dao.CampaignsDao;
import com.doumi.donation.campaigns.model.dto.Campaign;
import com.doumi.donation.campaigns.model.dto.DonorRefundDto;
import com.doumi.donation.chatbot.service.CampaignIndexService;
import com.doumi.donation.exception.DuplicateResourceException;
import com.doumi.donation.exception.ResourceNotFoundException;
import com.doumi.donation.member.model.dao.MemberDao;
import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.member.model.dto.MemberJoinRequest;
import com.doumi.donation.member.model.dto.MemberPageResponse;
import com.doumi.donation.member.model.dto.MemberUpdateRequest;
import com.doumi.donation.member.model.dto.MyDonationDto;
import com.doumi.donation.notification.model.dao.NotificationDao;
import com.doumi.donation.point.service.PointService;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MemberServiceImp implements MemberService {

    private static final String ORG_REG_NO_PATTERN = "\\d{3}-\\d{2}-\\d{5}";

    private final MemberDao memberDao;
    private final BCryptPasswordEncoder passwordEncoder;
    private final NotificationDao notificationDao;
    private final CampaignsDao campaignsDao;
    private final PointService pointService;
    private final CampaignIndexService campaignIndexService;
    private final EmailVerificationService emailVerificationService;

    public MemberServiceImp(MemberDao memberDao, BCryptPasswordEncoder passwordEncoder,
                            NotificationDao notificationDao, CampaignsDao campaignsDao,
                            PointService pointService, CampaignIndexService campaignIndexService,
                            EmailVerificationService emailVerificationService) {
        this.memberDao = memberDao;
        this.passwordEncoder = passwordEncoder;
        this.notificationDao = notificationDao;
        this.campaignsDao = campaignsDao;
        this.pointService = pointService;
        this.campaignIndexService = campaignIndexService;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    @Transactional
    public void join(MemberJoinRequest req) {
        if (memberDao.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("이미 사용 중인 이메일입니다.");
        }

        // 이메일 인증을 완료한 주소만 가입 허용 (인증코드 발급/검증은 /api/v1/auth/email/* 에서 처리)
        if (!emailVerificationService.isSignupVerified(req.getEmail())) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해 주세요.");
        }

        Member member = new Member();
        member.setEmail(req.getEmail());
        member.setPassword(passwordEncoder.encode(req.getPassword()));
        member.setName(req.getName());
        member.setMemberType(req.getMemberType());

        // 단체 회원은 고유번호 필수 + 관리자 승인 전까지 PENDING 상태
        if ("ORGANIZATION".equals(req.getMemberType())) {
            if (req.getOrgRegNo() == null || !req.getOrgRegNo().matches(ORG_REG_NO_PATTERN)) {
                throw new IllegalArgumentException("단체 고유번호를 123-45-67890 형식으로 입력해 주세요.");
            }
            member.setOrgRegNo(req.getOrgRegNo());
            member.setOrgStatus("PENDING");
        }

        memberDao.insertMember(member);

        // 가입 완료 후 인증 플래그 정리 (재사용 방지)
        emailVerificationService.clearSignupVerified(req.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberDao.existsByEmail(email);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        Member member = memberDao.findByEmail(email);
        if (member == null || member.getDeletedAt() != null) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }
        memberDao.updatePassword(email, passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void delete(String email) {
        Member member = memberDao.findByEmail(email);
        if (member == null || member.getDeletedAt() != null) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }
        // 단체 회원이면 소유 캠페인 정리(기부 환불 + 알림 + 캠페인 삭제) 후 익명화
        withdrawOwnedCampaigns(member);
        memberDao.deleteByEmail(email);
    }

    @Override
    @Transactional
    public void update(String email, MemberUpdateRequest req) {
        if (!memberDao.existsByEmail(email)) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }

        // 비밀번호가 있으면 암호화
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            req.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        memberDao.updateMember(email, req);
    }

    @Override
    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        Member member = memberDao.findByEmail(email);
        if (member == null) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }
        member.setPassword(null);
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public Member findByMemberId(long memberId) {
        Member member = memberDao.findByMemberId(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }
        member.setPassword(null);
        return member;
    }

    @Override
    @Transactional
    public Member findOrCreateKakaoMember(long kakaoId, String nickname) {
        // 카카오는 이메일 권한이 없어 고유 id 기반 내부 식별용 이메일을 합성한다.
        String email = "kakao_" + kakaoId + "@kakao.local";
        Member member = memberDao.findByEmail(email);
        if (member == null) {
            member = new Member();
            member.setEmail(email);
            // 카카오 전용 계정이라 직접 로그인은 막되, NOT NULL 제약을 위해 임의 비밀번호를 넣는다.
            member.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            member.setName(nickname == null || nickname.isBlank() ? ("카카오회원" + kakaoId) : nickname);
            member.setMemberType("INDIVIDUAL");
            memberDao.insertMember(member);
            member = memberDao.findByEmail(email); // 생성된 memberId 포함해 다시 조회
        }
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyDonationDto> findDonationsByEmail(String email) {
        return memberDao.findDonationsByEmail(email);
    }

    @Override
    @Transactional
    public void updateRefreshToken(String email, String refresh) {
        memberDao.updateRefreshToken(email, refresh);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> getOrganizations(String status) {
        List<Member> organizations = memberDao.findOrganizationsByStatus(status);
        organizations.forEach(m -> m.setPassword(null));
        return organizations;
    }

    @Override
    @Transactional
    public void approveOrganization(long memberId) {
        int updated = memberDao.updateOrgStatus(memberId, "APPROVED", null);
        if (updated == 0) {
            throw new ResourceNotFoundException("존재하지 않는 단체 회원입니다.");
        }
        notify(memberId, "ORG_APPROVED", "단체 승인이 완료되었습니다. 이제 캠페인을 등록할 수 있어요!", "/campaigns/register");
    }

    @Override
    @Transactional
    public void rejectOrganization(long memberId, String reason) {
        int updated = memberDao.updateOrgStatus(memberId, "REJECTED", reason);
        if (updated == 0) {
            throw new ResourceNotFoundException("존재하지 않는 단체 회원입니다.");
        }
        String content = "단체 승인이 거절되었습니다."
                + (reason != null && !reason.isBlank() ? " 사유: " + reason : "");
        notify(memberId, "ORG_REJECTED", content, "/mypage");
    }

    @Override
    @Transactional(readOnly = true)
    public MemberPageResponse getAllMembers(String type, String keyword, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;

        long total = memberDao.countMembers(type, keyword);
        List<Member> members = memberDao.findAllMembers(type, keyword, offset, size);
        members.forEach(m -> m.setPassword(null)); // 방어적: 비밀번호 노출 차단

        int totalPages = (int) Math.ceil((double) total / size);
        return MemberPageResponse.builder()
                .members(members)
                .currentPage(page)
                .totalPages(totalPages)
                .totalElements(total)
                .build();
    }

    @Override
    @Transactional
    public void forceWithdraw(long memberId) {
        Member member = memberDao.findByMemberId(memberId);
        if (member == null) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다.");
        }
        if ("ADMIN".equals(member.getMemberType())) {
            throw new IllegalArgumentException("관리자 계정은 강제 탈퇴할 수 없습니다.");
        }
        if (member.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }
        // 본인 탈퇴와 동일한 경로 — 소유 캠페인 정리(환불+알림+삭제) 후 익명화
        withdrawOwnedCampaigns(member);
        memberDao.deleteByEmail(member.getEmail());
    }

    /**
     * 단체 회원 탈퇴 시 소유 캠페인을 정리한다.
     * <ol>
     *   <li>'사용완료'가 아닌 캠페인의 기부자에게 포인트 환불 + 알림 (이미 집행된 캠페인은 환불 제외)</li>
     *   <li>소유 캠페인의 기부내역 → 캠페인 순으로 삭제 (FK 순서)</li>
     *   <li>삭제된 캠페인을 챗봇 벡터 색인에서도 제거 (실패해도 탈퇴는 진행)</li>
     * </ol>
     * 개인 회원은 소유 캠페인이 없으므로 아무 동작도 하지 않는다.
     */
    private void withdrawOwnedCampaigns(Member member) {
        if (!"ORGANIZATION".equals(member.getMemberType())) {
            return;
        }
        long ownerId = member.getMemberId();

        // 삭제 전 벡터 제거 대상 id 확보
        List<Campaign> owned = campaignsDao.getCampaignsByOwner(ownerId);

        // 1. 환불 + 알림 (사용완료 캠페인 제외)
        List<DonorRefundDto> refunds = campaignsDao.sumRefundableDonationsByOwner(ownerId);
        for (DonorRefundDto r : refunds) {
            pointService.refundToMember(r.getMemberId(), r.getAmount());
            String content = "기부하신 '" + r.getCampaignTitle()
                    + "' 캠페인이 단체 탈퇴로 종료되어 " + r.getAmount() + "P가 환불되었습니다.";
            try {
                notificationDao.insertNotification(r.getMemberId(), "DONATION_REFUNDED", content, "/mypage");
            } catch (Exception e) {
                log.error("환불 알림 생성 실패: memberId={}", r.getMemberId(), e);
            }
        }

        // 2. 기부내역 → 캠페인 순 삭제 (DONATION이 campaign을 FK로 참조하므로 순서 중요)
        campaignsDao.deleteDonationsByOwner(ownerId);
        campaignsDao.deleteCampaignsByOwner(ownerId);

        // 3. 벡터 색인 제거 (Redis 장애 등으로 실패해도 탈퇴 자체는 막지 않음)
        for (Campaign c : owned) {
            try {
                campaignIndexService.removeCampaign(c.getCampaignId());
            } catch (Exception e) {
                log.error("벡터 색인 제거 실패: campaignId={}", c.getCampaignId(), e);
            }
        }
    }

    // 알림 실패가 승인/거절 처리 자체를 실패시키지 않도록 격리
    private void notify(long memberId, String type, String content, String linkUrl) {
        try {
            notificationDao.insertNotification(memberId, type, content, linkUrl);
        } catch (Exception e) {
            log.error("단체 승인 알림 생성 실패: memberId={}", memberId, e);
        }
    }
}
