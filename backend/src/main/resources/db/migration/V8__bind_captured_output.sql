-- Aditiva: V1–V7 e snapshots existentes não são reescritos.
-- Mesma canonização parameter-output/v1: contagem e strings UTF-8 com int32 big endian.
CREATE FUNCTION pq_captured_output_digest(item JSONB) RETURNS TEXT LANGUAGE plpgsql IMMUTABLE STRICT AS $$
DECLARE c JSONB:=item#>'{question,content}'; capture JSONB:=item#>'{question,source,capture}';
 fields TEXT[]; alternatives TEXT[]:='{}'; parameter_key TEXT; opt RECORD; text_value TEXT;
 correct_index INTEGER:=-1; matches INTEGER:=0; encoded BYTEA; part BYTEA;
BEGIN
 IF c#>'{statement,resources}' IS DISTINCT FROM '[]'::jsonb OR
    c#>'{resolution,resources}' IS DISTINCT FROM '[]'::jsonb OR
    jsonb_typeof(c#>'{statement,text}') IS DISTINCT FROM 'string' OR
    jsonb_typeof(c#>'{resolution,text}') IS DISTINCT FROM 'string' THEN
  RAISE EXCEPTION 'Texto capturado fora do formato v1' USING ERRCODE='23514';
 END IF;
 FOR opt IN SELECT value,ordinality FROM jsonb_array_elements(c->'alternatives') WITH ORDINALITY LOOP
  text_value:=opt.value#>>'{content,text}';
  IF opt.value#>'{content,resources}' IS DISTINCT FROM '[]'::jsonb OR
     text_value IS NULL OR text_value !~ '^R[$] [1-9][0-9]*,00$' THEN
   RAISE EXCEPTION 'Alternativa capturada fora do formato v1' USING ERRCODE='23514';
  END IF;
  alternatives:=array_append(alternatives,substring(text_value FROM 4 FOR char_length(text_value)-6));
  IF opt.value->>'id'=c->>'correctAlternativeId' THEN correct_index:=opt.ordinality-1;matches:=matches+1; END IF;
 END LOOP;
 IF cardinality(alternatives)<>4 OR matches<>1 THEN
  RAISE EXCEPTION 'Alternativas/correção capturadas inválidas' USING ERRCODE='23514';
 END IF;
 SELECT (capture#>>'{parameters,family}')||':['||string_agg(k||'='||v,', ' ORDER BY k COLLATE "C")||']'
 INTO parameter_key FROM jsonb_each_text(capture#>'{parameters,values}') p(k,v);
 fields:=ARRAY['parameter-output/v1',capture->>'definitionDigest',parameter_key,c#>>'{statement,text}',correct_index::text]
         ||alternatives||ARRAY[c#>>'{resolution,text}'];
 encoded:=int4send(cardinality(fields));
 FOREACH text_value IN ARRAY fields LOOP
  IF text_value IS NULL THEN RAISE EXCEPTION 'Campo canônico ausente' USING ERRCODE='23514'; END IF;
  part:=convert_to(text_value,'UTF8');encoded:=encoded||int4send(octet_length(part))||part;
 END LOOP;
 RETURN encode(sha256(encoded),'hex');
END $$;
-- Recusa legado divergente sem corrigir/regerar conteúdo editorial silenciosamente.
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM pq_instance WHERE payload#>>'{question,source,capture,outputDigest}'
     IS DISTINCT FROM pq_captured_output_digest(payload)) THEN
  RAISE EXCEPTION 'Instância V7 diverge da saída aprovada; integração interrompida' USING ERRCODE='23514';
 END IF;
END $$;
CREATE FUNCTION pq_bind_instance_output() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM pq_instance WHERE item_id=NEW.item_id) THEN RETURN NULL; END IF;
 IF NEW.payload#>>'{question,source,capture,outputDigest}' IS DISTINCT FROM pq_captured_output_digest(NEW.payload) THEN
  RAISE EXCEPTION 'Conteúdo não corresponde à saída aprovada' USING ERRCODE='23514';
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER pq_instance_output AFTER INSERT ON pq_instance DEFERRABLE INITIALLY DEFERRED
 FOR EACH ROW EXECUTE FUNCTION pq_bind_instance_output();
