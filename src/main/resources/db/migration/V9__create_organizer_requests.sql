CREATE TYPE request_status AS ENUM ('PENDING', 'APPROVED', 'DECLINED');

CREATE TABLE organizer_requests (
                                    id           BIGSERIAL     PRIMARY KEY,
                                    user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                    name         VARCHAR(100)  NOT NULL,
                                    email        VARCHAR(255)  NOT NULL,
                                    phone        VARCHAR(30)   NOT NULL,
                                    message      TEXT          NOT NULL,
                                    status       request_status NOT NULL DEFAULT 'PENDING',
                                    reviewed_by  BIGINT        REFERENCES users (id) ON DELETE SET NULL,
                                    review_note  TEXT,
                                    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
                                    reviewed_at  TIMESTAMP
);

CREATE INDEX idx_org_req_user_id ON organizer_requests (user_id);
CREATE INDEX idx_org_req_status  ON organizer_requests (status);