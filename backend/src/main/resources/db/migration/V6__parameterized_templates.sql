-- Aditiva: templates privados não alteram acervo, contas, sessões ou tentativas V5.
CREATE TABLE pq_family (
 id UUID PRIMARY KEY, base_question_id UUID NOT NULL REFERENCES qb_question,
 kind VARCHAR(32) NOT NULL CHECK (kind IN ('DIRECT_PROPORTION','PERCENTAGE','SIMPLE_INTEREST')),
 eligible_version INTEGER, version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0),
 CONSTRAINT pq_family_base_key UNIQUE(base_question_id), UNIQUE(id,base_question_id)
);
CREATE TABLE pq_version (
 id UUID PRIMARY KEY, template_id UUID NOT NULL, version INTEGER NOT NULL CHECK(version>0),
 base_question_id UUID NOT NULL, base_revision_id UUID NOT NULL,
 definition_digest VARCHAR(64) NOT NULL CHECK(definition_digest ~ '^[0-9a-f]{64}$'),
 domain_digest VARCHAR(64) NOT NULL CHECK(domain_digest ~ '^[0-9a-f]{64}$'),
 manifest_digest VARCHAR(64) NOT NULL CHECK(manifest_digest ~ '^[0-9a-f]{64}$'),
 base_fingerprint VARCHAR(64) NOT NULL CHECK(base_fingerprint ~ '^[0-9a-f]{64}$'),
 payload JSONB NOT NULL CHECK(jsonb_typeof(payload)='object'),
 status VARCHAR(16) NOT NULL CHECK(status IN ('DRAFT','IN_REVIEW','APPROVED')),
 UNIQUE(template_id,version),
 FOREIGN KEY(template_id,base_question_id) REFERENCES pq_family(id,base_question_id),
 FOREIGN KEY(base_question_id,base_revision_id) REFERENCES qb_revision(question_id,id)
);
ALTER TABLE pq_family ADD CONSTRAINT pq_eligible_version_fk FOREIGN KEY(id,eligible_version)
 REFERENCES pq_version(template_id,version) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE pq_decision (
 id UUID PRIMARY KEY, template_id UUID NOT NULL, sequence INTEGER NOT NULL CHECK(sequence>=0),
 version INTEGER NOT NULL, operation_id UUID NOT NULL,
 payload JSONB NOT NULL CHECK(jsonb_typeof(payload)='object'),
 UNIQUE(template_id,sequence), UNIQUE(template_id,operation_id),
 FOREIGN KEY(template_id,version) REFERENCES pq_version(template_id,version)
);
CREATE FUNCTION pq_protect_family() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Família não pode ser apagada' USING ERRCODE='23514'; END IF;
 IF (NEW.id,NEW.base_question_id,NEW.kind) IS DISTINCT FROM (OLD.id,OLD.base_question_id,OLD.kind) THEN
  RAISE EXCEPTION 'Identidade de família imutável' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER pq_family_identity BEFORE UPDATE OR DELETE ON pq_family FOR EACH ROW EXECUTE FUNCTION pq_protect_family();
CREATE FUNCTION pq_protect_version() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Versão não pode ser apagada' USING ERRCODE='23514'; END IF;
 IF (NEW.id,NEW.template_id,NEW.version,NEW.base_question_id,NEW.base_revision_id,
 NEW.definition_digest,NEW.domain_digest,NEW.manifest_digest,NEW.base_fingerprint,NEW.payload) IS DISTINCT FROM
 (OLD.id,OLD.template_id,OLD.version,OLD.base_question_id,OLD.base_revision_id,
 OLD.definition_digest,OLD.domain_digest,OLD.manifest_digest,OLD.base_fingerprint,OLD.payload) THEN
  RAISE EXCEPTION 'Definição e manifesto imutáveis' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER pq_version_immutable BEFORE UPDATE OR DELETE ON pq_version FOR EACH ROW EXECUTE FUNCTION pq_protect_version();
CREATE TRIGGER pq_decision_immutable BEFORE UPDATE OR DELETE ON pq_decision FOR EACH ROW EXECUTE FUNCTION qb_immutable_history();
CREATE FUNCTION pq_validate_eligible() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM pq_family f JOIN pq_version v ON v.template_id=f.id AND v.version=f.eligible_version WHERE v.status<>'APPROVED') THEN
  RAISE EXCEPTION 'Template elegível deve estar aprovado' USING ERRCODE='23514',CONSTRAINT='pq_eligible_approved';
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER pq_eligible_check AFTER INSERT OR UPDATE ON pq_family DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION pq_validate_eligible();
CREATE CONSTRAINT TRIGGER pq_eligible_status_check AFTER UPDATE ON pq_version DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION pq_validate_eligible();
CREATE INDEX pq_version_base_idx ON pq_version(base_question_id,base_revision_id);
