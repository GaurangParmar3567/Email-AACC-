package com.example.mail.model;

import lombok.Data;

import javax.persistence.*;

import java.util.Arrays;

@Entity
@Table(name = "attachments")
@Data
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String mimeType;

    @Lob
//    @Column(columnDefinition="LONGBLOB")
    @Column(columnDefinition="VARBINARY(MAX)")
    private byte[] fileData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @Column(name = "internal_path")
    private String internalPath;

    @Column(name = "display_name")
    private String displayName;

    @ManyToOne
    @JoinColumn(name = "contact_action_id")
    private ContactAction contactAction;
}
