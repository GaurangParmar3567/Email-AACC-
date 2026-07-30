package com.example.mail.model;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "skill_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SkillMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., "EM_CHK_CHECKER", "CARDS_DEPT"

    @Column(name = "keywords", length = 1000)
    private String keywords;
}