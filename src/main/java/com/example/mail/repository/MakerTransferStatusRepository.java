package com.example.mail.repository;

import com.example.mail.model.MakerTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

@Repository
public interface MakerTransferStatusRepository extends JpaRepository<MakerTransferStatus, Long> {

    Optional<MakerTransferStatus> findFirstByContactIdOrderByMailIdDesc(String contactId);

    @Procedure(name = "MakerTransferStatus.saveMakerTransferDetails")
    Map<String, Object> executeSaveMakerTransferDetails(
            @Param("FromEmail") String fromEmail,
            @Param("ToEmail") String toEmail,
            @Param("Subject") String subject,
            @Param("BodyContent") String bodyContent,
            @Param("ContactID") String contactId,
            @Param("AgentID") Long agentId,
            @Param("ClosedReason") String closedReason,
            @Param("Comment") String comment,
            @Param("ActionID") Long actionId,
            @Param("AnswerDT") Timestamp answerDateTime);
}
