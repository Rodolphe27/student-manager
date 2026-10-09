package com.student_manager.feature.chat;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the changes the assistant proposed until the user confirms or ignores them.
 *
 * <p>In memory on purpose: a proposal is only useful for a few minutes, and losing it on a
 * restart just means the user asks again. An id can be used once, only by the user it was
 * made for, and only before it expires.
 */
@Component
public class PendingActionStore {

    static final Duration TTL = Duration.ofMinutes(10);
    static final int MAX_PER_USER = 5;

    /** A stored proposal and what is needed to run it. */
    public record Entry(String id, String username, String type, Map<String, Long> params,
                        String description, Instant expiresAt) {
    }

    private final Clock clock;
    private final Map<String, Deque<Entry>> byUser = new ConcurrentHashMap<>();

    public PendingActionStore() {
        this(Clock.systemUTC());
    }

    PendingActionStore(Clock clock) {
        this.clock = clock;
    }

    /** Stores a proposal for {@code username}; the oldest one is dropped beyond {@link #MAX_PER_USER}. */
    public PendingAction propose(String username, String type, Map<String, Long> params, String description) {
        Entry entry = new Entry(UUID.randomUUID().toString(), username, type, Map.copyOf(params),
                description, clock.instant().plus(TTL));
        byUser.compute(username, (u, queue) -> {
            Deque<Entry> q = queue == null ? new ArrayDeque<>() : queue;
            q.removeIf(e -> e.expiresAt().isBefore(clock.instant()));
            q.addLast(entry);
            while (q.size() > MAX_PER_USER) {
                q.removeFirst();
            }
            return q;
        });
        return new PendingAction(entry.id(), type, description);
    }

    /** Removes and returns the proposal if it exists, belongs to {@code username} and has not expired. */
    public Optional<Entry> take(String id, String username) {
        Deque<Entry> queue = byUser.get(username);
        if (queue == null) {
            return Optional.empty();
        }
        synchronized (queue) {
            Optional<Entry> found = queue.stream().filter(e -> e.id().equals(id)).findFirst();
            found.ifPresent(queue::remove);
            return found.filter(e -> !e.expiresAt().isBefore(clock.instant()));
        }
    }
}
