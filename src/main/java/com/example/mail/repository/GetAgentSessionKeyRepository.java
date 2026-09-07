
package com.example.mail.repository;

import com.example.mail.model.GetAgentSessionKey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GetAgentSessionKeyRepository extends JpaRepository<GetAgentSessionKey, String> {

    @Modifying
    @Query(value = "EXEC Usp_GetAvayaSession :avayaAgentID, :sessionKey", nativeQuery = true)
    void getAgentSessionKeySp(
        @Param("avayaAgentID") String avayaAgentID, 
        @Param("sessionKey") String sessionKey
    );
}