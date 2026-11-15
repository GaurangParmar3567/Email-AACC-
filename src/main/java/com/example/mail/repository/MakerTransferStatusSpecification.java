package com.example.mail.repository;

import com.example.mail.model.MakerTransferStatus;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MakerTransferStatusSpecification {

    public static Specification<MakerTransferStatus> withFilters(
            String contactId,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (contactId != null && !contactId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("contactId"), contactId.trim()));
            }

            // For Maker, date range uses createdDate
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

