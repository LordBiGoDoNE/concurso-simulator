-- Catálogo fixo. Nenhuma questão/importação/aprovação editorial é inserida por Flyway.
CREATE TABLE qb_discipline (id VARCHAR(64) PRIMARY KEY);
INSERT INTO qb_discipline VALUES ('portugues'), ('matematica-raciocinio'), ('informatica'),
 ('conhecimentos-administrativos'), ('conhecimentos-escolares');
CREATE TABLE qb_role (id VARCHAR(64) PRIMARY KEY, contest_id VARCHAR(64) NOT NULL CHECK (contest_id = 'limeira-01-2026'));
INSERT INTO qb_role VALUES ('assistente-administrativo', 'limeira-01-2026'), ('secretario-de-escola', 'limeira-01-2026');
CREATE TABLE qb_topic (
 id VARCHAR(16) PRIMARY KEY, discipline_id VARCHAR(64) NOT NULL REFERENCES qb_discipline,
 UNIQUE (id, discipline_id)
);
INSERT INTO qb_topic VALUES
 ('P01','portugues'),('P02','portugues'),('P03','portugues'),('P04','portugues'),('P05','portugues'),
 ('P06','portugues'),('P07','portugues'),('P08','portugues'),('P09','portugues'),('P10','portugues'),
 ('M01','matematica-raciocinio'),('M02','matematica-raciocinio'),('M03','matematica-raciocinio'),
 ('M04','matematica-raciocinio'),('M05','matematica-raciocinio'),('M06','matematica-raciocinio'),
 ('M07','matematica-raciocinio'),('M08','matematica-raciocinio'),('M09','matematica-raciocinio'),
 ('M10','matematica-raciocinio'),('M11','matematica-raciocinio'),('M12','matematica-raciocinio'),
 ('I01','informatica'),('I02','informatica'),('I03','informatica'),('I04','informatica'),('I05','informatica'),
 ('I06','informatica'),('I07','informatica'),('I08','informatica'),('I09','informatica'),('I10','informatica'),
 ('A01','conhecimentos-administrativos'),('A02','conhecimentos-administrativos'),
 ('A03','conhecimentos-administrativos'),('A04','conhecimentos-administrativos'),
 ('A05','conhecimentos-administrativos'),('A06','conhecimentos-administrativos'),
 ('A07','conhecimentos-administrativos'),('A08','conhecimentos-administrativos'),
 ('S01','conhecimentos-escolares'),('S02','conhecimentos-escolares'),('S03','conhecimentos-escolares'),
 ('S04','conhecimentos-escolares'),('S05','conhecimentos-escolares'),('S06','conhecimentos-escolares'),
 ('S07','conhecimentos-escolares'),('S08','conhecimentos-escolares'),('S09','conhecimentos-escolares'),
 ('S10','conhecimentos-escolares'),('S11','conhecimentos-escolares');

CREATE TABLE qb_question (
 id UUID PRIMARY KEY, origin_namespace VARCHAR(100) NOT NULL CHECK (btrim(origin_namespace) <> ''),
 origin_key VARCHAR(200) NOT NULL CHECK (btrim(origin_key) <> ''), version BIGINT NOT NULL DEFAULT 0,
 eligible_revision_id UUID,
 CONSTRAINT qb_question_origin_key UNIQUE (origin_namespace, origin_key)
);
CREATE TABLE qb_revision (
 id UUID PRIMARY KEY, question_id UUID NOT NULL REFERENCES qb_question,
 revision_number INTEGER NOT NULL CHECK (revision_number > 0), fingerprint VARCHAR(64) NOT NULL CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
 payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
 discipline_id VARCHAR(64) NOT NULL, topic_id VARCHAR(16) NOT NULL,
 correct_alternative_id UUID NOT NULL, status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT','IN_REVIEW','APPROVED')),
 CONSTRAINT qb_revision_number UNIQUE (question_id, revision_number), UNIQUE (question_id, id),
 FOREIGN KEY (topic_id, discipline_id) REFERENCES qb_topic (id, discipline_id)
);
CREATE TABLE qb_alternative (
 revision_id UUID NOT NULL REFERENCES qb_revision, option_id UUID NOT NULL, position INTEGER NOT NULL CHECK (position >= 0),
 PRIMARY KEY (revision_id, option_id), UNIQUE (revision_id, position)
);
ALTER TABLE qb_revision ADD CONSTRAINT qb_correct_alternative_fk
 FOREIGN KEY (id, correct_alternative_id) REFERENCES qb_alternative (revision_id, option_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE qb_question ADD CONSTRAINT qb_eligible_revision_fk
 FOREIGN KEY (id, eligible_revision_id) REFERENCES qb_revision (question_id, id) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE qb_revision_role (
 revision_id UUID NOT NULL REFERENCES qb_revision, role_id VARCHAR(64) NOT NULL REFERENCES qb_role,
 PRIMARY KEY (revision_id, role_id)
);
CREATE TABLE qb_reference (
 revision_id UUID NOT NULL REFERENCES qb_revision, position INTEGER NOT NULL CHECK (position >= 0),
 name VARCHAR(1000) NOT NULL CHECK (btrim(name) <> ''), url VARCHAR(4000) NOT NULL,
 provision VARCHAR(4000), checked_on DATE,
 PRIMARY KEY (revision_id, position), CHECK ((provision IS NULL) = (checked_on IS NULL))
);
CREATE TABLE qb_decision (
 id UUID PRIMARY KEY, question_id UUID NOT NULL REFERENCES qb_question, sequence INTEGER NOT NULL CHECK (sequence >= 0),
 revision_id UUID NOT NULL, action VARCHAR(32) NOT NULL CHECK (action IN ('SUBMIT','RETURN_TO_DRAFT','APPROVE','WITHDRAW')),
 fingerprint VARCHAR(64) NOT NULL CHECK (fingerprint ~ '^[0-9a-f]{64}$'), responsible VARCHAR(500) NOT NULL CHECK (btrim(responsible) <> ''),
 decided_at TIMESTAMP WITH TIME ZONE NOT NULL, checklist VARCHAR(300) NOT NULL, rationale VARCHAR(4000) NOT NULL CHECK (btrim(rationale) <> ''),
 UNIQUE (question_id, sequence), FOREIGN KEY (question_id, revision_id) REFERENCES qb_revision (question_id, id)
);

-- FK adiada permite inserir revisão/opções e trocar ponteiro na mesma unidade atômica.
CREATE FUNCTION qb_validate_eligible() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS (SELECT 1 FROM qb_question q JOIN qb_revision r ON r.id=q.eligible_revision_id WHERE r.status <> 'APPROVED') THEN
  RAISE EXCEPTION 'Revisão elegível deve estar aprovada' USING ERRCODE='23514', CONSTRAINT='qb_eligible_approved';
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER qb_eligible_check AFTER INSERT OR UPDATE ON qb_question
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION qb_validate_eligible();
CREATE CONSTRAINT TRIGGER qb_eligible_status_check AFTER UPDATE ON qb_revision
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION qb_validate_eligible();
CREATE FUNCTION qb_immutable_revision() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP = 'DELETE' THEN RAISE EXCEPTION 'Revisão não pode ser apagada' USING ERRCODE='23514'; END IF;
 IF (NEW.id,NEW.question_id,NEW.revision_number,NEW.fingerprint,NEW.payload,NEW.discipline_id,NEW.topic_id,NEW.correct_alternative_id)
 IS DISTINCT FROM (OLD.id,OLD.question_id,OLD.revision_number,OLD.fingerprint,OLD.payload,OLD.discipline_id,OLD.topic_id,OLD.correct_alternative_id) THEN
  RAISE EXCEPTION 'Conteúdo de revisão imutável' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER qb_revision_immutable BEFORE UPDATE OR DELETE ON qb_revision FOR EACH ROW EXECUTE FUNCTION qb_immutable_revision();
CREATE FUNCTION qb_immutable_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Registro imutável' USING ERRCODE='23514'; END $$;
CREATE TRIGGER qb_decision_immutable BEFORE UPDATE OR DELETE ON qb_decision FOR EACH ROW EXECUTE FUNCTION qb_immutable_history();
CREATE TRIGGER qb_alternative_immutable BEFORE UPDATE OR DELETE ON qb_alternative FOR EACH ROW EXECUTE FUNCTION qb_immutable_history();
CREATE TRIGGER qb_reference_immutable BEFORE UPDATE OR DELETE ON qb_reference FOR EACH ROW EXECUTE FUNCTION qb_immutable_history();
CREATE TRIGGER qb_role_immutable BEFORE UPDATE OR DELETE ON qb_revision_role FOR EACH ROW EXECUTE FUNCTION qb_immutable_history();
CREATE INDEX qb_revision_question_idx ON qb_revision(question_id);
