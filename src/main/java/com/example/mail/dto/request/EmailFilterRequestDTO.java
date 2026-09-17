package com.example.mail.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailFilterRequestDTO {

    private Long contactId;
    private Long agentId;
    private Long skillId;
    private String skillsetName;
    private String status;
    private String sender;
    private String recipient;
    private String subject;
    private Boolean assigned;
    private Boolean responded;
    private Boolean repeatFlag;
    private String source;
    private String priority;
    private Long priorityId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;

    private String fromDate;
    private String toDate;

    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "receivedDate";
    private String sortDirection = "DESC";

    public Date resolveStartDate() {
        if (startDate != null) {
            return startDate;
        }
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            return parseDateSafely(fromDate.trim(), false);
        }
        return null;
    }

    public Date resolveEndDate() {
        if (endDate != null) {
            return endDate;
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            return parseDateSafely(toDate.trim(), true);
        }
        return null;
    }

    private Date parseDateSafely(String str, boolean isEndOfDay) {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Date d = sdf.parse(str);
                if ("yyyy-MM-dd".equals(pattern) && isEndOfDay) {
                    return new Date(d.getTime() + (24L * 60 * 60 * 1000 - 1));
                }
                return d;
            } catch (ParseException ignored) {
            }
        }
        try {
            long epoch = Long.parseLong(str);
            return new Date(epoch);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }
}

