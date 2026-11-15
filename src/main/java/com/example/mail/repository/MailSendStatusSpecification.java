package com.example.mail.repository;

import com.example.mail.model.MailSendStatus;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MailSendStatusSpecification {

    public static Specification<MailSendStatus> withFilters(
            String contactId,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (contactId != null && !contactId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("contactId"), contactId.trim()));
            }

            // For Checker, date range uses updatedDate (with coalesce fallback to createdDate)
            Expression<LocalDateTime> effectiveDate = cb.coalesce(root.get("updatedDate"), root.get("createdDate"));

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(effectiveDate, fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(effectiveDate, toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

