# Grupo 2 — runtime de produção: evidências

Executado em 2026-11-13 pelo assistente como `concurso-dev`. Release de ensaio
`6079e913de9f` (branch de tarefa, estado marcado `rehearsal: true`).

## Testes automatizados

- `test_vps_production_runtime.py`: 15 testes (allowlist do contexto, Compose,
  nginx interno, Dockerfile, estado/segredos, recusas, lock).
- `test_local_app*.py`: 24 testes, 1 ignorado no Windows, sem regressão após os
  parâmetros opcionais de `restricted_tar`/`committed_context`/`owned`.

## Ensaio na VPS

- Clone dedicado `~/projects/production`; o checkout antigo e o projeto
  `concurso-simulator-app-local-vps` não foram alterados.
- `build`: 1m08s, manifesto com `on_main=false`.
- `init` sem `--rehearsal` recusado; com `--rehearsal`, estado e `secrets/` em 0700.
- `start`: três serviços healthy em 18 s. Portas: web `127.0.0.1:18090`; API e PG sem portas.
- Via loopback com Host canônico: SPA 200, `/material/` 200, `/api/v1/status` UP,
  `auth/config` `googleEnabled:false`, Host desconhecido 421, `X-Forwarded-Host` forjado ignorado.
- `Set-Cookie: SESSION; Path=/; Secure; HttpOnly; SameSite=Lax`; POST sem CSRF 403.
- O bundle não contém `127.0.0.1` nem origem absoluta do material: a main atual
  não usa mais `VITE_MATERIAL_URL`, então o `sub_filter` antigo é desnecessário.
- Serviço de usuário `concurso-production.service` habilitado; após o reboot do
  grupo 1 a aplicação voltou healthy sem intervenção.
- A senha inicial do admin existe em `secrets/admin_password`; não foi lida nem impressa.

## Limites

Release de ensaio, não da main. Atualização de release, backup e restauração são do grupo 6.
