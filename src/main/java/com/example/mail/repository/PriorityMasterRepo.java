package com.example.mail.repository;

import com.example.mail.model.PriorityMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriorityMasterRepo extends JpaRepository<PriorityMaster, Long> {
}
