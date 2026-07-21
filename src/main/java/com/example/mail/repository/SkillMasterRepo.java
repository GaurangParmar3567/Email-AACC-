package com.example.mail.repository;

import com.example.mail.model.SkillMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillMasterRepo extends JpaRepository<SkillMaster, Long> {
    SkillMaster findByName(String defaultSkillName);
}
