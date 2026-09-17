package com.mdyaipay.user.dao.merchant;

import com.mdyaipay.user.dao.merchant.mybatis.mapper.MerchantMemberMapper;
import com.mdyaipay.user.dao.merchant.mybatis.row.MerchantMemberRow;
import com.mdyaipay.user.domain.merchant.MerchantMember;
import com.mdyaipay.user.domain.merchant.MerchantMemberRepository;
import com.mdyaipay.user.domain.merchant.MerchantMemberRole;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/** 成员表 {@code merchant_member} MyBatis 仓储。 */
@Repository
public class MyBatisMerchantMemberRepository implements MerchantMemberRepository {

    private final MerchantMemberMapper memberMapper;

    public MyBatisMerchantMemberRepository(MerchantMemberMapper memberMapper) {
        this.memberMapper = Objects.requireNonNull(memberMapper);
    }

    @Override
    public MerchantMember save(MerchantMember member) {
        memberMapper.upsert(new MerchantMemberRow(
                member.getMemberId(),
                member.getMerchantId(),
                member.getUserId(),
                member.getRole().name(),
                member.getCreatedAt()));
        return member;
    }

    @Override
    public Optional<MerchantMember> findByMerchantIdAndUserId(long merchantId, long userId) {
        MerchantMemberRow row = memberMapper.findByMerchantIdAndUserId(merchantId, userId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new MerchantMember(
                row.memberId(),
                row.merchantId(),
                row.userId(),
                MerchantMemberRole.valueOf(row.role())));
    }
}
