package com.student_manager.feature.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Chat with the in-app assistant. Every signed-in role may use it; what it can do follows the caller's rights. */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;

    /** Lets the frontend hide the chat button when the server has no API key. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("enabled", service.isAvailable()));
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.chat(request, authentication));
    }

    /** Runs a change the assistant proposed, after the user pressed the confirm button. */
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResponse> confirm(@Valid @RequestBody ConfirmRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.confirm(request.actionId(), authentication));
    }
}
