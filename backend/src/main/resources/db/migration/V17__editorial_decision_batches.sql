-- Recibos de decisões editoriais em lote (#177). Somente evidência privada; nenhuma decisão
-- histórica é alterada. Cada item aponta para a decisão gravada na mesma unidade atômica.
CREATE TABLE qb_decision_batch (
 nonce UUID PRIMARY KEY,
 destination VARCHAR(100) NOT NULL,
 actor UUID NOT NULL,
 committed_at TIMESTAMP WITH TIME ZONE NOT NULL,
 action VARCHAR(32) NOT NULL CHECK (action IN ('SUBMIT','APPROVE')),
 review_kind VARCHAR(16) CHECK (review_kind IN ('HUMAN_PEER','AI_ONLY')),
 digest VARCHAR(64) NOT NULL CHECK (digest ~ '^[0-9a-f]{64}$'),
 payload JSONB NOT NULL CHECK (jsonb_typeof(payload)='object'),
 CHECK (action <> 'APPROVE' OR review_kind IS NOT NULL)
);
CREATE TABLE qb_decision_batch_item (
 nonce UUID NOT NULL REFERENCES qb_decision_batch,
 question_id UUID NOT NULL,
 decision_sequence INTEGER NOT NULL,
 PRIMARY KEY (nonce, question_id),
 UNIQUE (question_id, decision_sequence),
 FOREIGN KEY (question_id, decision_sequence) REFERENCES qb_decision (question_id, sequence)
);
CREATE TRIGGER qb_decision_batch_immutable BEFORE UPDATE OR DELETE ON qb_decision_batch FOR EACH ROW EXECUTE FUNCTION qi_immutable();
CREATE TRIGGER qb_decision_batch_item_immutable BEFORE UPDATE OR DELETE ON qb_decision_batch_item FOR EACH ROW EXECUTE FUNCTION qi_immutable();
