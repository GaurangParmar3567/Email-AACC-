package com.example.mail.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactState {

    @Id
    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "skill_name")
    private String skillName;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // e.g., "OPEN", "MAKER_REVIEW", "CHECKER_REVIEW", "CLOSED"

    @Column(name = "is_assigned")
    private boolean assigned;

    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;

    @Column(name = "assigned_time")
    private LocalDateTime assignedTime;

    @Column(name = "is_responded")
    private boolean responded;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}