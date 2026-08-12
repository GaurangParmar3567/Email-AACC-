package com.example.mail.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "closedreasons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosedReason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "DeletionTimeStamp")
    private LocalDateTime deletionTimeStamp;

    @Column(name = "MarkAsDeleted")
    private Boolean markAsDeleted;

    @Column(name = "Name", length = 255)
    private String name;

    @Column(name = "OldCodeMappingID")
    private Integer oldCodeMappingID;

    @Column(name = "Type")
    private Integer type;
}
