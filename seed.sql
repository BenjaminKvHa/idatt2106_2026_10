-- Manual development seed data for the pupil map flow.
-- Apply after schema.sql has created the database objects.

-- Run this to seed the database: docker compose exec -T db psql -U app -d app < seed.sql
-- Run this to seed the database on windows: Get-Content .\seed.sql | docker compose exec -T db psql -U app -d app
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
    ('CHAPAA', 'Kapitteltestklasse', 'Utviklingsklasse for kapittelvisning'),
    ('TESTAA', 'Demo testklasse A', 'Utviklingsklasse for lærer- og varslingsvisning'),
    ('TESTBB', 'Demo testklasse B', 'Ekstra utviklingsklasse for lærer- og varslingsvisning')
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

SELECT setval(
    'chapter_id_seq',
    GREATEST(COALESCE((SELECT MAX(id) FROM chapter), 0), 3),
    TRUE
);

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
    ('teacher.shared@example.com', 'MAPAAC'),
    ('teacher.shared@example.com', 'TESTAA')
ON CONFLICT (account_email, classroom_code) DO NOTHING;

INSERT INTO pupil_classroom_membership (
    account_email,
    classroom_code,
    username,
    membership_confirmed_by_teacher,
    avatar_file_object_id
)
VALUES
    ('demo.alice@example.com', 'TESTAA', 'alice', TRUE, (SELECT id FROM file_object WHERE object_key = 'avatar-images/demo_alice.png')),
    ('demo.bob@example.com', 'TESTAA', 'bob', TRUE, (SELECT id FROM file_object WHERE object_key = 'avatar-images/demo_bob.png')),
    ('demo.carla@example.com', 'TESTBB', 'carla', TRUE, (SELECT id FROM file_object WHERE object_key = 'avatar-images/demo_carla.png')),
    ('demo.pending.one@example.com', 'TESTAA', 'pending.one', FALSE, NULL),
    ('demo.pending.two@example.com', 'TESTAA', 'pending.two', FALSE, NULL),
    ('demo.pending.three@example.com', 'TESTBB', 'pending.three', FALSE, NULL),
    ('map.partial@example.com', 'MAPAAA', 'map.partial', TRUE, NULL),
    ('map.complete@example.com', 'MAPAAB', 'map.complete', TRUE, NULL),
    ('rank.two@example.com', 'MAPAAB', 'rank.two', TRUE, NULL),
    ('rank.three@example.com', 'MAPAAC', 'rank.three', TRUE, NULL),
    ('chapter.test@example.com', 'CHAPAA', 'chapter.test', TRUE, 1)
ON CONFLICT (account_email) DO UPDATE
SET
    classroom_code = EXCLUDED.classroom_code,
    username = EXCLUDED.username,
    membership_confirmed_by_teacher = EXCLUDED.membership_confirmed_by_teacher,
    avatar_file_object_id = EXCLUDED.avatar_file_object_id;

WITH progression_seed (
    email,
    chapter_id,
    successful_runs,
    highscore,
    unsuccessful_streak
)
AS (
    VALUES
        ('demo.alice@example.com', 1, 3, 3, 0),
        ('demo.alice@example.com', 2, 0, 2, 1),
        ('demo.bob@example.com', 1, 0, 1, 1),
        ('demo.carla@example.com', 1, 3, 3, 0),
        ('demo.carla@example.com', 2, 3, 3, 0),
        ('demo.carla@example.com', 3, 0, 2, 1),
        ('map.partial@example.com', 1, 3, 3, 0),
        ('map.partial@example.com', 2, 3, 3, 0),
        ('map.complete@example.com', 1, 3, 3, 0),
        ('map.complete@example.com', 2, 3, 3, 0),
        ('map.complete@example.com', 3, 5, 5, 0),
        ('rank.two@example.com', 1, 1, 3, 0),
        ('rank.two@example.com', 2, 0, 1, 1),
        ('rank.three@example.com', 1, 0, 1, 1)
),
chapter_tips (chapter_id, tips)
AS (
    VALUES
        (1, ARRAY[
            'Se nøye på kilden før du bestemmer deg.',
            'Sammenlign overskrift og innhold. De bør fortelle samme historie.',
            'Let etter tegn på at saken prøver å lure deg med sterke følelser.',
            'Sjekk om bildet faktisk passer til saken som blir fortalt.',
            'Vær ekstra kritisk hvis overskriften lover noe veldig dramatisk.'
        ]::TEXT[]),
        (2, ARRAY[
            'Se etter detaljer i bildet som ikke passer med resten.',
            'Sjekk lys, skygger og kanter rundt viktige objekter.',
            'Tenk over om bildet kan være tatt ut av sin opprinnelige sammenheng.',
            'Zoom mentalt inn på hender, ansikter og tekst. Der dukker feil ofte opp.',
            'Sammenlign speilinger og bakgrunn med resten av bildet.'
        ]::TEXT[]),
        (3, ARRAY[
            'Les hele oppgaven før du velger svar.',
            'Bruk det du lærte i de tidligere kapitlene.',
            'Ta deg tid til å sjekke både tekst og bilde.',
            'Hvis noe virker for godt eller rart til å være sant, undersøk det ekstra nøye.',
            'Se etter små detaljer som avslører hva som faktisk skjer.',
            'Tenk gjennom hvem som kan tjene på at du tror på innholdet.'
        ]::TEXT[])
)
INSERT INTO pupil_chapter_bridge_progression (
    email,
    chapter_id,
    successful_runs,
    highscore,
    unsuccessful_streak,
    notebook_json
)
SELECT
    progression_seed.email,
    progression_seed.chapter_id,
    progression_seed.successful_runs,
    progression_seed.highscore,
    progression_seed.unsuccessful_streak,
    jsonb_build_object(
        'tips',
        CASE
            WHEN progression_seed.successful_runs >= 1
                THEN to_jsonb(COALESCE(chapter_tips.tips, ARRAY[]::TEXT[]))
            ELSE '[]'::jsonb
        END,
        'ownNotes',
        ''
    )
FROM progression_seed
LEFT JOIN chapter_tips ON chapter_tips.chapter_id = progression_seed.chapter_id
ON CONFLICT (email, chapter_id) DO UPDATE
SET
    successful_runs = EXCLUDED.successful_runs,
    highscore = EXCLUDED.highscore,
    unsuccessful_streak = EXCLUDED.unsuccessful_streak,
    notebook_json = EXCLUDED.notebook_json;

-- Weekly mysteries: a mix of approved (different ISO weeks) and pending,
-- across the TESTAA classroom so demo.alice / demo.bob / demo.pending pupils see them.
-- file_object ids 1..10 come from TaskSeeder. weekly_mystery.file_object_id is UNIQUE,
-- so each id is used at most once below.
DELETE FROM weekly_mystery WHERE classroom_code IN ('TESTAA', 'TESTBB');

INSERT INTO weekly_mystery (
    file_object_id,
    classroom_code,
    account_email,
    comment,
    approved,
    approved_at,
    created_at
)

VALUES
    -- Current week (TESTAA): two approved + one pending
    (1, 'TESTAA', 'demo.alice@example.com',
     'Bildet ser veldig ekte ut, men jeg er litt usikker på øynene.',
     TRUE,  NOW() - INTERVAL '1 day',  NOW() - INTERVAL '1 day 2 hours'),
    (2, 'TESTAA', 'demo.bob@example.com',
     'Folkene i mengden ser nesten identiske ut – ser KI-generert ut.',
     TRUE,  NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days 3 hours'),
    (3, 'TESTAA', 'demo.alice@example.com',
     'Skarpheten på jorda er for perfekt. Jeg tror dette er KI.',
     FALSE, NULL,                       NOW() - INTERVAL '4 hours'),

    -- Last week (TESTAA): one approved
    (4, 'TESTAA', 'demo.bob@example.com',
     'Blomstene har for glatte teksturer.',
     TRUE,  NOW() - INTERVAL '8 days',  NOW() - INTERVAL '8 days 2 hours'),

    -- Two weeks ago (TESTAA): one approved
    (5, 'TESTAA', 'demo.alice@example.com',
     'Trærne ser duplisert ut og himmelen er rar.',
     TRUE,  NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days 4 hours'),

    -- TESTBB: one approved this week + one pending
    (6, 'TESTBB', 'demo.carla@example.com',
     'Ansiktet i bakgrunnen ser litt forvrengt ut.',
     TRUE,  NOW() - INTERVAL '1 day',   NOW() - INTERVAL '1 day 1 hour'),
    (7, 'TESTBB', 'demo.carla@example.com',
     'Logoen på skiltet ser ikke riktig ut.',
     FALSE, NULL,                       NOW() - INTERVAL '6 hours');



DELETE FROM notification
WHERE classroom_code = 'MAPAAA'
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
        'MAPAAA',
        'demo.alice@example.com',
        'Alice Accepted har bedt om hjelp i Nyhetskvartalet',
        'UNREAD'
    ),
    (
        'MAPAAA',
        'demo.bob@example.com',
        'Bob Accepted venter på tilbakemelding fra lærer',
        'UNREAD'
    ),
    (
        'MAPAAA',
        'demo.carla@example.com',
        'Carla Accepted har fullført Fotografen',
        'READ'
    );
