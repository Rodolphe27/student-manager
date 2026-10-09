package com.student_manager.feature.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The whole conversation so far; the server keeps no chat history of its own. */
public record ChatRequest(
        @NotEmpty @Size(max = ChatService.MAX_MESSAGES, message = "conversation is too long") List<@Valid ChatMessage> messages) {
}
