package com.student_manager.feature.profile;

/**
 * Self-service editing of the calling user's own account and personal details.
 * Every method acts on the account named by {@code username}, never on an id
 * from the request, so a user can only ever touch their own data.
 */
public interface ProfileService {

    /**
     * @param username the session principal's username
     * @return the caller's account and, if linked, profile details
     */
    ProfileDTO get(String username);

    /**
     * Updates the caller's username, email and (if linked) profile details.
     * A changed email is written to the linked profile too, so the two never
     * diverge.
     *
     * @param username the session principal's username (before the update)
     * @param request  the new values
     * @return the updated account and profile details
     * @throws com.student_manager.shared.exception.ValidationException if the new username or
     *         email is taken, or a linked profile is missing a first or last name
     */
    ProfileDTO update(String username, UpdateProfileRequest request);

    /**
     * Changes the caller's password after checking the current one.
     *
     * @param username the session principal's username
     * @param request  the current and new password
     * @throws com.student_manager.shared.exception.ValidationException if the current password is wrong
     */
    void changePassword(String username, ChangePasswordRequest request);
}
