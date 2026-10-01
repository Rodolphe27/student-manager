-- A new grade is flagged as unseen until the student acknowledges it (in-app notification).
-- Existing rows count as seen, so nobody is notified about old grades.
ALTER TABLE enrollments
    ADD COLUMN grade_seen boolean NOT NULL DEFAULT true;
