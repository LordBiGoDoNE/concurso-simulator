# Login Google em produção — roteiro

Grupo 4 da #156. O client é criado pelo usuário; o client ID e o secret só existem
no Google Cloud e nos arquivos privados da VPS. Nunca cole o secret em chat, issue,
commit ou argumento de comando.

## 1. Google Cloud Console

1. Em <https://console.cloud.google.com/>, crie ou escolha um projeto (ex.: `reemsoftworks`).
2. **APIs e serviços → Tela de consentimento OAuth** (Google Auth Platform → Branding/Audience):
   - Tipo de usuário: **Externo**.
   - Nome do app: `Concurso Simulator`; e-mail de suporte e de contato do desenvolvedor: o seu.
   - Domínio autorizado: `reemsoftworks.com`.
   - Página inicial: `https://concurso-simulator.reemsoftworks.com`.
   - Escopos: nenhum além dos básicos. A aplicação pede só `openid` e `profile`, que
     não exigem verificação do Google.
   - **Público → Publicar app (Em produção)**. Em "Teste", só usuários de teste
     cadastrados conseguem entrar.
3. **APIs e serviços → Credenciais → Criar credenciais → ID do cliente OAuth**:
   - Tipo: **Aplicativo da Web**; nome: `concurso-simulator-prod`.
   - Origens JavaScript autorizadas: `https://concurso-simulator.reemsoftworks.com`
   - URIs de redirecionamento autorizados (exato):
     `https://concurso-simulator.reemsoftworks.com/login/oauth2/code/google`
   - Guarde o client ID e o secret apenas até o passo 2.

## 2. Configurar na VPS (usuário, terminal interativo)

```sh
ssh -t -i ~/.ssh/hostingerVPS -o IdentitiesOnly=yes concurso-dev@<VPS>
export DOCKER_CONTEXT=rootless
cd ~/projects/production
python3 scripts/vps_production.py configure-google
```

O comando pede o client ID e o secret (o secret não aparece na tela), valida o
formato, grava os arquivos privados, reinicia a API e confere `googleEnabled`.
Se a API recusar a configuração, o Google volta a ficar desligado sozinho.
Para desligar depois: `python3 scripts/vps_production.py disable-google`.

## 3. Smoke real (antes da abertura)

Do IP liberado na allowlist, em `https://concurso-simulator.reemsoftworks.com`:

1. Visitante: aulas acessíveis, `/api/v1/auth/config` com `googleEnabled:true`, `/api/v1/me` 401.
2. Entrar com Google: consentimento mínimo (nome e foto), retorno à aplicação conectada,
   `/api/v1/me` 200.
3. Cookie `SESSION` HttpOnly, Secure, SameSite=Lax; nenhum token na URL final ou no storage.
4. Sair: `/api/v1/me` 401. Entrar de novo: mesma conta.
5. Cancelar o consentimento: mensagem genérica, aulas continuam acessíveis.
6. Reiniciar a API e confirmar que a conta e o histórico continuam.
