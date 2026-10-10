-- The invitation flow was removed; accounts are now created by an ADMIN
-- (POST /api/auth/register). The invite table and its foreign keys go with it.
DROP TABLE IF EXISTS registration_invites;
