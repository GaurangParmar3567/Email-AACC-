package com.example.mail.repository;

import com.example.mail.model.UserMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMasterRepo extends JpaRepository<UserMaster, Long> {
}
