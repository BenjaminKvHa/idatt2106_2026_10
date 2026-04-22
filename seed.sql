-- Manual development seed data for the pupil map flow.
-- Apply after schema.sql has created the database objects.

-- Run this to seed the datbase: docker compose exec -T db psql -U app -d app < seed.sql
-- Accounts you can logg in with:
-- map.partial@example.com
-- map.complete@example.com
-- teacher.multi@example.com
-- teacher.shared@example.com
-- rank.two@example.com
-- rank.three@example.com

-- Password: Test1234

INSERT INTO classroom (classroom_code, classroom_name, classroom_description)
VALUES
    ('MAP001', 'Kart testklasse', 'Utviklingsklasse for kartvisning'),
    ('MAP002', 'Mellomklasse', 'Ekstra klasse for ranking og teacher-overview'),
    ('MAP003', 'Sisteklasse', 'Ekstra klasse for ranking og teacher-overview')
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
    ),
    (
        'rank.two@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Rank Two',
        'PUPIL'
    ),
    (
        'rank.three@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Rank Three',
        'PUPIL'
    ),
    (
        'teacher.multi@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Teacher Multi',
        'TEACHER'
    ),
    (
        'teacher.shared@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Teacher Shared',
        'TEACHER'
    )
ON CONFLICT (email) DO UPDATE
SET
    password_data = EXCLUDED.password_data,
    legal_name = EXCLUDED.legal_name,
    account_role = EXCLUDED.account_role;

INSERT INTO account_classroom_bridge_teacher (account_email, classroom_code)
VALUES
    ('teacher.multi@example.com', 'MAP001'),
    ('teacher.multi@example.com', 'MAP002'),
    ('teacher.shared@example.com', 'MAP002'),
    ('teacher.shared@example.com', 'MAP003')
ON CONFLICT (account_email, classroom_code) DO NOTHING;

INSERT INTO pupil_classroom_membership (
    account_email,
    classroom_code,
    username,
    membership_confirmed_by_teacher
)
VALUES
    ('map.partial@example.com', 'MAP001', 'map.partial', TRUE),
    ('map.complete@example.com', 'MAP001', 'map.complete', TRUE),
    ('rank.two@example.com', 'MAP002', 'rank.two', TRUE),
    ('rank.three@example.com', 'MAP003', 'rank.three', TRUE)
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
    ('map.complete@example.com', 3, 5, 5, 0),
    ('rank.two@example.com', 1, 1, 2, 0),
    ('rank.two@example.com', 2, 1, 1, 0),
    ('rank.three@example.com', 1, 1, 1, 0)
ON CONFLICT (email, chapter_id) DO UPDATE
SET
    successful_runs = EXCLUDED.successful_runs,
    highscore = EXCLUDED.highscore,
    unsuccessful_streak = EXCLUDED.unsuccessful_streak;
