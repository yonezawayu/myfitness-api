DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.training_sessions
        WHERE user_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'training_sessions.user_id contains NULL values; run the approved local backfill before V3';
    END IF;
END
$$;

ALTER TABLE public.training_sessions
    ALTER COLUMN user_id SET NOT NULL;
