-- Adds the educators table. There is no registration endpoint: every row here is inserted by
-- EducatorSeeder from the `app.educators` configuration list, never by a user-facing request.

CREATE TABLE educators (
    id            uuid                        NOT NULL,
    email         character varying(255)      NOT NULL,
    display_name  character varying(255)      NOT NULL,
    password_hash character varying(255)      NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_educators PRIMARY KEY (id),
    CONSTRAINT uq_educators_email UNIQUE (email)
);
