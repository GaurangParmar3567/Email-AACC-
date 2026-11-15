package com.example.mail.service;

import com.example.mail.dto.soap.aacc.UnableToReadContactRequest;
import com.example.mail.dto.soap.aacc.UnableToReadContactResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;

@Service
public class UnableToReadContactService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${db.sp.unable-to-read-contact:Usp_UnableToReadContact}")
    private String spUnableToReadContact;

    @Transactional
    public UnableToReadContactResponse unableToReadContact(UnableToReadContactRequest request) {
        UnableToReadContactResponse objResult = new UnableToReadContactResponse();
        objResult.setMessage("Bad Request");

        if (request != null && isNotEmpty(request.getStrAgentID()) && isNotEmpty(request.getStrContactID())) {
            log.info("unableToReadContact() - Inside UnableToReadContact for AgentID: {}, ContactID: {}",
                    request.getStrAgentID(), request.getStrContactID());
            objResult.setMessage("Valid Input");
            objResult.setContactResultID(-1);

            try {
                Long contactIdLong = null;
                try {
                    contactIdLong = Long.parseLong(request.getStrContactID().trim());
                } catch (Exception ex) {
                    log.debug("unableToReadContact() - ContactID parse as Long: {}", ex.getMessage());
                }

                log.info("unableToReadContact() - Executing procedure {}: AgentID: {}, ContactID: {}, Error: {}",
                        spUnableToReadContact, request.getStrAgentID(), request.getStrContactID(), request.getStrError());

                try {
                    SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                            .withProcedureName(spUnableToReadContact)
                            .declareParameters(
                                    new SqlParameter("AgentID", Types.VARCHAR),
                                    new SqlParameter("ContactID", Types.BIGINT),
                                    new SqlParameter("Error", Types.VARCHAR)
                            );

                    MapSqlParameterSource inParams = new MapSqlParameterSource()
                            .addValue("AgentID", request.getStrAgentID().trim())
                            .addValue("ContactID", contactIdLong)
                            .addValue("Error", request.getStrError() != null ? request.getStrError().trim() : "");

                    jdbcCall.execute(inParams);
                    objResult.setContactResultID(0);
                    log.info("unableToReadContact() - Executed {} successfully via SimpleJdbcCall", spUnableToReadContact);
                } catch (Exception spEx) {
                    log.warn("unableToReadContact() - SimpleJdbcCall failed, attempting native EXEC fallback: {}", spEx.getMessage());
                    jdbcTemplate.update("EXEC " + spUnableToReadContact + " ?, ?, ?",
                            request.getStrAgentID().trim(),
                            contactIdLong != null ? contactIdLong : request.getStrContactID().trim(),
                            request.getStrError() != null ? request.getStrError().trim() : "");
                    objResult.setContactResultID(0);
                    log.info("unableToReadContact() - Executed {} successfully via native EXEC", spUnableToReadContact);
                }

            } catch (Exception ex) {
                log.warn("unableToReadContact() - Procedure {} call failed (procedure can be created manually in DB): {}", spUnableToReadContact, ex.getMessage());
                // Return success code matching legacy behavior so client flow does not break
                objResult.setContactResultID(0);
                objResult.setMessage("Valid Input");
            }
        } else {
            log.warn("unableToReadContact() - Rejected: strAgentID and strContactID are required");
        }

        return objResult;
    }

    private boolean isNotEmpty(String val) {
        return val != null && !val.trim().isEmpty();
    }
}
