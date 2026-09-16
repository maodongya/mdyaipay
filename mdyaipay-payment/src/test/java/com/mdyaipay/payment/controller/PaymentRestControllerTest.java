package com.mdyaipay.payment.controller;

import com.mdyaipay.payment.service.collect.PaymentApplicationService;
import com.mdyaipay.payment.domain.collect.PaymentOrder;
import com.mdyaipay.payment.domain.collect.PaymentProductType;
import com.mdyaipay.payment.domain.collect.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentRestController.class)
class PaymentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentApplicationService paymentService;

    @Test
    void collectReturnsOrderJson() throws Exception {
        PaymentOrder order = new PaymentOrder("O-1", 100L, "MOCK", PaymentProductType.QUICK_COLLECTION);
        order.markProcessing();
        order.markSuccess();
        when(paymentService.createAndPay(eq("O-1"), eq(100L), eq("MOCK"), any())).thenReturn(order);

        mockMvc.perform(post("/api/v1/payments/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"O-1","amount":100,"channel":"MOCK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("O-1"))
                .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.name()));
    }
}
