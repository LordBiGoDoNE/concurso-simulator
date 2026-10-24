-- Reserva explícita: não reinterpretar uma origem anterior como novo acervo.
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM qb_question WHERE origin_namespace='lesson-practice-v1') THEN
  RAISE EXCEPTION 'Origem reservada de prática já existia; migração interrompida' USING ERRCODE='23514';
 END IF;
END $$;
-- Classificação aditiva; a nova trava abaixo torna a origem imutável também no banco.
ALTER TABLE qb_question ADD COLUMN purpose VARCHAR(16) GENERATED ALWAYS AS
 (CASE WHEN origin_namespace='lesson-practice-v1' THEN 'PRATICA_AULA' ELSE 'SIMULADO' END) STORED;
CREATE INDEX qb_question_purpose_idx ON qb_question(purpose,eligible_revision_id);
CREATE INDEX me_attempt_mode_history_idx ON me_attempt(owner_kind,owner_id,
 (coalesce(payload->>'purpose','SIMULADO')),created_at DESC,id DESC);
ALTER TABLE me_attempt ADD CONSTRAINT me_attempt_purpose_valid CHECK (COALESCE((
 (coalesce(payload->>'purpose','SIMULADO')='SIMULADO' AND NOT (payload ? 'practiceTopic') AND
   (NOT (payload ? 'format') OR payload->>'format'='2')) OR
 (payload->>'purpose'='PRATICA_AULA' AND payload->>'format'='3' AND
   jsonb_typeof(payload->'practiceTopic')='string' AND
   jsonb_array_length(payload->'items') BETWEEN 1 AND 5 AND
   payload#>>'{composition,useParameterizedQuestions}'='false')),false));
CREATE FUNCTION lp_protect_context() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF (NEW.payload->'purpose',NEW.payload->'practiceTopic',NEW.payload->'format') IS DISTINCT FROM
    (OLD.payload->'purpose',OLD.payload->'practiceTopic',OLD.payload->'format') THEN
  RAISE EXCEPTION 'Modalidade/assunto/formato imutáveis' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER lp_attempt_context BEFORE UPDATE ON me_attempt FOR EACH ROW EXECUTE FUNCTION lp_protect_context();
CREATE FUNCTION lp_check_pool() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE actual_purpose TEXT; expected_purpose TEXT; topic TEXT; practice_topic TEXT;
BEGIN
 SELECT q.purpose INTO STRICT actual_purpose FROM qb_question q WHERE q.id=NEW.question_id;
 SELECT coalesce(a.payload->>'purpose','SIMULADO'),a.payload->>'practiceTopic'
 INTO STRICT expected_purpose,practice_topic FROM me_attempt a WHERE a.id=NEW.attempt_id;
 IF actual_purpose IS DISTINCT FROM expected_purpose OR
   coalesce(NEW.payload#>>'{question,purpose}','SIMULADO') IS DISTINCT FROM expected_purpose THEN
  RAISE EXCEPTION 'Base de outro acervo' USING ERRCODE='23514';
 END IF;
 IF expected_purpose='PRATICA_AULA' THEN
  SELECT r.topic_id INTO STRICT topic FROM qb_revision r WHERE r.id=NEW.revision_id AND r.question_id=NEW.question_id;
  IF topic IS DISTINCT FROM practice_topic OR NEW.payload#>>'{question,source,kind}' IS DISTINCT FROM 'FIXED' THEN
   RAISE EXCEPTION 'Assunto/fonte inválidos para prática' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER lp_item_pool AFTER INSERT ON me_item DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION lp_check_pool();

CREATE FUNCTION lp_protect_origin() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF (NEW.origin_namespace,NEW.origin_key) IS DISTINCT FROM (OLD.origin_namespace,OLD.origin_key) THEN
  RAISE EXCEPTION 'Origem/destinação da base imutáveis' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER lp_question_origin BEFORE UPDATE ON qb_question FOR EACH ROW EXECUTE FUNCTION lp_protect_origin();
