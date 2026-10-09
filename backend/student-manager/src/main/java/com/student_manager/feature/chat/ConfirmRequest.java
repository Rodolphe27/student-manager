package com.student_manager.feature.chat;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRequest(@NotBlank String actionId) {
}
