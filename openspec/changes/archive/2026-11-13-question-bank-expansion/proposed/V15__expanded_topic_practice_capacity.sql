-- Proposta para revisão: ainda fora do diretório Flyway executável.
-- Amplia somente novas práticas formato 4; não regrava capturas ou comandos.
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
   jsonb_array_length(payload->'items') BETWEEN 1 AND 25)),false));
