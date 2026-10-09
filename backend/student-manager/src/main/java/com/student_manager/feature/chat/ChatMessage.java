package com.student_manager.feature.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One turn of the conversation as the browser keeps it. Only plain text travels between
 * browser and server; tool calls and their results stay on the server.
 */
public record ChatMessage(
        @NotBlank @Pattern(regexp = "user|assistant", message = "role must be user or assistant") String role,
        @NotBlank @Size(max = ChatService.MAX_MESSAGE_CHARS, message = "message is too long") String content) {
}
