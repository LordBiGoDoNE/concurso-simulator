-- Aditiva: nenhuma questão, conta ou sessão preexistente é alterada.
CREATE TABLE me_attempt (
 id UUID PRIMARY KEY, owner_kind VARCHAR(7) NOT NULL, owner_id UUID NOT NULL,
 account_id UUID REFERENCES app_user(id), created_at TIMESTAMPTZ NOT NULL,
 finished_at TIMESTAMPTZ, expires_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
 payload JSONB NOT NULL CHECK (jsonb_typeof(payload)='object'),
 CONSTRAINT me_attempt_owner CHECK (
  (owner_kind='ACCOUNT' AND account_id IS NOT NULL AND account_id=owner_id AND expires_at IS NULL) OR
  (owner_kind='VISITOR' AND account_id IS NULL AND expires_at IS NOT NULL AND expires_at=created_at+interval '24 hours')),
 CHECK (finished_at IS NULL OR (finished_at>=created_at AND (expires_at IS NULL OR finished_at<expires_at))),
 CHECK ((payload->>'version') IS NOT NULL AND (payload->>'version')::bigint=version),
 UNIQUE(id,owner_kind,owner_id)
);
CREATE TABLE me_item (
 id UUID PRIMARY KEY, attempt_id UUID NOT NULL REFERENCES me_attempt ON DELETE CASCADE,
 position INTEGER NOT NULL CHECK (position BETWEEN 0 AND 99), question_id UUID NOT NULL, revision_id UUID NOT NULL,
 payload JSONB NOT NULL CHECK (jsonb_typeof(payload)='object'),
 UNIQUE(attempt_id,position), UNIQUE(attempt_id,question_id),
 FOREIGN KEY(question_id,revision_id) REFERENCES qb_revision(question_id,id)
);
CREATE TABLE me_command (
 id UUID PRIMARY KEY, owner_kind VARCHAR(7) NOT NULL, owner_id UUID NOT NULL,
 operation_id UUID NOT NULL, attempt_id UUID NOT NULL,
 payload_hash VARCHAR(64) NOT NULL CHECK (payload_hash ~ '^[0-9a-f]{64}$'),
 outcome JSONB NOT NULL CHECK (jsonb_typeof(outcome)='object'),
 CONSTRAINT me_command_operation_key UNIQUE(owner_kind,owner_id,operation_id),
 FOREIGN KEY(attempt_id,owner_kind,owner_id) REFERENCES me_attempt(id,owner_kind,owner_id) ON DELETE CASCADE
);
CREATE INDEX me_attempt_history_idx ON me_attempt(owner_kind,owner_id,created_at DESC,id DESC);
CREATE INDEX me_attempt_expiry_idx ON me_attempt(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX me_item_attempt_idx ON me_item(attempt_id);
CREATE INDEX me_command_attempt_idx ON me_command(attempt_id);
CREATE FUNCTION me_protect_attempt() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.finished_at IS NOT NULL OR NEW.version<>OLD.version+1 OR
 (NEW.id,NEW.owner_kind,NEW.owner_id,NEW.account_id,NEW.created_at,NEW.expires_at,
  NEW.payload->'items',NEW.payload->'composition',NEW.payload->'policy') IS DISTINCT FROM
 (OLD.id,OLD.owner_kind,OLD.owner_id,OLD.account_id,OLD.created_at,OLD.expires_at,
  OLD.payload->'items',OLD.payload->'composition',OLD.payload->'policy') THEN
  RAISE EXCEPTION 'Identidade/snapshot/estado imutável de tentativa' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER me_attempt_immutable BEFORE UPDATE ON me_attempt FOR EACH ROW EXECUTE FUNCTION me_protect_attempt();
-- DELETE continua permitido para expiração, com cascata somente dos dados temporários.
CREATE TRIGGER me_item_immutable BEFORE UPDATE ON me_item FOR EACH ROW EXECUTE FUNCTION qb_immutable_history();
CREATE TRIGGER me_command_immutable BEFORE UPDATE ON me_command FOR EACH ROW EXECUTE FUNCTION qb_immutable_history();
