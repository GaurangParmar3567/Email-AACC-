package com.example.mail.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.mail.model.DeleteAgentSessionKey;

@Repository
public interface DeleteAgentSessionKeyRepository extends JpaRepository<DeleteAgentSessionKey, String> {

    @Modifying
    @Query(value = "EXEC Usp_DeleteAvayaSession :avayaAgentID, :sessionKey", nativeQuery = true)
    void deleteAvayaSessionSp(
        @Param("avayaAgentID") String avayaAgentID, 
        @Param("sessionKey") String sessionKey
    );
}