# Tasks

**Proposta aprovada pelo usuário em 2026-10-14, issue #64; 4/24 implementadas.** Pré-requisito cumprido: question-bank integrado pelo #63 (`d65006e`). Épica desta spec criada da épica geral atualizada; lote autorizado inclui **1.1–1.4 e 2.1–2.4 completos**, sem merge presumido da implementação. Grupos 3–6 aguardam pedido próprio. Visitante temporário, conta com salvar/retomar e os requisitos desta proposta aprovados.

Cada grupo inclui seus próprios código/testes/docs. Tarefas pequenas/coesas em PRs
revisáveis (preferência 300–500 linhas autorais), sem reunir toda a spec num PR.
Antes de aplicar, o usuário escolhe os grupos completos do lote, sem prefixos parciais.
`[x]` futuro registra implementado/verificado na branch, não aprovação/merge na épica.

## 1. Tentativa e regras de aplicação

- [x] 1.1 Definir composição por cargo/disciplina/assuntos e limites 1–100; testar critérios incompatíveis/quantidades/insuficiência em Java puro e documentar pontuação/distribuição sem regra oficial presumida.
- [x] 1.2 Implementar agregado/itens/respostas e snapshot de versões/ordens/parâmetros; testar identidades distintas, associação de alternativa, limpar resposta e imutabilidade, com modelo documentado.
- [x] 1.3 Implementar conclusão e correção uniforme-v1; testar respostas em branco, 3/5=60%, arredondamento, irreversibilidade e independência de mudanças do banco, com regras de resultado documentadas.
- [x] 1.4 Implementar UseCases/portas de criação, resposta, conclusão, leitura e histórico com ator/posse/versão/operação/relógio/sorteio substituíveis; testar autorização de negócio e retries sem framework, documentando responsabilidades.

## 2. Persistência, composição real e duração — depende de 1

- [ ] 2.1 Criar migração aditiva/JPA/repository de tentativas, itens e comandos, sem editar V1–V4; testar constraints/novo banco/upgrade PG18.6 preservando contas/sessões/acervo, documentando schema e rollback não destrutivo.
- [ ] 2.2 Implementar contrato interno mínimo do banco e captura/seleção transacional das versões elegíveis; testar sem reposição, filtros, insuficiência atômica e retirada concorrente antes/depois do snapshot, com fronteira de módulos documentada.
- [ ] 2.3 Implementar persistência de comandos/idempotência e compare-and-set; testar duas abas/criações/conclusões concorrentes, timeout/retry, hash divergente, rollback integral e propagação de falhas desconhecidas, documentando limites.
- [ ] 2.4 Implementar consultas paginadas por owner e limpeza visitante por vencimento/startup; testar 24h/limite de uma hora, reinício, cascata dos dados temporários e preservação da conta, com operação documentada e relógio controlável.

## 3. Ator técnico, segurança e HTTP — depende de 1–2

- [ ] 3.1 Resolver ator tipado da sessão/identidade verificada e proteger rotas/CSRF/CORS sem duplicar rejeições no controller; testar cadeia real visitante/conectado/expirado/principal inválido e ausência de owner vindo do cliente, documentando checklist HTTP.
- [ ] 3.2 Expor criação/consulta/respostas com DTOs tipados e OpenAPI, sem campos corretivos; testar 200/201/400/403/404/409/503, campos extras e negativo controlado de vazamento/posse, com contratos documentados.
- [ ] 3.3 Expor conclusão/resultado e proteger transições; testar resultado precoce 409, dono incorreto 404 indistinguível, correção da versão capturada e retry de conclusão sem novo efeito, com exemplos documentados.
- [ ] 3.4 Expor histórico privado/retomada com paginação/ordem/no-store; testar outra conta, visitante 401, cookies/CSRF, indisponibilidade e isolamento da consulta stateless existente, com regressões OIDC/logout/readiness e ArchUnit.

## 4. Interface de composição e realização — depende de 3

- [ ] 4.1 Criar cliente tipado com prazo abrangendo JSON e composição acessível; testar validação/insuficiência, duplo clique/retry da mesma criação e pedidos obsoletos, documentando uso e limites.
- [ ] 4.2 Implementar navegação/resposta/limpeza e fila de salvamentos com versão/commandId; testar pendente/salvo/falhou/409, preservação em memória da escolha não confirmada e ordem estável, documentando recuperação.
- [ ] 4.3 Implementar confirmação de conclusão e resultado/resoluções/fontes; testar bloqueio com salvamentos pendentes, resultado precoce, campos corretivos só após concluir e nota exclusivamente do backend, documentando o fluxo.
- [ ] 4.4 Verificar teclado/foco/radios/progresso em 390/1280, falha/retry e acesso às aulas; testes browser com fixtures e negativo de gabarito em JSON/DOM/assets, com limites da proteção documentados.

## 5. Histórico e contextos de identidade — depende de 3–4

- [ ] 5.1 Implementar lista privada paginada e retomada da conta sem novo sorteio; testar estados/ordem, página vazia e ausência de nota parcial, com orientação de respostas confirmadas documentada.
- [ ] 5.2 Integrar retomada entre sessões da mesma conta; testar navegador real com OIDC local/PG e duas contas, preservando ordens e respostas, com fronteira de posse documentada sem credenciais Google reais.
- [ ] 5.3 Implementar aviso do modo visitante, expiração e login sem transferência automática; testar visitante→login, logout/troca de conta e tentativa inacessível, com duração/limpeza/ausência de mescla documentadas.
- [ ] 5.4 Invalidar conteúdo privado e respostas tardias em mudança de identidade; testar logout/401/retorno tardio, storage/cache vazios e aulas funcionais, com regressões de sessão e limitações offline documentadas.

## 6. Verificação da combinação — depende de 1–5 completos

- [ ] 6.1 Exercitar upgrade/reinício/rollback operacional sobre base com identidades/sessões/acervo e tentativas; conferir integridade, duração visitante e retomada real, registrando comandos/resultados sem dados privados.
- [ ] 6.2 Executar E2E completo visitante e conta, duas abas/contas e mudança editorial após início; comprovar seleção/correção histórica, isolamento e conflitos, sem usar acervo real aprovado como fixture de CI.
- [ ] 6.3 Executar builds/testes/ArchUnit/HTTP/browser/OpenSpec e regressão das aulas/URLs/offline; inspecionar JAR/bundle/contratos contra fixtures/segredos/gabarito antecipado e comprovar CI sem deploy.
- [ ] 6.4 Consolidar matriz requisitos→testes, PRs/SHAs/checks, limites e achados triados da combinação; manter issue distinguindo implementado de integrado e preparar evidência para o gate cumulativo sem inventar sua execução.

## Workflow follow-up

- Publicar os artefatos aprovados da issue #64 na branch correta; lote 1/2 já escolhido explicitamente. Novos grupos e merges da implementação continuam exigindo autorização própria.
- Usar Stacks v0.2.1 apenas em tarefas realmente dependentes desta spec, drafts com trunk `epic/mock-exam-attempts`, código/testes/docs coesos; conferir metadados/checks a cada camada e preservar refs.
- Revisão independente SOL 6.1 xhigh por lote, observações mínimas de cota antes/depois e nenhuma geração concorrente; integrações somente com aprovação explícita dos PRs/heads.
- Ao concluir a spec, abrir `[SPEC]` para a épica geral e executar gate cumulativo Astra medium em contexto novo; aprovação de tarefas não autoriza esse merge.
- Não arquivar, publicar, integrar main ou iniciar as etapas 5/6/deployment automaticamente.
