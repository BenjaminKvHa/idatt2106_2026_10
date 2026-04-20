-- Manual development seed data for the pupil map flow.
-- Apply after schema.sql has created the database objects.

-- Run this to seed the datbase: docker compose exec -T db psql -U app -d app < seed.sql
-- Accounts you can logg in with:
-- map.partial@example.com
-- map.complete@example.com

-- Password: Test1234

INSERT INTO classroom (classroom_code, classroom_name, classroom_description)
VALUES ('MAP001', 'Kart testklasse', 'Utviklingsklasse for kartvisning')
ON CONFLICT (classroom_code) DO UPDATE
SET
    classroom_name = EXCLUDED.classroom_name,
    classroom_description = EXCLUDED.classroom_description;

INSERT INTO chapter (id, chapter_name, successful_runs_needed_for_completion)
VALUES
    (1, 'Nyhetskvartalet', 3),
    (2, 'Fotografen', 3),
    (3, 'Datasenteret', 5)
ON CONFLICT (id) DO UPDATE
SET
    chapter_name = EXCLUDED.chapter_name,
    successful_runs_needed_for_completion = EXCLUDED.successful_runs_needed_for_completion;

SELECT setval('chapter_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM chapter), 0), 3), true);

INSERT INTO account (email, password_data, legal_name, account_role)
VALUES
    (
        'map.partial@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Map Partial',
        'PUPIL'
    ),
    (
        'map.complete@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Map Complete',
        'PUPIL'
    )
ON CONFLICT (email) DO UPDATE
SET
    password_data = EXCLUDED.password_data,
    legal_name = EXCLUDED.legal_name,
    account_role = EXCLUDED.account_role;

INSERT INTO pupil_classroom_membership (
    account_email,
    classroom_code,
    username,
    membership_confirmed_by_teacher
)
VALUES
    ('map.partial@example.com', 'MAP001', 'map.partial', TRUE),
    ('map.complete@example.com', 'MAP001', 'map.complete', TRUE)
ON CONFLICT (account_email) DO UPDATE
SET
    classroom_code = EXCLUDED.classroom_code,
    username = EXCLUDED.username,
    membership_confirmed_by_teacher = EXCLUDED.membership_confirmed_by_teacher;

INSERT INTO pupil_chapter_bridge_progression (
    email,
    chapter_id,
    successful_runs,
    highscore,
    unsuccessful_streak
)
VALUES
    ('map.partial@example.com', 1, 3, 3, 0),
    ('map.partial@example.com', 2, 3, 3, 0),
    ('map.complete@example.com', 1, 3, 3, 0),
    ('map.complete@example.com', 2, 3, 3, 0),
    ('map.complete@example.com', 3, 5, 5, 0)
ON CONFLICT (email, chapter_id) DO UPDATE
SET
    successful_runs = EXCLUDED.successful_runs,
    highscore = EXCLUDED.highscore,
    unsuccessful_streak = EXCLUDED.unsuccessful_streak;
