-- Additive: existing accounts and immutable study/import evidence keep their identifiers.
ALTER TABLE app_user ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
CREATE TABLE admin_registration (
 id INTEGER PRIMARY KEY CHECK (id=1),
 owner_id UUID REFERENCES app_user(id), username VARCHAR(64) UNIQUE,
 password_hash VARCHAR(512), must_change BOOLEAN NOT NULL DEFAULT TRUE,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, auth_version BIGINT NOT NULL DEFAULT 0 CHECK(auth_version>=0),
 failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK(failed_attempts>=0),
 failure_window TIMESTAMPTZ, blocked_until TIMESTAMPTZ,
 CHECK ((owner_id IS NULL AND username IS NULL AND password_hash IS NULL) OR
        (owner_id IS NOT NULL AND username IS NOT NULL AND password_hash IS NOT NULL))
);
INSERT INTO admin_registration(id) VALUES(1);
-- Account row is the barrier shared by linking and all authenticated writes.
CREATE FUNCTION administrative_account_write_guard() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE actor UUID; available BOOLEAN;
BEGIN
 IF TG_TABLE_NAME='qb_import_receipt' THEN actor=NEW.actor;
 ELSIF NEW.owner_kind='ACCOUNT' THEN actor=NEW.owner_id;
 ELSE RETURN NEW; END IF;
 SELECT active INTO available FROM app_user WHERE id=actor FOR SHARE;
 IF available IS FALSE THEN
   RAISE EXCEPTION 'Inactive account' USING ERRCODE='23514', CONSTRAINT='active_account_write';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER me_attempt_active_account BEFORE INSERT OR UPDATE ON me_attempt
 FOR EACH ROW EXECUTE FUNCTION administrative_account_write_guard();
CREATE TRIGGER me_command_active_account BEFORE INSERT ON me_command
 FOR EACH ROW EXECUTE FUNCTION administrative_account_write_guard();
CREATE TRIGGER qb_import_active_account BEFORE INSERT ON qb_import_receipt
 FOR EACH ROW EXECUTE FUNCTION administrative_account_write_guard();
