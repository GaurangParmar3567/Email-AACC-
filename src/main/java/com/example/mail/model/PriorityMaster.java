package com.example.mail.model;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "priority_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PriorityMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String priorityLevel;

    @Column(name = "keywords", length = 1000)
    private String keywords;
}