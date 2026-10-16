-- Classificação é semântica de conjunto para cargos; referências editoriais são ordenadas.
CREATE FUNCTION pq_captured_metadata_matches(item JSONB, definition JSONB) RETURNS BOOLEAN
LANGUAGE sql IMMUTABLE STRICT AS $$
 SELECT COALESCE(
  (item#>'{question,content,classification}') @> (definition->'classification') AND
  (definition->'classification') @> (item#>'{question,content,classification}') AND
  (item#>'{question,content,references}') = (definition->'references') AND
  (item#>'{question,content,provenance}') = (definition->'provenance'),false)
$$;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM pq_instance i JOIN pq_version v ON v.template_id=i.template_id AND v.version=i.template_version
     WHERE NOT pq_captured_metadata_matches(i.payload,v.payload->'definition')) THEN
  RAISE EXCEPTION 'Metadados existentes divergem da definição aprovada; integração interrompida' USING ERRCODE='23514';
 END IF;
END $$;
CREATE FUNCTION pq_bind_instance_metadata() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE definition JSONB;
BEGIN
 IF NOT EXISTS(SELECT 1 FROM pq_instance WHERE item_id=NEW.item_id) THEN RETURN NULL; END IF;
 SELECT payload->'definition' INTO STRICT definition FROM pq_version
  WHERE template_id=NEW.template_id AND version=NEW.template_version;
 IF NOT pq_captured_metadata_matches(NEW.payload,definition) THEN
  RAISE EXCEPTION 'Metadados capturados divergem da definição aprovada' USING ERRCODE='23514';
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER pq_instance_metadata AFTER INSERT ON pq_instance DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION pq_bind_instance_metadata();
