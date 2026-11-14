# Grupo 7 — abertura e gate: evidências

Executado em 2026-11-13 e 2026-11-14 (UTC). As etapas na conta do projeto foram
feitas pelo assistente como `concurso-dev`. As fases da borda, com sudo, foram
executadas pelo usuário na conta administrativa.

## 7.1 e 7.2 — revisão e merge

- Revisão final em três rodadas com `spec-reviewer` (Claude Opus 5.5, medium), em
  contexto novo e somente leitura. Registro em `spec-review.json` e `spec-review.md`,
  com head revisado `a5fe1d3`, nenhum bloqueador e somente itens baixos adiados (#221).
- Antes do merge: `validate_report` com os SHAs reais de `main` e da épica, e CI 12/12.
- Merge autorizado pelo usuário: #223 na épica, depois o `[SPEC]` #224 em `main` (`d68a82f`).

## 7.3 — corte

- Release `d68a82f10a99` construída a partir da `main` (`on_main: true`, compose
  fixado com SHA-256). O `release` fez o backup automático antes da troca.
- `promote` executado (R3): `rehearsal: false` e release anterior de ensaio descartada
  como alvo de rollback. Release de ensaio passou a ser recusada.
- Borda (usuário, com sudo, a partir de `infra/vps-production/edge` em `d68a82f`):
  - `nginx -T` antes da abertura: um único `default_server` na porta 80; como o `open` passou no `nginx -t`,
    era o do acesso antigo pelo IP, removido na mesma fase.
  - `admin <IP>`: allowlist administrativa gravada; `nginx -t` ok.
  - `https`: vhost com proteção da área admin e limite de login; `nginx -t` ok.
  - `open`: allowlist pública esvaziada (anterior preservada em `.bak-20261114T021740Z`),
    link de `concurso-public.conf` removido de `sites-enabled` (arquivo mantido em
    `sites-available`), `default_server` próprio na porta 80; `nginx -t` ok.
- O volume e o projeto antigos foram preservados até decisão do usuário (#221).

## 7.4 — checklist público (2026-11-14, de fora da VPS)

| Verificação | Resultado |
| --- | --- |
| `https://concurso-simulator.reemsoftworks.com/` | 200, `Strict-Transport-Security: max-age=31536000` |
| `/api/v1/status`, `/api/v1/csrf`, `/api/v1/auth/config` | 200; `googleEnabled: false` |
| Rotas inexistentes da API | 403 da aplicação (negação padrão) |
| `https://reemsoftworks.com/` | 302 para o app |
| `.com.br` (raiz, `www`, `concurso-simulator`) | 301 para o mesmo host e caminho em `.com` |
| `http://` em qualquer nome | 301 para `https://` |
| `http://<IP>/` e Host desconhecido na porta 80 | conexão fechada sem resposta (444) |
| `https://<IP>/` (`central`) | 200, preservado |
| Área admin a partir de IP fora da allowlist (a própria VPS) | 403 do nginx; páginas públicas 200 |
| Login admin, POST repetido a partir do IP liberado | 6 respostas 403 da aplicação (sem CSRF), depois 429 do nginx (`burst=5`) |
| Interface | início em 390 px e simulados na largura desktop do painel renderizados; demais telas não repetidas |
| Simulado de 5 questões como visitante | `POST /api/v1/attempts` 201, questões exibidas |
| `status` na VPS | api, postgres e web `healthy` |

O acervo segue com 2.587 questões elegíveis (grupo 5).

## Fora desta entrega (#221)

- Login Google: segredos preparados, mas desligado (`googleEnabled: false`).
- Conjuntos de questões de outubro e as 250 estáticas.
- Reboot após a abertura. O reboot foi verificado no grupo 1, com a borda HTTP e o
  serviço de usuário, mas não repetido com a configuração final.
- Itens baixos da revisão (B5, R2, R4–R6, S1), endurecimento de SSH e limpeza do
  projeto e das branches antigos.

## Verificado × integrado

Os grupos 1–7 foram implementados e verificados, e estão integrados em `main` por #224.
Este registro pós-corte só altera documentação da spec (`tasks.md` e este arquivo).
Ele não muda código, não reabre a revisão e não arquiva a spec. O arquivamento
OpenSpec exige autorização própria.
