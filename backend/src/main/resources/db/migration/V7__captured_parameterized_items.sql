-- Aditiva. Revisões reais e triggers V5/V6 permanecem intactos.
CREATE TABLE pq_instance (
 item_id UUID PRIMARY KEY REFERENCES me_item(id) ON DELETE CASCADE,
 template_id UUID NOT NULL, template_version INTEGER NOT NULL,
 payload JSONB NOT NULL CHECK(jsonb_typeof(payload)='object'),
 FOREIGN KEY(template_id,template_version) REFERENCES pq_version(template_id,version)
);
CREATE FUNCTION pq_protect_instance() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' OR EXISTS(SELECT 1 FROM me_item WHERE id=OLD.item_id) THEN
  RAISE EXCEPTION 'Instância imutável; remoção somente com item' USING ERRCODE='23514';
 END IF;
 RETURN OLD;
END $$;
CREATE TRIGGER pq_instance_immutable BEFORE UPDATE OR DELETE ON pq_instance FOR EACH ROW EXECUTE FUNCTION pq_protect_instance();

-- Ambos os inserts ocorrem na mesma transação; nenhuma metade pode confirmar sozinha.
CREATE FUNCTION pq_check_instance() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE item me_item%ROWTYPE; instance pq_instance%ROWTYPE; template pq_version%ROWTYPE; capture JSONB;
BEGIN
 IF TG_TABLE_NAME='me_item' THEN SELECT * INTO item FROM me_item WHERE id=NEW.id;
 ELSE SELECT * INTO item FROM me_item WHERE id=NEW.item_id; END IF;
 IF NOT FOUND THEN RETURN NULL; END IF; -- Cascata de expiração na mesma transação.
 SELECT * INTO instance FROM pq_instance WHERE item_id=item.id;
 IF item.payload#>>'{question,source,kind}'='PARAMETERIZED' THEN
  IF instance.item_id IS NULL OR instance.payload IS DISTINCT FROM item.payload THEN
   RAISE EXCEPTION 'Captura de instância ausente/divergente' USING ERRCODE='23514';
  END IF;
  capture=item.payload#>'{question,source,capture}';
  SELECT * INTO STRICT template FROM pq_version WHERE template_id=instance.template_id AND version=instance.template_version;
  IF (capture->>'templateId', (capture->>'templateVersion')::integer, capture->>'definitionDigest',
      capture->>'domainDigest', capture->>'manifestDigest', capture->>'baseRevisionId',capture->>'baseFingerprint',
      item.question_id,item.revision_id) IS DISTINCT FROM
     (template.template_id::text,template.version,template.definition_digest,template.domain_digest,
      template.manifest_digest,template.base_revision_id::text,template.base_fingerprint,
      template.base_question_id,template.base_revision_id) THEN
   RAISE EXCEPTION 'Fonte da instância diverge do template/base' USING ERRCODE='23514';
  END IF;
  IF capture->>'renderedFingerprint' IS DISTINCT FROM item.payload#>>'{question,fingerprint}' OR
     capture->>'generatorVersion' IS DISTINCT FROM template.payload#>>'{definition,generatorVersion}' OR
     NOT EXISTS(SELECT 1 FROM jsonb_array_elements(template.payload#>'{manifest,entries}') e
       WHERE e->'parameters'=capture->'parameters' AND e->>'outputDigest'=capture->>'outputDigest') THEN
   RAISE EXCEPTION 'Tupla/saída/fingerprint fora da captura aprovada' USING ERRCODE='23514';
  END IF;
 ELSIF instance.item_id IS NOT NULL THEN
  RAISE EXCEPTION 'Item fixo com instância' USING ERRCODE='23514';
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER pq_item_capture AFTER INSERT ON me_item DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION pq_check_instance();
CREATE CONSTRAINT TRIGGER pq_instance_capture AFTER INSERT ON pq_instance DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION pq_check_instance();
