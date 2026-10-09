package com.student_manager.feature.chat;

import java.util.List;
import java.util.Map;

/**
 * The language model behind the chat. An interface so the authorization and confirmation rules
 * can be tested without calling a real model.
 */
public interface AssistantClient {

    /** False when no API key is configured; the endpoint then answers 503 instead of failing mid-request. */
    boolean isConfigured();

    /**
     * Runs one user turn to completion, letting the model call {@code tools} as often as it
     * needs, and returns its final text.
     */
    String reply(String systemPrompt, List<ChatMessage> history, Toolbox tools);

    /** What the model may do during a turn. */
    interface Toolbox {

        List<ToolSpec> specs();

        /** Runs a tool and returns text for the model to read. Never throws for "not allowed". */
        String run(String name, Map<String, Object> input);
    }

    /**
     * @param properties JSON-schema property definitions, by property name
     * @param required   names of the required properties
     */
    record ToolSpec(String name, String description, Map<String, Map<String, Object>> properties, List<String> required) {
    }
}
