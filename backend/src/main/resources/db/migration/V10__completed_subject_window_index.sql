-- Aditiva, sem regravar payloads/itens/comandos V5 ou formato 2.
-- A janela filtra proprietário/cargo capturado antes de ordenar por conclusão.
CREATE INDEX me_completed_subject_window_idx
 ON me_attempt(owner_id,(payload->'composition'->>'role'),finished_at DESC,id DESC)
 WHERE owner_kind='ACCOUNT' AND finished_at IS NOT NULL;
