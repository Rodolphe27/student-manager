package com.student_manager.feature.chat;

/**
 * Something the assistant wants to change on the user's behalf. It is only a proposal: it runs
 * when the user confirms it in the UI, never because the model said so.
 *
 * @param id          opaque, single-use id the browser sends back to confirm
 * @param type        what will happen, e.g. {@code ENROLL}
 * @param description human-readable sentence shown on the confirmation button
 */
public record PendingAction(String id, String type, String description) {
}
