CREATE TABLE IF NOT EXISTS users (
                                     id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     name  VARCHAR(250) NOT NULL,
    email VARCHAR(254) NOT NULL,
    CONSTRAINT uq_email UNIQUE (email)
    );

CREATE TABLE IF NOT EXISTS categories (
                                          id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                          name VARCHAR(50) NOT NULL,
    CONSTRAINT uq_category_name UNIQUE (name)
    );

CREATE TABLE IF NOT EXISTS events (
                                      id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      annotation         VARCHAR(2000) NOT NULL,
    category_id        BIGINT        NOT NULL REFERENCES categories (id),
    created_on         TIMESTAMP     NOT NULL,
    description        VARCHAR(7000) NOT NULL,
    event_date         TIMESTAMP     NOT NULL,
    initiator_id       BIGINT        NOT NULL REFERENCES users (id),
    lat                FLOAT         NOT NULL,
    lon                FLOAT         NOT NULL,
    paid               BOOLEAN       NOT NULL,
    participant_limit  INTEGER       NOT NULL,
    published_on       TIMESTAMP,
    request_moderation BOOLEAN       NOT NULL,
    state              VARCHAR(20)   NOT NULL,
    title              VARCHAR(120)  NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_events_category ON events (category_id);
CREATE INDEX IF NOT EXISTS idx_events_initiator ON events (initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_state_date ON events (state, event_date);

CREATE TABLE IF NOT EXISTS requests (
                                        id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        created      TIMESTAMP   NOT NULL,
                                        event_id     BIGINT      NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    requester_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL,
    CONSTRAINT uq_request UNIQUE (event_id, requester_id)
    );

CREATE INDEX IF NOT EXISTS idx_requests_event ON requests (event_id);

CREATE TABLE IF NOT EXISTS compilations (
                                            id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                            pinned BOOLEAN     NOT NULL,
                                            title  VARCHAR(50) NOT NULL,
    CONSTRAINT uq_compilation_name UNIQUE (title)
    );

CREATE TABLE IF NOT EXISTS compilation_events (
                                                  compilation_id BIGINT NOT NULL REFERENCES compilations (id) ON DELETE CASCADE,
    event_id       BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
    );

CREATE TABLE IF NOT EXISTS comments (
                                        id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        text      VARCHAR(2000) NOT NULL,
    event_id  BIGINT        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    author_id BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created   TIMESTAMP     NOT NULL,
    edited    TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_comments_event ON comments (event_id, created);
CREATE INDEX IF NOT EXISTS idx_comments_author ON comments (author_id);