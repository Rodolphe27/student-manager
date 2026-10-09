package com.student_manager.feature.chat;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Talks to Claude through the official Anthropic SDK. The API key comes from the server's
 * environment ({@code ANTHROPIC_API_KEY}); without it {@link #isConfigured()} is false.
 *
 * <p>The tool loop is written by hand so that every tool call is executed through
 * {@link Toolbox}, where the caller's rights are enforced.
 */
@Slf4j
@Component
public class ClaudeAssistantClient implements AssistantClient {

    /** Tool round-trips allowed per user message; stops a model that keeps calling tools. */
    static final int MAX_STEPS = 6;
    private static final long MAX_TOKENS = 4096L;

    private final AnthropicClient client;
    private final String model;

    public ClaudeAssistantClient(@Value("${chat.anthropic-api-key:}") String apiKey,
                                 @Value("${chat.model:claude-opus-5-5}") String model) {
        this.client = apiKey == null || apiKey.isBlank() ? null
                : AnthropicOkHttpClient.builder().apiKey(apiKey).timeout(Duration.ofSeconds(90)).build();
        this.model = model;
    }

    @Override
    public boolean isConfigured() {
        return client != null;
    }

    @Override
    public String reply(String systemPrompt, List<ChatMessage> history, Toolbox tools) {
        if (client == null) {
            throw new AssistantUnavailableException("The assistant is not set up on this server.");
        }
        MessageCreateParams.Builder request = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(systemPrompt);
        for (ToolSpec spec : tools.specs()) {
            request.addTool(toTool(spec));
        }
        for (ChatMessage m : history) {
            if ("assistant".equals(m.role())) {
                request.addAssistantMessage(m.content());
            } else {
                request.addUserMessage(m.content());
            }
        }

        try {
            for (int step = 0; step < MAX_STEPS; step++) {
                Message response = client.messages().create(request.build());
                // Keep the whole answer (thinking blocks included) in the conversation for the next round.
                request.addMessage(response);

                if (response.stopReason().filter(StopReason.TOOL_USE::equals).isEmpty()) {
                    return textOf(response);
                }
                List<ContentBlockParam> results = new ArrayList<>();
                response.content().forEach(block -> block.toolUse().ifPresent(use -> {
                    results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                            .toolUseId(use.id())
                            .content(runTool(tools, use.name(), use._input()))
                            .build()));
                }));
                request.addUserMessageOfBlockParams(results);
            }
        } catch (AnthropicException e) {
            log.warn("Claude request failed: {}", e.getMessage());
            throw new AssistantUnavailableException("The assistant could not answer right now. Please try again.", e);
        }
        return "I could not finish that request. Please try asking in a simpler way.";
    }

    @SuppressWarnings("unchecked")
    private static String runTool(Toolbox tools, String name, JsonValue input) {
        Map<String, Object> arguments;
        try {
            Map<String, Object> converted = input.convert(Map.class);
            arguments = converted == null ? Map.of() : converted;
        } catch (RuntimeException e) {
            arguments = new HashMap<>();
        }
        try {
            return tools.run(name, arguments);
        } catch (RuntimeException e) {
            log.warn("Assistant tool {} failed: {}", name, e.toString());
            return "Error: the tool failed.";
        }
    }

    private static String textOf(Message response) {
        String text = response.content().stream()
                .flatMap(b -> b.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining("\n"))
                .strip();
        return text.isEmpty() ? "I do not have an answer for that." : text;
    }

    private static Tool toTool(ToolSpec spec) {
        Tool.InputSchema.Properties.Builder properties = Tool.InputSchema.Properties.builder();
        spec.properties().forEach((name, schema) -> properties.putAdditionalProperty(name, JsonValue.from(schema)));
        return Tool.builder()
                .name(spec.name())
                .description(spec.description())
                .inputSchema(Tool.InputSchema.builder()
                        .properties(properties.build())
                        .required(spec.required())
                        .build())
                .build();
    }
}
