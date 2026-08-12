package com.example.mail.repository;

import com.example.mail.model.ContactAction;
import com.example.mail.dto.response.MakerMailActionDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ContactActionRepository extends JpaRepository<ContactAction, Long> {
    List<ContactAction> findByContactId(Long contactId);

    @Query("SELECT new com.example.mail.dto.response.MakerMailActionDTO("
            + "ca.creationTime, ca.mailFrom, ca.mailTo, ca.mailCc, ca.subject, ca.textHtml, ca.textContent) "
            + "FROM ContactAction ca WHERE ca.contactId = :contactId ORDER BY ca.id ASC")
    List<MakerMailActionDTO> findMakerMailFieldsByContactId(@Param("contactId") Long contactId);

    @Query("SELECT DISTINCT ca.closedReasonName, ca.closedReasonNumericValue "
            + "FROM ContactAction ca "
            + "WHERE ca.closedReasonName IS NOT NULL AND ca.closedReasonNumericValue IS NOT NULL")
    List<Object[]> findDistinctClosedReasonCodes();

}
