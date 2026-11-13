-- Origem da revisão por decisão (#177). Anulável: decisões anteriores ficam "não informado",
-- sem inferência. Nenhuma linha histórica é alterada (ADD COLUMN sem default não reescreve linhas).
-- A obrigatoriedade em APPROVE novo pertence ao domínio, que distingue decisão nova de histórico.
ALTER TABLE qb_decision ADD COLUMN review_kind VARCHAR(16)
 CONSTRAINT qb_decision_review_kind_check CHECK (review_kind IN ('HUMAN_PEER','AI_ONLY'));
