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

## Troca de release e rollback reais (2026-11-13)

Release de ensaio `9d3510970e5c` (branch `task/vps-staging-delivery/release-rehearsal`:
topo da stack + main `3bc2d93`, redesign `focus-visual-refresh`, sem migrações).

- Primeira tentativa recusada pela trava do operador: havia um `configure-google`
  interativo aberto pelo usuário. Encerrado com SIGINT por autorização do usuário;
  trava liberada pelo próprio operador, Google seguiu desligado.
- `release`: backup automático e troca em 22 s; bundle novo servido; status UP; 2.587 elegíveis.
- `rollback`: schema conferido (V17 inalterado) e release anterior restabelecida em 14 s.
- `release` de novo: backup automático e volta à versão com o redesign.
- Conferido de fora por HTTPS (navegador, IP liberado): nova interface servida.

## Limites

- Rollback com mudança de schema continua coberto só por teste (recusa e
  orientação de restaurar em alvo separado).
- Backup manual, sem agendamento nem cópia externa automática; a cópia no PC é responsabilidade do usuário.
