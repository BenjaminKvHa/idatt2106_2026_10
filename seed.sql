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
    ('CHAPAA', 'Kapitteltestklasse', 'Utviklingsklasse for kapittelvisning'),
    ('TESTAA', 'Demo testklasse A', 'Utviklingsklasse for lærer- og varslingsvisning'),
    ('TESTBB', 'Demo testklasse B', 'Ekstra utviklingsklasse for lærer- og varslingsvisning')
ON CONFLICT (classroom_code) DO UPDATE
SET
    classroom_name = EXCLUDED.classroom_name,
    classroom_description = EXCLUDED.classroom_description;

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
<<<<<<< HEAD
    ('demo.alice@example.com', 'MAPAAA', 'demo.alice', TRUE, NULL),
    ('demo.bob@example.com', 'MAPAAA', 'demo.bob', TRUE, NULL),
    ('demo.carla@example.com', 'MAPAAA', 'demo.carla', TRUE, NULL),
=======
    ('demo.alice@example.com', 'TESTAA', 'alice', TRUE, (SELECT id FROM file_object WHERE object_key = 'avatar-images/demo_alice.png')),
    ('demo.bob@example.com', 'TESTAA', 'bob', TRUE, (SELECT id FROM file_object WHERE object_key = 'avatar-images/demo_bob.png')),
    ('demo.carla@example.com', 'TESTBB', 'carla', TRUE, (SELECT id FROM file_object WHERE object_key = 'avatar-images/demo_carla.png')),
    ('demo.pending.one@example.com', 'TESTAA', 'pending.one', FALSE, NULL),
    ('demo.pending.two@example.com', 'TESTAA', 'pending.two', FALSE, NULL),
    ('demo.pending.three@example.com', 'TESTBB', 'pending.three', FALSE, NULL),
>>>>>>> 1230b6773f1d74e90158f742e23e9059420fab47
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

