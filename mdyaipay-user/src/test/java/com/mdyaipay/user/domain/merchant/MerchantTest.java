package com.mdyaipay.user.domain.merchant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MerchantTest {

    @Test
    void shouldMoveFromDraftToPendingOnSubmitAudit() {
        Merchant merchant = new Merchant(1L, "demo");
        merchant.submitAudit();
        Assertions.assertEquals(MerchantStatus.PENDING, merchant.getStatus());
    }

    @Test
    void shouldRejectIllegalApproveFromDraft() {
        Merchant merchant = new Merchant(1L, "demo");
        Assertions.assertThrows(IllegalStateException.class, merchant::approve);
    }
}
