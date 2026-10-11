CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE external_identity (
    user_id UUID NOT NULL REFERENCES app_user (id),
    provider VARCHAR(32) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL CHECK (length(subject) > 0),
    CONSTRAINT external_identity_key UNIQUE (provider, issuer, subject)
);

CREATE INDEX external_identity_user_idx ON external_identity (user_id);
