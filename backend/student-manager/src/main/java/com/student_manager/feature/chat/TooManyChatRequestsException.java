package com.student_manager.feature.chat;

/** A user sent more chat requests than the per-minute limit allows. */
public class TooManyChatRequestsException extends RuntimeException {

    public TooManyChatRequestsException(String message) {
        super(message);
    }
}
