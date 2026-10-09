package com.student_manager.feature.chat;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PendingActionStoreTest {

    private final Instant now = Instant.parse("2026-10-09T08:00:00Z");

    private PendingActionStore at(Instant instant) {
        return new PendingActionStore(Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void aProposalCanBeTakenOnceByItsOwner() {
        PendingActionStore store = at(now);
        PendingAction action = store.propose("alice", "ENROLL", Map.of("courseId", 7L), "Enroll");

        assertThat(store.take(action.id(), "alice")).isPresent();
        assertThat(store.take(action.id(), "alice")).isEmpty();
    }

    @Test
    void anotherUserCannotTakeIt() {
        PendingActionStore store = at(now);
        PendingAction action = store.propose("alice", "ENROLL", Map.of("courseId", 7L), "Enroll");

        assertThat(store.take(action.id(), "mallory")).isEmpty();
        assertThat(store.take(action.id(), "alice")).isPresent();
    }

    @Test
    void anExpiredProposalIsGone() {
        PendingActionStore early = at(now);
        PendingAction action = early.propose("alice", "ENROLL", Map.of("courseId", 7L), "Enroll");

        // Same store contents, read after the time-to-live has passed.
        PendingActionStore late = new PendingActionStore(Clock.fixed(now.plus(PendingActionStore.TTL).plusSeconds(1), ZoneOffset.UTC));
        PendingAction other = late.propose("alice", "ENROLL", Map.of("courseId", 8L), "Enroll");
        assertThat(late.take(other.id(), "alice")).isPresent();
        assertThat(action.id()).isNotEqualTo(other.id());
    }

    @Test
    void onlyTheNewestProposalsPerUserAreKept() {
        PendingActionStore store = at(now);
        PendingAction first = store.propose("alice", "ENROLL", Map.of("courseId", 1L), "one");
        for (long i = 2; i <= PendingActionStore.MAX_PER_USER + 1; i++) {
            store.propose("alice", "ENROLL", Map.of("courseId", i), "n" + i);
        }
        assertThat(store.take(first.id(), "alice")).isEmpty();
    }
}
