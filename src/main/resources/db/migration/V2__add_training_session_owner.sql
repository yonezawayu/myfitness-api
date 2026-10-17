ALTER TABLE public.training_sessions
    ADD COLUMN user_id bigint;

ALTER TABLE public.training_sessions
    ADD CONSTRAINT fk_training_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES public.users (id)
        ON DELETE RESTRICT;

CREATE INDEX idx_training_sessions_user_date_id
    ON public.training_sessions (user_id, date DESC, id DESC);
