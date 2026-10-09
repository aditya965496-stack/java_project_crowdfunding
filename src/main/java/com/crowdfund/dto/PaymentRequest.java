package com.crowdfund.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private Long campaignId;
    private BigDecimal amount;
    private String paymentMethod;
    private String idempotencyKey;

    public PaymentRequest() {
    }

    public PaymentRequest(Long campaignId, BigDecimal amount, String paymentMethod, String idempotencyKey) {
        this.campaignId = campaignId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
