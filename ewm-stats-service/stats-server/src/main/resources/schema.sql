CREATE TABLE IF NOT EXISTS hits (
                                    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    app     VARCHAR(255) NOT NULL,
    uri     VARCHAR(512) NOT NULL,
    ip      VARCHAR(45)  NOT NULL,
    created TIMESTAMP    NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_hits_created ON hits (created);
CREATE INDEX IF NOT EXISTS idx_hits_uri ON hits (uri);