package com.example.mail.repository;

import com.example.mail.model.UpdateAuxDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UpdateAuxDetailsRepository extends JpaRepository<UpdateAuxDetails, String> {

    @Modifying
    @Query(value = "EXEC USP_UpdateAuxDetails :agentID, :auxCode, :startTime, :endTime", nativeQuery = true)
    void updateAuxDetails(
        @Param("agentID") String agentID,
        @Param("auxCode") String auxCode,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );
}