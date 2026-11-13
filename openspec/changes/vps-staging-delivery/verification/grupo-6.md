# Grupo 6 — backup, atualização e recuperação: evidências

Executado em 2026-11-13 pelo assistente como `concurso-dev`. Produção com release de
ensaio `6079e913de9f`, banco V17 sem questões nem contas.

## Testes automatizados

`test_vps_production_data.py` (14 testes): alvos de produção e de recuperação,
recusa de backup adulterado, de outro tipo, fora do diretório de backups ou com
schema desconhecido; prévia sem escrita; recusa de restauração sobre o próprio
estado de origem, com outra imagem ou confirmação errada; troca de release com
backup antes e release anterior preservada; release fora da main recusada;
rollback recusado quando o schema mudou. Testes locais (`test_local_app*`) sem regressão.

## Ensaio na VPS

- `backup`: 21 s com a API parada e reiniciada; dump e manifesto em 0600, diretório 0700.
- Cópia para o PC por `scp` iniciado do PC, para uma pasta privada fora do repositório;
  SHA-256 conferido contra o manifesto (schema 17, 27 tabelas).
- Segundo backup com 1 sessão ativa, para exercitar a invalidação.
- Alvo `concurso-simulator-prod-recovery-ensaio` (porta 18091, fora do nginx):
  prévia mostra o texto exato de confirmação; confirmação errada recusada;
  restauração validada (fingerprints por tabela, schema 17, 51 assuntos);
  sessões restauradas: 0; cookie anterior recebe 401; nova restauração no mesmo
  destino recusada; dump truncado com manifesto original recusado.
- Produção seguiu saudável durante todo o ensaio. O alvo de ensaio foi parado,
  volume preservado até decisão do usuário.

## Limites

- Troca de release e rollback foram exercitados só por testes: ainda não existe
  uma segunda release com imagens diferentes (os commits desta spec não mudam o
  contexto das imagens). O primeiro uso real será a release da main no corte (grupo 7).
- Backup manual, sem agendamento nem cópia externa automática; a cópia no PC é responsabilidade do usuário.
