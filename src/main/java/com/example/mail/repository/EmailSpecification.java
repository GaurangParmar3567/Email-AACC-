package com.example.mail.repository;

import com.example.mail.dto.request.EmailFilterRequestDTO;
import com.example.mail.model.Email;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EmailSpecification {

    public static Specification<Email> withFilter(EmailFilterRequestDTO filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getContactId() != null) {
                predicates.add(cb.equal(root.get("contactId"), filter.getContactId()));
            }

            if (filter.getAgentId() != null) {
                predicates.add(cb.equal(root.get("agentId"), filter.getAgentId()));
            }

            if (filter.getSkillId() != null) {
                predicates.add(cb.equal(root.get("skillId"), filter.getSkillId()));
            }

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), filter.getStatus().trim().toLowerCase()));
            }

            if (filter.getSkillsetName() != null && !filter.getSkillsetName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("skillsetName")), "%" + filter.getSkillsetName().trim().toLowerCase() + "%"));
            }

            if (filter.getSender() != null && !filter.getSender().trim().isEmpty()) {
                String senderTerm = "%" + filter.getSender().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("sender")), senderTerm),
                        cb.like(cb.lower(root.get("mailFrom")), senderTerm)
                ));
            }

            if (filter.getRecipient() != null && !filter.getRecipient().trim().isEmpty()) {
                String recipientTerm = "%" + filter.getRecipient().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("recipient")), recipientTerm),
                        cb.like(cb.lower(root.get("mailTo")), recipientTerm)
                ));
            }

            if (filter.getSubject() != null && !filter.getSubject().trim().isEmpty()) {
                String subjectTerm = "%" + filter.getSubject().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("subject")), subjectTerm),
                        cb.like(cb.lower(root.get("originalSubject")), subjectTerm)
                ));
            }

            if (filter.getAssigned() != null) {
                predicates.add(cb.equal(root.get("assigned"), filter.getAssigned()));
            }

            if (filter.getResponded() != null) {
                predicates.add(cb.equal(root.get("responded"), filter.getResponded()));
            }

            if (filter.getRepeatFlag() != null) {
                predicates.add(cb.equal(root.get("repeatFlag"), filter.getRepeatFlag()));
            }

            if (filter.getSource() != null && !filter.getSource().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("source")), filter.getSource().trim().toLowerCase()));
            }

            if (filter.getPriority() != null && !filter.getPriority().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("priority")), filter.getPriority().trim().toLowerCase()));
            }

            if (filter.getPriorityId() != null) {
                predicates.add(cb.equal(root.get("priorityId"), filter.getPriorityId()));
            }

            Date start = filter.resolveStartDate();
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receivedDate"), start));
            }

            Date end = filter.resolveEndDate();
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receivedDate"), end));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

