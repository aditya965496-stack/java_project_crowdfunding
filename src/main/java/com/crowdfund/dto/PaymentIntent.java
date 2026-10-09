package com.crowdfund.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentIntent {
    private String transactionId;
    private String clientSecret;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String idempotencyKey;
    private Instant createdAt;

    public PaymentIntent() {
    }

    public PaymentIntent(String transactionId, String clientSecret, BigDecimal amount,
                         String currency, String status, String idempotencyKey) {
        this.transactionId = transactionId;
        this.clientSecret = clientSecret;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
