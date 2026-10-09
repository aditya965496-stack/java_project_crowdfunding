package com.crowdfund.service.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Encapsulates payment transaction parameters demonstrating OOP data encapsulation.
 */
public class PaymentContext {

    private final Long campaignId;
    private final Long contributorId;
    private final BigDecimal amount;
    private final String paymentMethod;
    private final String upiId;
    private final String utrNumber;
    private final String screenshotPath;
    private final Map<String, Object> metadata;

    public PaymentContext(Long campaignId, Long contributorId, BigDecimal amount,
                          String paymentMethod, String upiId, String utrNumber,
                          String screenshotPath, Map<String, Object> metadata) {
        this.campaignId = campaignId;
        this.contributorId = contributorId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.upiId = upiId;
        this.utrNumber = utrNumber;
        this.screenshotPath = screenshotPath;
        this.metadata = metadata;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getContributorId() {
        return contributorId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getUpiId() {
        return upiId;
    }

    public String getUtrNumber() {
        return utrNumber;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
