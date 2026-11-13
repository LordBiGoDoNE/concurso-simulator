-- Novas práticas por assunto; V1–V13 e capturas/comandos existentes permanecem intactos.
ALTER TABLE me_attempt DROP CONSTRAINT me_attempt_purpose_valid;
ALTER TABLE me_attempt ADD CONSTRAINT me_attempt_purpose_valid CHECK (COALESCE((
 (coalesce(payload->>'purpose','SIMULADO')='SIMULADO' AND NOT (payload ? 'practiceTopic') AND
   (NOT (payload ? 'format') OR payload->>'format'='2')) OR
 (payload->>'purpose'='PRATICA_AULA' AND payload->>'format'='3' AND
   jsonb_typeof(payload->'practiceTopic')='string' AND
   jsonb_array_length(payload->'items') BETWEEN 1 AND 5 AND
   payload#>>'{composition,useParameterizedQuestions}'='false') OR
 (payload->>'purpose'='PRATICA_AULA' AND payload->>'format'='4' AND
   jsonb_typeof(payload->'practiceTopic')='string' AND
   payload->'composition'='null'::jsonb AND
   jsonb_array_length(payload->'items') BETWEEN 1 AND 5)),false));
-- O assunto precisa existir também para o formato novo. As travas de origem/fonte,
-- identidade dos itens, modalidade e imutabilidade da V11 continuam ativas.
CREATE FUNCTION lp_check_topic_capture() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.payload->>'format'='4' THEN
  IF NOT EXISTS(SELECT 1 FROM qb_topic WHERE id=NEW.payload->>'practiceTopic') THEN
   RAISE EXCEPTION 'Assunto da captura inexistente' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER lp_topic_capture BEFORE INSERT ON me_attempt FOR EACH ROW EXECUTE FUNCTION lp_check_topic_capture();

-- Validar o conjunto ao commit permite a inserção dos filhos na mesma transação.
CREATE FUNCTION lp_bind_topic_items() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE captured JSONB; stored JSONB; capture_id UUID;
BEGIN
 IF TG_TABLE_NAME='me_attempt' THEN capture_id:=NEW.id; ELSE capture_id:=NEW.attempt_id; END IF;
 SELECT payload INTO captured FROM me_attempt WHERE id=capture_id;
 IF NOT FOUND THEN RETURN NULL; END IF;
 IF captured->>'format' IS DISTINCT FROM '4' THEN RETURN NULL; END IF;
 SELECT coalesce(jsonb_agg(i.payload ORDER BY i.position),'[]'::jsonb) INTO stored FROM me_item i WHERE i.attempt_id=capture_id;
 IF stored IS DISTINCT FROM captured->'items' OR EXISTS(
  SELECT 1 FROM jsonb_array_elements(captured->'items') item WHERE
   item#>>'{question,purpose}' IS DISTINCT FROM 'PRATICA_AULA' OR
   item#>>'{question,source,kind}' IS DISTINCT FROM 'FIXED' OR
   item#>>'{question,content,classification,topic}' IS DISTINCT FROM captured->>'practiceTopic') THEN
  RAISE EXCEPTION 'Itens divergem da captura por assunto' USING ERRCODE='23514';
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER lp_topic_items AFTER INSERT ON me_attempt DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION lp_bind_topic_items();
CREATE CONSTRAINT TRIGGER lp_topic_item_capture AFTER INSERT ON me_item DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION lp_bind_topic_items();
