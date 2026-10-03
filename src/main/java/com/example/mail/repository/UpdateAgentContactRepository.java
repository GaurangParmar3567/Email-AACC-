package com.example.mail.repository;

import com.example.mail.model.UpdateAgentContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UpdateAgentContactRepository extends JpaRepository<UpdateAgentContact, String> {

    @Modifying
    @Query(value = "EXEC Usp_UpdateAgentContact :agentID", nativeQuery = true)
    void updateAgentContactSp(@Param("agentID") String agentID);
}
