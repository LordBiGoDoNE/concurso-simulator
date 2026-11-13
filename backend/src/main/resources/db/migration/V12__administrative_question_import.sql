-- Somente recibos/evidências privados; nenhum seed, aprovação ou publicação.
CREATE TABLE qb_import_receipt (
 nonce UUID PRIMARY KEY,
 destination VARCHAR(100) NOT NULL,
 actor UUID NOT NULL,
 committed_at TIMESTAMP WITH TIME ZONE NOT NULL,
 payload JSONB NOT NULL CHECK (jsonb_typeof(payload)='object')
);
CREATE TABLE qb_import_blob (
 sha256 VARCHAR(64) PRIMARY KEY CHECK (sha256 ~ '^[0-9a-f]{64}$'),
 content BYTEA NOT NULL CHECK (octet_length(content)<=1048576)
);
CREATE TABLE qb_import_file (
 nonce UUID NOT NULL REFERENCES qb_import_receipt,
 path VARCHAR(1000) NOT NULL,
 media_type VARCHAR(64) NOT NULL,
 sha256 VARCHAR(64) NOT NULL REFERENCES qb_import_blob,
 PRIMARY KEY (nonce,path)
);
CREATE TABLE qb_import_revision (
 nonce UUID NOT NULL REFERENCES qb_import_receipt,
 question_id UUID NOT NULL,
 revision_id UUID NOT NULL,
 reused BOOLEAN NOT NULL,
 PRIMARY KEY (nonce,question_id),
 FOREIGN KEY (question_id,revision_id) REFERENCES qb_revision(question_id,id)
);
CREATE FUNCTION qi_immutable() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Evidência de importação imutável' USING ERRCODE='23514';
END $$;
CREATE TRIGGER qi_receipt_immutable BEFORE UPDATE OR DELETE ON qb_import_receipt FOR EACH ROW EXECUTE FUNCTION qi_immutable();
CREATE TRIGGER qi_blob_immutable BEFORE UPDATE OR DELETE ON qb_import_blob FOR EACH ROW EXECUTE FUNCTION qi_immutable();
CREATE TRIGGER qi_file_immutable BEFORE UPDATE OR DELETE ON qb_import_file FOR EACH ROW EXECUTE FUNCTION qi_immutable();
CREATE TRIGGER qi_revision_immutable BEFORE UPDATE OR DELETE ON qb_import_revision FOR EACH ROW EXECUTE FUNCTION qi_immutable();
