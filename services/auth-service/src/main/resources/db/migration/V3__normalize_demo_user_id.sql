DO $$
DECLARE
    stable_demo_user_id UUID := '11111111-1111-1111-1111-111111111111';
    existing_demo_user_id UUID;
BEGIN
    SELECT id
    INTO existing_demo_user_id
    FROM user_credentials
    WHERE username = 'demo.user'
    LIMIT 1;

    IF existing_demo_user_id IS NOT NULL
       AND existing_demo_user_id <> stable_demo_user_id
       AND NOT EXISTS (SELECT 1 FROM user_credentials WHERE id = stable_demo_user_id) THEN
        UPDATE user_credentials
        SET username = 'demo.user.legacy-' || LEFT(existing_demo_user_id::text, 8),
            email = 'legacy-' || existing_demo_user_id::text || '@novabank.test',
            enabled = false,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = existing_demo_user_id;

        INSERT INTO user_credentials (
            id,
            username,
            email,
            password_hash,
            role,
            enabled,
            failed_login_count,
            locked_until,
            created_at,
            updated_at,
            version
        )
        SELECT
            stable_demo_user_id,
            'demo.user',
            'demo.user@novabank.test',
            password_hash,
            role,
            true,
            failed_login_count,
            locked_until,
            created_at,
            CURRENT_TIMESTAMP,
            version
        FROM user_credentials
        WHERE id = existing_demo_user_id;

        UPDATE refresh_tokens
        SET user_id = stable_demo_user_id
        WHERE user_id = existing_demo_user_id;

        UPDATE password_reset_tokens
        SET user_id = stable_demo_user_id
        WHERE user_id = existing_demo_user_id;

        UPDATE audit_logs
        SET user_id = stable_demo_user_id
        WHERE user_id = existing_demo_user_id;

        DELETE FROM user_credentials
        WHERE id = existing_demo_user_id;
    END IF;
END $$;
