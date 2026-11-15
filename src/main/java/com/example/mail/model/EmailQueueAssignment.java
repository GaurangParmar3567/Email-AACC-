package com.example.mail.model;

import lombok.*;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "email_queue_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailQueueAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timing")
    private Date timing;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "assigned_at")
    private Date assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Email email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserMaster assignedUser;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "skillset_name", length = 100)
    private String skillsetName;

    @Column(name = "priority_level")
    private String priorityLevel;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "is_completed")
    private boolean isCompleted;
}