package com.crowdfund.dto;

import java.math.BigDecimal;

public class PaymentWebhookPayload {
    private String eventId;
    private String eventType; // "payment.succeeded" or "payment.failed"
    private EventData data;

    public static class EventData {
        private Long campaignId;
        private String contributorEmail;
        private BigDecimal amount;
        private String paymentMethod;
        private String transactionId;
        private String idempotencyKey;

        public Long getCampaignId() {
            return campaignId;
        }

        public void setCampaignId(Long campaignId) {
            this.campaignId = campaignId;
        }

        public String getContributorEmail() {
            return contributorEmail;
        }

        public void setContributorEmail(String contributorEmail) {
            this.contributorEmail = contributorEmail;
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

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public EventData getData() {
        return data;
    }

    public void setData(EventData data) {
        this.data = data;
    }
}
