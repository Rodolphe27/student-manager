package com.student_manager.feature.invite;

import com.student_manager.feature.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationInviteDTO {
    private Long id;
    private String code;
    private Role role;
    private ProfileType targetType;
    private Long targetId;
    private LocalDateTime expiresAt;
}
