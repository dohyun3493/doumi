package com.doumi.donation.member.model.dao;

import com.doumi.donation.member.model.dto.Member;
import com.doumi.donation.member.model.dto.MyDonationDto;
import com.doumi.donation.member.model.dto.MemberUpdateRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberDao {
    boolean existsByEmail(@Param("email") String email);
    void insertMember(Member member);
    void deleteByEmail(@Param("email") String email);
    Member findByEmail(@Param("email") String email);
    Member findByMemberId(@Param("memberId") long memberId);
    void updateMember(@Param("email") String email, @Param("req") MemberUpdateRequest req);
    void updatePassword(@Param("email") String email, @Param("password") String password);
    void updatePointBalance(@Param("email") String email, @Param("delta") long delta);
    void updatePointBalanceById(@Param("memberId") long memberId, @Param("delta") long delta);
    int decreasePointIfEnough(@Param("memberId") long memberId, @Param("amount") long amount);
    List<MyDonationDto> findDonationsByEmail(@Param("email") String email);
    List<MyDonationDto> findDonationsByMemberId(@Param("memberId") long memberId);
    void updateRefreshToken(@Param("email") String email, @Param("refresh") String refresh);

    List<Member> findOrganizationsByStatus(@Param("status") String status);
    int updateOrgStatus(@Param("memberId") long memberId,
                        @Param("status") String status,
                        @Param("rejectReason") String rejectReason);

    // 관리자 회원 관리 (페이징)
    long countMembers(@Param("type") String type, @Param("keyword") String keyword);
    List<Member> findAllMembers(@Param("type") String type, @Param("keyword") String keyword,
                                @Param("offset") int offset, @Param("size") int size);
}
