-- Спецкурсы по ссылке-приглашению
ALTER TABLE courses ADD COLUMN invite_only BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE courses ADD COLUMN invite_code VARCHAR(20);
CREATE UNIQUE INDEX IF NOT EXISTS ux_courses_invite_code ON courses (invite_code);
