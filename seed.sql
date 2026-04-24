-- Manual development seed data for the pupil map flow.
-- Apply after schema.sql has created the database objects.

-- Run this to seed the database: docker compose exec -T db psql -U app -d app < seed.sql
-- Accounts you can log in with:
-- demo.teacher@example.com
-- map.partial@example.com
-- map.complete@example.com
-- teacher.multi@example.com
-- teacher.shared@example.com
-- rank.two@example.com
-- rank.three@example.com

-- Password: Test1234

INSERT INTO classroom (classroom_code, classroom_name, classroom_description)
VALUES
    ('MAPAAA', 'Kart testklasse', 'Utviklingsklasse for kartvisning'),
    ('MAPAAB', 'Mellomklasse', 'Ekstra klasse for ranking og teacher-overview'),
    ('MAPAAC', 'Sisteklasse', 'Ekstra klasse for ranking og teacher-overview'),
    ('CHAPAA', 'Kapitteltestklasse', 'Utviklingsklasse for kapittelvisning')
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
        'demo.teacher@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Demo Teacher',
        'TEACHER'
    ),
    (
        'demo.alice@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Alice Accepted',
        'PUPIL'
    ),
    (
        'demo.bob@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Bob Accepted',
        'PUPIL'
    ),
    (
        'demo.carla@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Carla Accepted',
        'PUPIL'
    ),
    (
        'demo.pending.one@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Pending One',
        'PUPIL'
    ),
    (
        'demo.pending.two@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Pending Two',
        'PUPIL'
    ),
    (
        'demo.pending.three@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Pending Three',
        'PUPIL'
    ),
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
        'chapter.test@example.com',
        '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
        'Chapter Test',
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

INSERT INTO file_object (object_key, uploaded_by, classroom_code, access_level, file_name)
VALUES
    ('avatar-images/demo_alice.png', NULL, NULL, 'PUBLIC', 'demo_alice.png'),
    ('avatar-images/demo_bob.png', NULL, NULL, 'PUBLIC', 'demo_bob.png'),
    ('avatar-images/demo_carla.png', NULL, NULL, 'PUBLIC', 'demo_carla.png')
ON CONFLICT (object_key) DO UPDATE
SET
    uploaded_by = EXCLUDED.uploaded_by,
    classroom_code = EXCLUDED.classroom_code,
    access_level = EXCLUDED.access_level,
    file_name = EXCLUDED.file_name;

INSERT INTO account_classroom_bridge_teacher (account_email, classroom_code)
VALUES
    ('teacher.multi@example.com', 'MAPAAA'),
    ('teacher.multi@example.com', 'MAPAAB'),
    ('teacher.shared@example.com', 'MAPAAB'),
    ('teacher.shared@example.com', 'MAPAAC')
ON CONFLICT (account_email, classroom_code) DO NOTHING;

INSERT INTO pupil_classroom_membership (
    account_email,
    classroom_code,
    username,
    membership_confirmed_by_teacher,
    avatar_file_object_id
)
VALUES
    ('map.partial@example.com', 'MAPAAA', 'map.partial', TRUE),
    ('map.complete@example.com', 'MAPAAB', 'map.complete', TRUE),
    ('rank.two@example.com', 'MAPAAB', 'rank.two', TRUE),
    ('rank.three@example.com', 'MAPAAC', 'rank.three', TRUE),
    ('chapter.test@example.com', 'CHAPAA', 'chapter.test', TRUE)
ON CONFLICT (account_email) DO UPDATE
SET
    classroom_code = EXCLUDED.classroom_code,
    username = EXCLUDED.username,
    membership_confirmed_by_teacher = EXCLUDED.membership_confirmed_by_teacher,
    avatar_file_object_id = EXCLUDED.avatar_file_object_id;

INSERT INTO pupil_chapter_bridge_progression (
    email,
    chapter_id,
    successful_runs,
    highscore,
    unsuccessful_streak
)
VALUES
    ('demo.alice@example.com', 1, 3, 3, 0),
    ('demo.alice@example.com', 2, 2, 2, 0),
    ('demo.bob@example.com', 1, 1, 1, 0),
    ('demo.carla@example.com', 1, 3, 3, 0),
    ('demo.carla@example.com', 2, 3, 3, 0),
    ('demo.carla@example.com', 3, 1, 2, 0),
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

DELETE FROM notification
WHERE classroom_code IN ('TESTAA', 'TESTBB')
  AND notification_text IN (
      'Alice Accepted har bedt om hjelp i Nyhetskvartalet',
      'Bob Accepted venter på tilbakemelding fra lærer',
      'Carla Accepted har fullført Fotografen'
  );

INSERT INTO notification (
    classroom_code,
    account_email,
    notification_text,
    status_notifications
)
VALUES
    (
        'TESTAA',
        'demo.alice@example.com',
        'Alice Accepted har bedt om hjelp i Nyhetskvartalet',
        'UNREAD'
    ),
    (
        'TESTAA',
        'demo.bob@example.com',
        'Bob Accepted venter på tilbakemelding fra lærer',
        'UNREAD'
    ),
    (
        'TESTBB',
        'demo.carla@example.com',
        'Carla Accepted har fullført Fotografen',
        'READ'
    );

-- Fotografen (chapter_id = 2) task bank.
-- selectedCategory mapping: 1 = ekte, 2 = KI-generert, 3 = manipulert.
-- fileId must reference an existing row in file_object (upload via POST /api/objects first).
INSERT INTO task (id, difficulty, chapter_id, classroom_code, task_json)
VALUES
    (1, 'EASY', 2, 'CHAPAA', '{
        "taskDescription": "Er dette bildet ekte, KI-generert eller manipulert?",
        "fileId": 1,
        "correctCategory": 1,
        "correctJustification": "Detaljer og skygger ser naturlige ut",
        "justifications": [
            "Detaljer og skygger ser naturlige ut",
            "Ansikter ser litt rare ut",
            "Bakgrunnen er kopiert inn"
        ],
        "correctDescription": "Riktig! Dette bildet er ekte.",
        "falseDescription": "Ikke helt. Dette bildet er faktisk ekte."
    }'::jsonb),
    (2, 'MEDIUM', 2, 'CHAPAA', '{
        "taskDescription": "Er dette bildet ekte, KI-generert eller manipulert?",
        "fileId": 2,
        "correctCategory": 2,
        "correctJustification": "Hender og små detaljer ser unaturlige ut",
        "justifications": [
            "Hender og små detaljer ser unaturlige ut",
            "Bildet er tatt med dårlig lys",
            "Et objekt er fjernet fra bakgrunnen"
        ],
        "correctDescription": "Riktig! Dette bildet er KI-generert.",
        "falseDescription": "Ikke helt. Dette bildet er KI-generert."
    }'::jsonb),
    (3, 'HARD', 2, 'CHAPAA', '{
        "taskDescription": "Er dette bildet ekte, KI-generert eller manipulert?",
        "fileId": 1,
        "correctCategory": 3,
        "correctJustification": "Et objekt er lagt til eller fjernet",
        "justifications": [
            "Et objekt er lagt til eller fjernet",
            "Fargene er litt blasse",
            "Hele bildet er KI-generert"
        ],
        "correctDescription": "Riktig! Dette bildet er manipulert.",
        "falseDescription": "Ikke helt. Dette bildet er manipulert."
    }'::jsonb)
ON CONFLICT (id) DO UPDATE
SET
    difficulty = EXCLUDED.difficulty,
    chapter_id = EXCLUDED.chapter_id,
    classroom_code = EXCLUDED.classroom_code,
    task_json = EXCLUDED.task_json;

SELECT setval('task_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM task), 0), 3), true);