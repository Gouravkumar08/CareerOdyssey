package com.outreach.repository;

import com.outreach.model.Outreach;
import com.outreach.model.OutreachStatus;
import com.outreach.model.RecipientType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class OutreachSpecifications {

    private OutreachSpecifications() {
    }

    public static Specification<Outreach> userIdEquals(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Outreach> companyIdEquals(Long companyId) {
        return (root, query, cb) -> companyId == null ? null : cb.equal(root.get("company").get("id"), companyId);
    }

    public static Specification<Outreach> statusEquals(OutreachStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Outreach> recipientTypeEquals(RecipientType recipientType) {
        return (root, query, cb) -> recipientType == null ? null : cb.equal(root.get("recipientType"), recipientType);
    }

    public static Specification<Outreach> replyReceivedEquals(Boolean replyReceived) {
        return (root, query, cb) -> replyReceived == null ? null : cb.equal(root.get("replyReceived"), replyReceived);
    }

    public static Specification<Outreach> sentAtFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("sentAt"), from);
    }

    public static Specification<Outreach> sentAtTo(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("sentAt"), to);
    }
}
