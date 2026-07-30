package com.example.mail.repository;

import com.example.mail.model.MakerTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MakerTransferStatusRepository extends JpaRepository<MakerTransferStatus, Long> {

    Optional<MakerTransferStatus> findFirstByContactIdOrderByMailIdDesc(String contactId);
}
