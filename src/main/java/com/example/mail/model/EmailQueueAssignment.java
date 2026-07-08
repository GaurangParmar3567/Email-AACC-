package com.example.mail.model;

import lombok.*;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "email_queue_assignment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EmailQueueAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", unique = true, nullable = false)
    private Email email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private UserMaster assignedUser;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "priority_level", nullable = false)
    private String priorityLevel;

    private boolean isCompleted;

    @Temporal(TemporalType.TIMESTAMP)
    private Date assignedAt;
}