-- Personal details for accounts that have no Student/Teacher profile (e.g. ADMIN),
-- so every user can keep a name on their own account.
ALTER TABLE users
    ADD COLUMN first_name character varying(255),
    ADD COLUMN last_name character varying(255);
