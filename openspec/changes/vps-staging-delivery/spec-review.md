# Revisão final — vps-staging-delivery

Gate do PR `[SPEC]` #224 (`epic/vps-staging-delivery` → `main`), conforme
`docs/architecture/spec-review.md` e a decisão do usuário de 2026-11-12: revisão
final de código por agente autônomo novo (`.claude/agents/spec-reviewer.md`),
**Claude Opus 5.5, esforço medium**, contexto independente e somente leitura.

| Item | Valor |
| --- | --- |
| Base (main) | `3bc2d93053e2909c9fb0306ae83c0bf50374aed8` |
| Head revisado | `a5fe1d33fe6526d8782f38e61d754daefd27c1d1` |
| Digest do diff (sem os dois arquivos de evidência) | `25b9560a266198cdb97af629aaca4e30f0a733d88745517265dbd23e7c9e4509` |
| Risco | crítico: rede pública, segredos, sessão/OAuth, persistência, restauração, operação editorial |

Histórico: em 2026-10-25 a proposta original (privada, Rocky) teve revisão
documental SOL 6.1 xhigh (#157), sem bloqueadores; aquele registro era pendente
para implementação e é substituído por este gate.

## Escopo e cobertura

Três rodadas, cada uma com um agente novo que recebeu só a spec, AGENTS.md,
ADRs/padrões e os SHAs imutáveis (sem conversa nem justificativas da implementação;
sem `verification/`, `handoff.md` ou este relatório):

1. **2c0f491** (épica integrada): 0 bloqueadores, 0 altos, 4 médios (M1–M4), 7 baixos (B1–B7).
2. **96e444b** (correções, PR #223): confirmou M1, M3, B1–B4, B6, B7; M2 resolvido com
   ajuste de requisito; M4 parcial; novos N1, N2 (médios) e L1–L5 (baixos).
3. **a5fe1d3** (correções): confirmou N1, N2, L1, L2, L5; L3/L4 parciais. Nenhum
   bloqueador, alto ou médio. Restaram R1–R6 e a sugestão S1, todos baixos/opcionais.

Cobertura: requisitos dos quatro deltas, arquitetura (sem mudança de backend),
integração (operador, compose, systemd, borda, loader, CI), segurança (segredos,
borda, allowlists, OAuth), persistência (backup, restauração, release, rollback,
promote), testes e manutenção.

## Achados e triagem

| ID | Severidade | Disposição | Resumo |
| --- | --- | --- | --- |
| M1 | médio | corrigido | Limite de login admin global atrás do proxy → allowlist administrativa na borda + `limit_req` por IP real |
| M2 | médio | corrigido | `default_server` da porta 80 versionado (fase `open`); 443 fica no `central` por decisão registrada |
| M3 | médio | corrigido | `editorial-cli` removido da entrega (#221) |
| M4 | médio | corrigido | Compose fixado por release (SHA-256) |
| N1 | médio | corrigido | `release`/`rollback` validam o compose antes de parar serviços |
| N2 | médio | corrigido | Comando `promote` para sair do modo ensaio; executado no corte |
| B1–B4, B6, B7, L1–L5 | baixo | corrigidos | CI, textos, imagem PG, `.partial`/`pg_restore --list`, destino de importação, lock órfão, instalador, loader |
| B5 | baixo | adiado (#221) | Retomada do loader quando a resposta de um lote se perde; limitação documentada |
| R1 | baixo | adiado | `tasks.md` 5.2 ainda diz HTTPS; spec/design corretos; ajuste pós-corte |
| R2 | baixo | adiado (#221) | Pacotes não comprovadamente do commit; carga feita com checkout limpo no commit |
| R3 | baixo | adiado | `promote` fora do checklist; cumprido e registrado no corte |
| R4 | baixo | adiado (#221, prioritário) | `rollback` recusado por schema deixa api/web parados |
| R5 | baixo | adiado (#221) | Tratamento de erros do loader |
| R6 | baixo | adiado (#221, prioritário) | Fases `http`/`https` não revertem link se `nginx -t` falhar |
| S1 | opcional | adiado (#221) | `release` reconferir `on_main` com git |

Nenhum bloqueador foi adiado. Os adiamentos mantêm o diff revisado congelado; cada
um está descrito em `spec-review.json` e na #221.

## Evidências e limitações

- Testes: 67 testes de produção (`test_vps_*.py`) e os locais (`test_local_app*.py`)
  passando em a5fe1d3; OpenSpec 1.15.0 `--strict` válido; `nginx -t` das configs
  da borda em container descartável.
- Ensaios reais na VPS (grupos 1–6): reboot, borda/TLS, backup e restauração em alvo
  separado, troca de release e rollback, carga de 2.587 questões; ver `verification/`.
- Revisores não executaram código, Docker, nginx, banco nem VPS. O esforço medium vem
  do perfil e não pôde ser confirmado pelo próprio modelo.
- Testes de configuração são estruturais; o comportamento real foi conferido pelo
  coordenador nos ensaios. Fora da entrega: Google ligado, acervo de outubro e itens da #221.

## Revalidação

Qualquer mudança de base (`main`) ou do diff, exceto nestes dois arquivos, invalida
este registro. Antes do merge do #224, o coordenador executa
`validate_report(report, explanation, base, head, "vps-staging-delivery", 224)` com
os SHAs reais da `main` e da épica (head revisado deve ser ancestral do head do PR,
com o mesmo digest) e confere os checks. A aprovação de merge, a publicação e o corte
exigem autorização específica do usuário.
