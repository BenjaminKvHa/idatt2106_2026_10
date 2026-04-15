DROP TABLE IF EXISTS
    pupil_chapter_bridge_progression,
    task,
    chapter,
    refresh_token,
    pupil_classroom_membership,
    account_classroom_bridge_teacher,
    file_object,
    classroom,
    account
CASCADE;

DROP TYPE IF EXISTS
    task_difficulty_enum,
    account_role_enum,
    classroom_access_enum
CASCADE;

CREATE TYPE account_role_enum AS ENUM ('TEACHER', 'PUPIL');
CREATE TYPE classroom_access_enum AS ENUM ('ONLY_TEACHERS_IN_CLASS', 'PUBLIC', 'ONLY_CLASS');
CREATE TYPE task_difficulty_enum AS ENUM ('EASY', 'MEDIUM', 'HARD');

CREATE TABLE classroom (
    classroom_code VARCHAR(6) PRIMARY KEY,
    classroom_name VARCHAR(255) NOT NULL,
    classroom_description TEXT
);

CREATE TABLE account (
    email VARCHAR(255) PRIMARY KEY,
    password_data TEXT NOT NULL,
    legal_name VARCHAR(255) NOT NULL,
    account_role account_role_enum NOT NULL,
    -- municaplity VARCHAR(255) NOT NULL
    -- county VARCHAR(255) NOT NULL
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE file_object (
    id BIGSERIAL PRIMARY KEY,
    object_key TEXT UNIQUE NOT NULL,
    uploaded_by VARCHAR(255) REFERENCES account(email) ON DELETE SET NULL,
    classroom_code VARCHAR(6) REFERENCES classroom(classroom_code) ON DELETE CASCADE,
    access_level classroom_access_enum NOT NULL DEFAULT 'PUBLIC',
    file_name VARCHAR(255) NOT NULL
);

CREATE TABLE account_classroom_bridge_teacher (
    account_email VARCHAR(255) REFERENCES account(email) ON DELETE CASCADE,
    classroom_code VARCHAR(6) REFERENCES classroom(classroom_code) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_email, classroom_code)
);

CREATE TABLE pupil_classroom_membership (
    account_email VARCHAR(255) PRIMARY KEY REFERENCES account(email) ON DELETE CASCADE,
    classroom_code VARCHAR(6) REFERENCES classroom(classroom_code) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    username VARCHAR(255),
    membership_confirmed_by_teacher BOOLEAN NOT NULL DEFAULT FALSE, -- if false, then waiting else its confirmed and a part of classroom
    avatar_file_object_id BIGINT REFERENCES file_object(id) ON DELETE SET NULL
);

CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL REFERENCES account(email) ON DELETE CASCADE,
    refresh_token TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chapter (
    id BIGSERIAL PRIMARY KEY,
    chapter_name VARCHAR(255) NOT NULL,
    successful_runs_needed_for_completion INT NOT NULL DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    difficulty task_difficulty_enum NOT NULL,
    chapter_id BIGINT NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
    classroom_code VARCHAR(6) REFERENCES classroom(classroom_code) ON DELETE CASCADE, -- If null, then its a task for everyone
    task_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pupil_chapter_bridge_progression (
    email VARCHAR(255) NOT NULL REFERENCES pupil_classroom_membership(account_email) ON DELETE CASCADE,
    chapter_id BIGINT NOT NULL REFERENCES chapter(id) ON DELETE CASCADE,
    successful_runs INT NOT NULL DEFAULT 0,
    highscore INT NOT NULL DEFAULT 0,
    unsuccessful_streak INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (email, chapter_id)
);