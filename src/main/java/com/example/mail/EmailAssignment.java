package com.example.mail;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "email_assignments")
public class EmailAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    // Assuming you have an Agent or User ID in Service 2
    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "assigned_at")
    private Date assignedAt;

    // e.g., "ASSIGNED", "IN_PROGRESS", "TRANSFERRED", "RESOLVED"
    @Column(name = "status")
    private String status;

    public EmailAssignment() {
    }

    public EmailAssignment(Email email, Long agentId, Date assignedAt, String status) {
        this.email = email;
        this.agentId = agentId;
        this.assignedAt = assignedAt;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Email getEmail() { return email; }
    public void setEmail(Email email) { this.email = email; }

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }

    public Date getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Date assignedAt) { this.assignedAt = assignedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

