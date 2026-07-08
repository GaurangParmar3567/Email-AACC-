package com.example.mail.dto.request;

import lombok.Data;
import java.util.Set;

@Data
public class UserRequestDTO {
    private String firstName;
    private String lastName;
    private Set<Long> skillIds;
}
