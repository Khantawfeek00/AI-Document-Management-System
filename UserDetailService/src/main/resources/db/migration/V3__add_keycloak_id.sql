ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS keycloak_id VARCHAR(255);
