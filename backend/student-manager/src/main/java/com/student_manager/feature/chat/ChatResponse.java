package com.student_manager.feature.chat;

import java.util.List;

/** The assistant's answer plus any changes it proposes and the user still has to confirm. */
public record ChatResponse(String reply, List<PendingAction> pendingActions) {
}
