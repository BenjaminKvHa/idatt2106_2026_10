-- Development/demo seed data for local Docker Compose.
-- compose.yaml runs this automatically after the backend is healthy, so Java boot seeders
-- such as ChapterSeeder, TaskSeeder, MedalAssetInitializer, and DemoAssetInitializer have completed first.
--
-- Optional manual rerun on macOS/Linux: docker compose exec -T db psql -U app -d app < seed.sql
-- Optional manual rerun on Windows: Get-Content .\seed.sql | docker compose exec -T db psql -U app -d app
--
-- Password for every documented login user: Test1234
--
-- Teacher demo logins:
-- teacher.anna@example.com   | owns DEMOAA, DEMOBB, MAPAAA | rich classroom data, pending requests, notifications
-- teacher.marius@example.com | shares DEMOAA               | shared-teacher classroom testing
-- teacher.empty@example.com  | owns EMPTYA                 | mostly empty classroom state
--
-- Pupil demo logins:
-- pupil.start@example.com       | DEMOAA confirmed | no progress yet
-- pupil.pending@example.com     | DEMOAA pending   | waiting-room flow
-- pupil.map.partial@example.com | MAPAAA confirmed | chapter 1+2 complete, chapter 3 unlocked
-- pupil.complete@example.com    | DEMOAA confirmed | all chapters complete
-- pupil.stuck@example.com       | DEMOAA confirmed | stuck/progress notification scenario
-- pupil.weekly@example.com      | DEMOAA confirmed | approved weekly mystery and medal scenario
--
-- Compatibility logins kept for older manual testing:
-- map.partial@example.com, map.complete@example.com, rank.two@example.com, rank.three@example.com

INSERT INTO classroom (classroom_code, classroom_name, classroom_description)
VALUES
    ('DEMOAA', 'Demo 5A', 'Hovedklasse med full demoaktivitet for lærervisning.'),
    ('DEMOBB', 'Demo 6B', 'Ekstra klasse med egne elever og ukens mysterium-bidrag.'),
    ('MAPAAA', 'Kart testklasse', 'Klasse for kartprogresjon og delvis fullført spill.'),
    ('LEADAA', 'Ledertavle testklasse', 'Klasse som gir flere rader på ledertavlen.'),
    ('EMPTYA', 'Tom demoklasse', 'Klasse for å teste tomme lærerflater.'),
    ('CHAPAA', 'Kapitteltestklasse', 'Utviklingsklasse for kapittelvisning.'),
    ('MAPAAB', 'Mellomklasse', 'Kompatibilitetsklasse for eldre rankingtester.'),
    ('MAPAAC', 'Sisteklasse', 'Kompatibilitetsklasse for eldre rankingtester.'),
    ('TESTAA', 'Demo testklasse A', 'Kompatibilitetsklasse for eldre lærer- og varslingsvisning.'),
    ('TESTBB', 'Demo testklasse B', 'Kompatibilitetsklasse for eldre lærer- og varslingsvisning.')
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

WITH demo_accounts (email, legal_name, role) AS (
    VALUES
        ('teacher.anna@example.com', 'Anna Berg', 'TEACHER'),
        ('teacher.marius@example.com', 'Marius Holm', 'TEACHER'),
        ('teacher.empty@example.com', 'Eline Strand', 'TEACHER'),
        ('pupil.start@example.com', 'Sofie Start', 'PUPIL'),
        ('pupil.pending@example.com', 'Peder Pending', 'PUPIL'),
        ('pupil.map.partial@example.com', 'Maja Kart', 'PUPIL'),
        ('pupil.complete@example.com', 'Oskar Komplett', 'PUPIL'),
        ('pupil.stuck@example.com', 'Nora Fastlåst', 'PUPIL'),
        ('pupil.weekly@example.com', 'Ibrahim Mysterium', 'PUPIL'),
        ('pupil.alva@example.com', 'Alva Solheim', 'PUPIL'),
        ('pupil.jonas@example.com', 'Jonas Bakke', 'PUPIL'),
        ('pupil.emma@example.com', 'Emma Lie', 'PUPIL'),
        ('pupil.noah@example.com', 'Noah Vik', 'PUPIL'),
        ('pupil.sara@example.com', 'Sara Moen', 'PUPIL'),
        ('pupil.theodor@example.com', 'Theodor Dahl', 'PUPIL'),
        ('pupil.lina@example.com', 'Lina Aas', 'PUPIL'),
        ('pupil.mathias@example.com', 'Mathias Lund', 'PUPIL'),
        ('pupil.pending.nora@example.com', 'Nora Søker', 'PUPIL'),
        ('pupil.pending.aksel@example.com', 'Aksel Søker', 'PUPIL'),
        ('pupil.pending.ida@example.com', 'Ida Søker', 'PUPIL'),
        ('pupil.pending.leo@example.com', 'Leo Søker', 'PUPIL'),
        ('pupil.pending.frida@example.com', 'Frida Søker', 'PUPIL'),
        ('demo.teacher@example.com', 'Demo Teacher', 'TEACHER'),
        ('teacher.multi@example.com', 'Teacher Multi', 'TEACHER'),
        ('teacher.shared@example.com', 'Teacher Shared', 'TEACHER'),
        ('demo.alice@example.com', 'Alice Accepted', 'PUPIL'),
        ('demo.bob@example.com', 'Bob Accepted', 'PUPIL'),
        ('demo.carla@example.com', 'Carla Accepted', 'PUPIL'),
        ('demo.pending.one@example.com', 'Pending One', 'PUPIL'),
        ('demo.pending.two@example.com', 'Pending Two', 'PUPIL'),
        ('demo.pending.three@example.com', 'Pending Three', 'PUPIL'),
        ('map.partial@example.com', 'Map Partial', 'PUPIL'),
        ('map.complete@example.com', 'Map Complete', 'PUPIL'),
        ('rank.two@example.com', 'Rank Two', 'PUPIL'),
        ('rank.three@example.com', 'Rank Three', 'PUPIL'),
        ('chapter.test@example.com', 'Chapter Test', 'PUPIL')
)
INSERT INTO account (email, password_data, legal_name, account_role)
SELECT
    email,
    '$2a$10$d8rn.gEcXxyXDGh6NTzF2uZTAlOI5m/T1x5Hh/8A7.BeLhGY232ym',
    legal_name,
    role::account_role_enum
FROM demo_accounts
ON CONFLICT (email) DO UPDATE
SET
    password_data = EXCLUDED.password_data,
    legal_name = EXCLUDED.legal_name,
    account_role = EXCLUDED.account_role;

DELETE FROM file_object
WHERE object_key IN (
    'avatar-images/white_long-hair-black_sweater.png',
    'avatar-images/brown_long-hair-black_detective.png',
    'avatar-images/brown_long-hair-black_sweater.png',
    'avatar-images/white_long-hair-brown_sweater.png',
    'avatar-images/white_long-hair-ginger_regular-joe.png'
);

WITH demo_files (object_key, uploaded_by, classroom_code, access_level, file_name) AS (
    VALUES
        ('avatar-images/white_redditor-black_sweater.png', NULL, NULL, 'PUBLIC', 'white_redditor-black_sweater.png'),
        ('avatar-images/brown_redditor-black_detective.png', NULL, NULL, 'PUBLIC', 'brown_redditor-black_detective.png'),
        ('avatar-images/white_old-black_detective.png', NULL, NULL, 'PUBLIC', 'white_old-black_detective.png'),
        ('avatar-images/brown_old-blond_detective.png', NULL, NULL, 'PUBLIC', 'brown_old-blond_detective.png'),
        ('avatar-images/brown_2016-brown-ginger_regular-joe.png', NULL, NULL, 'PUBLIC', 'brown_2016-brown-ginger_regular-joe.png'),
        ('avatar-images/white_redditor-brown_sweater.png', NULL, NULL, 'PUBLIC', 'white_redditor-brown_sweater.png'),
        ('avatar-images/white_redditor-ginger_regular-joe.png', NULL, NULL, 'PUBLIC', 'white_redditor-ginger_regular-joe.png'),
        ('avatar-images/white_2016-white-ginger_detective.png', NULL, NULL, 'PUBLIC', 'white_2016-white-ginger_detective.png'),
        ('avatar-images/brown_2016-brown-black_detective.png', NULL, NULL, 'PUBLIC', 'brown_2016-brown-black_detective.png'),
        ('avatar-images/white_2016-white-black_detective.png', NULL, NULL, 'PUBLIC', 'white_2016-white-black_detective.png'),
        ('avatar-images/brown_redditor-black_sweater.png', NULL, NULL, 'PUBLIC', 'brown_redditor-black_sweater.png'),
        ('weekly-mysteries/demo-fake-news-1.png', 'pupil.weekly@example.com', 'DEMOAA', 'ONLY_CLASS', 'demo-fake-news-1.png'),
        ('weekly-mysteries/demo-crowd-1.png', 'pupil.alva@example.com', 'DEMOAA', 'ONLY_CLASS', 'demo-crowd-1.png'),
        ('weekly-mysteries/demo-flowers-1.png', 'pupil.jonas@example.com', 'DEMOAA', 'ONLY_TEACHERS_IN_CLASS', 'demo-flowers-1.png'),
        ('weekly-mysteries/demo-datacenter-news-1.png', 'pupil.sara@example.com', 'DEMOBB', 'ONLY_CLASS', 'demo-datacenter-news-1.png'),
        ('weekly-mysteries/demo-earth-1.png', 'pupil.theodor@example.com', 'DEMOBB', 'ONLY_TEACHERS_IN_CLASS', 'demo-earth-1.png'),
        ('weekly-mysteries/demo-building-1.png', 'pupil.map.partial@example.com', 'MAPAAA', 'ONLY_CLASS', 'demo-building-1.png')
)
INSERT INTO file_object (object_key, uploaded_by, classroom_code, access_level, file_name)
SELECT
    object_key,
    uploaded_by,
    classroom_code,
    access_level::classroom_access_enum,
    file_name
FROM demo_files
ON CONFLICT (object_key) DO UPDATE
SET
    uploaded_by = EXCLUDED.uploaded_by,
    classroom_code = EXCLUDED.classroom_code,
    access_level = EXCLUDED.access_level,
    file_name = EXCLUDED.file_name;

INSERT INTO account_classroom_bridge_teacher (account_email, classroom_code)
VALUES
    ('teacher.anna@example.com', 'DEMOAA'),
    ('teacher.anna@example.com', 'DEMOBB'),
    ('teacher.anna@example.com', 'MAPAAA'),
    ('teacher.marius@example.com', 'DEMOAA'),
    ('teacher.empty@example.com', 'EMPTYA'),
    ('demo.teacher@example.com', 'TESTAA'),
    ('teacher.multi@example.com', 'MAPAAA'),
    ('teacher.multi@example.com', 'MAPAAB'),
    ('teacher.shared@example.com', 'MAPAAB'),
    ('teacher.shared@example.com', 'MAPAAC'),
    ('teacher.shared@example.com', 'TESTAA')
ON CONFLICT (account_email, classroom_code) DO NOTHING;

WITH membership_seed (email, classroom_code, username, confirmed, avatar_key) AS (
    VALUES
        ('pupil.start@example.com', 'DEMOAA', 'start.sofie', TRUE, 'avatar-images/white_redditor-black_sweater.png'),
        ('pupil.pending@example.com', 'DEMOAA', 'venter.peder', FALSE, 'avatar-images/brown_redditor-black_detective.png'),
        ('pupil.map.partial@example.com', 'MAPAAA', 'kart.maja', TRUE, 'avatar-images/white_old-black_detective.png'),
        ('pupil.complete@example.com', 'DEMOAA', 'komplett.oskar', TRUE, 'avatar-images/brown_old-blond_detective.png'),
        ('pupil.stuck@example.com', 'DEMOAA', 'fast.nora', TRUE, 'avatar-images/brown_2016-brown-ginger_regular-joe.png'),
        ('pupil.weekly@example.com', 'DEMOAA', 'mysterium.ibrahim', TRUE, 'avatar-images/white_redditor-brown_sweater.png'),
        ('pupil.alva@example.com', 'DEMOAA', 'alva.detektiv', TRUE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('pupil.jonas@example.com', 'DEMOAA', 'jonas.kilde', TRUE, 'avatar-images/white_2016-white-ginger_detective.png'),
        ('pupil.emma@example.com', 'DEMOAA', 'emma.foto', TRUE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('pupil.noah@example.com', 'DEMOAA', 'noah.nett', TRUE, 'avatar-images/white_2016-white-ginger_detective.png'),
        ('pupil.sara@example.com', 'DEMOBB', 'sara.spor', TRUE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('pupil.theodor@example.com', 'DEMOBB', 'theodor.tips', TRUE, 'avatar-images/white_2016-white-ginger_detective.png'),
        ('pupil.lina@example.com', 'DEMOBB', 'lina.logg', TRUE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('pupil.mathias@example.com', 'LEADAA', 'mathias.rank', TRUE, 'avatar-images/white_2016-white-ginger_detective.png'),
        ('pupil.pending.nora@example.com', 'DEMOAA', 'nora.soker', FALSE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('pupil.pending.aksel@example.com', 'DEMOAA', 'aksel.soker', FALSE, 'avatar-images/white_2016-white-ginger_detective.png'),
        ('pupil.pending.ida@example.com', 'DEMOBB', 'ida.soker', FALSE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('pupil.pending.leo@example.com', 'DEMOBB', 'leo.soker', FALSE, 'avatar-images/white_2016-white-ginger_detective.png'),
        ('pupil.pending.frida@example.com', 'MAPAAA', 'frida.soker', FALSE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('demo.alice@example.com', 'TESTAA', 'alice', TRUE, 'avatar-images/brown_2016-brown-black_detective.png'),
        ('demo.bob@example.com', 'TESTAA', 'bob', TRUE, 'avatar-images/white_2016-white-black_detective.png'),
        ('demo.carla@example.com', 'TESTBB', 'carla', TRUE, 'avatar-images/brown_redditor-black_sweater.png'),
        ('demo.pending.one@example.com', 'TESTAA', 'pending.one', FALSE, NULL),
        ('demo.pending.two@example.com', 'TESTAA', 'pending.two', FALSE, NULL),
        ('demo.pending.three@example.com', 'TESTBB', 'pending.three', FALSE, NULL),
        ('map.partial@example.com', 'MAPAAA', 'map.partial', TRUE, 'avatar-images/white_old-black_detective.png'),
        ('map.complete@example.com', 'MAPAAB', 'map.complete', TRUE, 'avatar-images/brown_old-blond_detective.png'),
        ('rank.two@example.com', 'MAPAAB', 'rank.two', TRUE, 'avatar-images/white_redditor-ginger_regular-joe.png'),
        ('rank.three@example.com', 'MAPAAC', 'rank.three', TRUE, 'avatar-images/white_2016-white-ginger_detective.png'),
        ('chapter.test@example.com', 'CHAPAA', 'chapter.test', TRUE, 'avatar-images/white_redditor-black_sweater.png')
)
INSERT INTO pupil_classroom_membership (
    account_email,
    classroom_code,
    username,
    membership_confirmed_by_teacher,
    avatar_file_object_id
)
SELECT
    membership_seed.email,
    membership_seed.classroom_code,
    membership_seed.username,
    membership_seed.confirmed,
    file_object.id
FROM membership_seed
LEFT JOIN file_object ON file_object.object_key = membership_seed.avatar_key
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
    unsuccessful_streak,
    current_run_attempts,
    current_run_correct,
    own_notes
) AS (
    VALUES
        ('pupil.map.partial@example.com', 1, 3, 3, 0, 0, 0, 'Nyhetskvartalet er ferdig. Jeg må alltid sjekke kilden.'),
        ('pupil.map.partial@example.com', 2, 3, 3, 0, 0, 0, 'Fotografen er ferdig. Skygger og detaljer avslører mye.'),
        ('pupil.complete@example.com', 1, 3, 3, 0, 0, 0, 'Fullført med full score.'),
        ('pupil.complete@example.com', 2, 3, 3, 0, 0, 0, 'Fullført med full score.'),
        ('pupil.complete@example.com', 3, 5, 5, 0, 0, 0, 'Hele spillet er fullført.'),
        ('pupil.stuck@example.com', 1, 0, 2, 5, 5, 2, 'Jeg står fast på kildesjekken.'),
        ('pupil.weekly@example.com', 1, 1, 3, 0, 0, 0, 'Jeg fant et godt eksempel til Ukens mysterium.'),
        ('pupil.alva@example.com', 1, 2, 3, 0, 0, 0, ''),
        ('pupil.alva@example.com', 2, 1, 2, 0, 0, 0, ''),
        ('pupil.jonas@example.com', 1, 1, 2, 0, 0, 0, ''),
        ('pupil.emma@example.com', 1, 3, 3, 0, 0, 0, ''),
        ('pupil.emma@example.com', 2, 2, 3, 0, 0, 0, ''),
        ('pupil.noah@example.com', 1, 0, 1, 3, 3, 1, ''),
        ('pupil.sara@example.com', 1, 3, 3, 0, 0, 0, ''),
        ('pupil.sara@example.com', 2, 3, 3, 0, 0, 0, ''),
        ('pupil.theodor@example.com', 1, 1, 2, 0, 0, 0, ''),
        ('pupil.lina@example.com', 1, 0, 2, 2, 2, 1, ''),
        ('pupil.mathias@example.com', 1, 3, 3, 0, 0, 0, ''),
        ('pupil.mathias@example.com', 2, 3, 2, 0, 0, 0, ''),
        ('demo.alice@example.com', 1, 3, 3, 0, 0, 0, ''),
        ('demo.alice@example.com', 2, 0, 2, 1, 1, 1, ''),
        ('demo.bob@example.com', 1, 0, 1, 1, 1, 0, ''),
        ('demo.carla@example.com', 1, 3, 3, 0, 0, 0, ''),
        ('demo.carla@example.com', 2, 3, 3, 0, 0, 0, ''),
        ('demo.carla@example.com', 3, 0, 2, 1, 1, 0, ''),
        ('map.partial@example.com', 1, 3, 3, 0, 0, 0, 'Jeg har tre stjerner i første kapittel.'),
        ('map.partial@example.com', 2, 3, 3, 0, 0, 0, 'Jeg har tre stjerner i andre kapittel.'),
        ('map.complete@example.com', 1, 3, 3, 0, 0, 0, ''),
        ('map.complete@example.com', 2, 3, 3, 0, 0, 0, ''),
        ('map.complete@example.com', 3, 5, 5, 0, 0, 0, ''),
        ('rank.two@example.com', 1, 1, 3, 0, 0, 0, ''),
        ('rank.two@example.com', 2, 0, 1, 1, 1, 0, ''),
        ('rank.three@example.com', 1, 0, 1, 1, 1, 0, '')
),
chapter_tips (chapter_id, tips) AS (
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
    current_run_attempts,
    current_run_correct,
    notebook_json
)
SELECT
    progression_seed.email,
    progression_seed.chapter_id,
    progression_seed.successful_runs,
    progression_seed.highscore,
    progression_seed.unsuccessful_streak,
    progression_seed.current_run_attempts,
    progression_seed.current_run_correct,
    jsonb_build_object(
        'tips',
        CASE
            WHEN progression_seed.successful_runs >= 1
                THEN to_jsonb(COALESCE(chapter_tips.tips, ARRAY[]::TEXT[]))
            WHEN progression_seed.email = 'pupil.stuck@example.com'
                THEN to_jsonb(ARRAY['Se nøye på kilden før du bestemmer deg.']::TEXT[])
            ELSE '[]'::jsonb
        END,
        'ownNotes',
        progression_seed.own_notes
    )
FROM progression_seed
LEFT JOIN chapter_tips ON chapter_tips.chapter_id = progression_seed.chapter_id
ON CONFLICT (email, chapter_id) DO UPDATE
SET
    successful_runs = EXCLUDED.successful_runs,
    highscore = EXCLUDED.highscore,
    unsuccessful_streak = EXCLUDED.unsuccessful_streak,
    current_run_attempts = EXCLUDED.current_run_attempts,
    current_run_correct = EXCLUDED.current_run_correct,
    notebook_json = EXCLUDED.notebook_json;

DELETE FROM weekly_mystery
USING file_object
WHERE weekly_mystery.file_object_id = file_object.id
  AND (
      weekly_mystery.classroom_code IN ('DEMOAA', 'DEMOBB', 'MAPAAA', 'TESTAA', 'TESTBB')
      OR file_object.object_key LIKE 'weekly-mysteries/demo-%'
  );

WITH mystery_seed (
    object_key,
    classroom_code,
    account_email,
    comment,
    approved,
    approved_at,
    created_at
) AS (
    VALUES
        ('weekly-mysteries/demo-fake-news-1.png', 'DEMOAA', 'pupil.weekly@example.com',
         'Denne artikkelen bruker veldig dramatisk overskrift, men bildet virker mistenkelig.',
         TRUE, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day 2 hours'),
        ('weekly-mysteries/demo-crowd-1.png', 'DEMOAA', 'pupil.alva@example.com',
         'Flere ansikter i folkemengden ser nesten like ut.',
         TRUE, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days 1 hour'),
        ('weekly-mysteries/demo-flowers-1.png', 'DEMOAA', 'pupil.jonas@example.com',
         'Blomstene har rare kanter og bakgrunnen virker kunstig.',
         FALSE, NULL, NOW() - INTERVAL '4 hours'),
        ('weekly-mysteries/demo-datacenter-news-1.png', 'DEMOBB', 'pupil.sara@example.com',
         'Denne nyhetssaken ser ut til å blande ekte og falsk informasjon.',
         TRUE, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days 3 hours'),
        ('weekly-mysteries/demo-earth-1.png', 'DEMOBB', 'pupil.theodor@example.com',
         'Jorda ser altfor glatt ut, og detaljene stemmer ikke helt.',
         FALSE, NULL, NOW() - INTERVAL '7 hours'),
        ('weekly-mysteries/demo-building-1.png', 'MAPAAA', 'pupil.map.partial@example.com',
         'Bygningen ser generert ut fordi vinduene ikke følger samme mønster.',
         TRUE, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days 2 hours')
)
INSERT INTO weekly_mystery (
    file_object_id,
    classroom_code,
    account_email,
    comment,
    approved,
    approved_at,
    created_at
)
SELECT
    file_object.id,
    mystery_seed.classroom_code,
    mystery_seed.account_email,
    mystery_seed.comment,
    mystery_seed.approved,
    mystery_seed.approved_at,
    mystery_seed.created_at
FROM mystery_seed
JOIN file_object ON file_object.object_key = mystery_seed.object_key;

DELETE FROM notification
WHERE classroom_code IN ('DEMOAA', 'DEMOBB', 'MAPAAA', 'TESTAA', 'TESTBB');

INSERT INTO notification (
    classroom_code,
    account_email,
    notification_text,
    status_notifications,
    created_at
)
VALUES
    ('DEMOAA', 'pupil.weekly@example.com',
     'Ibrahim Mysterium har sendt inn et nytt bidrag til Ukens mysterium.',
     'UNREAD', NOW() - INTERVAL '4 hours'),
    ('DEMOAA', 'pupil.complete@example.com',
     'Oskar Komplett har fullført Datasenteret for første gang.',
     'UNREAD', NOW() - INTERVAL '1 day'),
    ('DEMOAA', 'pupil.stuck@example.com',
     'Nora Fastlåst står fast på Nyhetskvartalet og kan trenge hjelp.',
     'UNREAD', NOW() - INTERVAL '2 days'),
    ('DEMOAA', 'pupil.pending@example.com',
     'Peder Pending venter på å bli godkjent i Demo 5A.',
     'READ', NOW() - INTERVAL '3 days'),
    ('DEMOBB', 'pupil.theodor@example.com',
     'Theodor Dahl har sendt inn et nytt bidrag til Ukens mysterium.',
     'UNREAD', NOW() - INTERVAL '7 hours'),
    ('MAPAAA', 'pupil.map.partial@example.com',
     'Maja Kart har fullført Fotografen for første gang.',
     'READ', NOW() - INTERVAL '8 days'),
    ('TESTAA', 'demo.alice@example.com',
     'Alice Accepted har bedt om hjelp i Nyhetskvartalet.',
     'UNREAD', NOW() - INTERVAL '2 days'),
    ('TESTAA', 'demo.bob@example.com',
     'Bob Accepted venter på tilbakemelding fra lærer.',
     'UNREAD', NOW() - INTERVAL '1 day');
