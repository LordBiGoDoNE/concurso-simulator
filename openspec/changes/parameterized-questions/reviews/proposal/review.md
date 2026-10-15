# Revisão documental da proposta — PR #87

**Concluída/revalidada, sem achados abertos.** Modelo de referência
`openai/gpt-6.1-sol#xhigh`, sessão nova `/root/parameterized_proposal_sol`, explicitamente
selecionada no Codex com `fork_turns=none`, `model=gpt-6.1-sol`, `reasoning_effort=xhigh`.
Mesmo contexto retomado na revalidação. Não é gate cumulativo de produto.

## Escopo e risco

PR #87: `task/parameterized-questions/proposal` → `epic/parameterized-questions`,
fora de stack. Risco **crítico** pelo efeito futuro dos requisitos de elegibilidade,
proteção de respostas, atomicidade, persistência/histórico e replay.

| Fase | Base | Head | Digest canônico |
| --- | --- | --- | --- |
| Inicial | `0d0fc4848f55fc93c736aa76dcfbb73a7269eb39` | `655cde1296b28345384fe8ef5fd54dbdc11ab547` | `077c35efd0da79eb37d1e05ba0d7885cf31c590fcd16c258bba741d8b2843eea` |
| Revalidação | mesma base | `d80e00b20fff26cfedecf1b343aee36270fa4303` | `9e17d04714027bafdfb94ed3cf9d1488c28e94bc262280b8e8169967c266b06d` |

Sete arquivos documentais completos, 504 inserções/2 remoções após correções.
Digests excluem somente os dois `spec-review.*` do gate futuro; estes registros de
evidência são posteriores ao head analisado e não são declarados parte desse digest.
Alterações posteriores de requisitos/fontes exigem nova revalidação.

## Achados e triagem

Inicial: nenhum bloqueador, duas recomendações confirmadas. Ambas corrigidas em
`d80e00b`; revalidação examinou correções e diff completo contra a mesma base.

- **P5-PROP-R1 — fixed:** faltava definir sucessão da versão elegível. Agora há
  ponteiro único separado de aprovações históricas, troca atômica com versão esperada,
  retirada sem reativação anterior e regressões de concorrência. Design/requisito e
  tarefas 1.4/2.2 coerentes com invariantes do banco.
- **P5-PROP-R2 — fixed:** incluir false na canonização quebraria replay do hash V5.
  Agora criação fixa preserva hash antigo; true usa discriminador versionado e mesma
  chave retorna 409. Design/requisito/tarefa 3.3 exigem comando realmente produzido
  pelo V5, separando compatibilidade do hash e do JSON, sem reescrever histórico.

Revalidação: nenhum novo achado ou recomendação aberta. “Corrigido” refere-se aos
documentos; regressões de produto ainda serão implementadas nos grupos autorizados.

## Cobertura e evidências

Requisitos/domínios/checklist completos; ADR 0001, AGENTS, checklist HTTP e revisão;
código real de composição/tentativa/UseCases/portas/JPA/pools e contratos/validadores
frontend; V4/V5 e testes relevantes de domínio/aplicação/persistência/schema/ArchUnit.
Cobertura detalhada de sete áreas e cada achado em `review.json`.

Revisor confirmou hashes por leitura do diff e não executou testes/build/DB/rede nem
escreveu arquivos. Comando de snapshot foi rejeitado pela aprovação automática;
hashing direto do stdout binário de Git foi autorizado e resolveu a verificação.
Coordenador executou OpenSpec offline estrito **5/5 PASS**, diffcheck e conferência de
**111 combinações propostas / 24 tarefas pendentes**. CI remoto é conferido no PR.

## Limitações e próxima decisão

Sem código de produto, conteúdo das variantes, bases reais ou aprovação editorial
validados nesta revisão. Contagem futura e manifesto não equivalem a prova de correção.
Modelo/variante efetivos disponíveis são os informados pelo orquestrador, sem outro
ID de provedor ou telemetria de tokens expostos. Medições mínimas privadas antes/depois
ficam fora do Git, conforme `docs/review-quota.md`; não foram inventados deltas.

O usuário escolheu três famílias. A proposta completa, seu merge e grupos de execução
ainda exigem decisões próprias. Gate em `spec-review.json/md` permanece pending;
sem merge/main/deploy/arquivamento ou implementação automaticamente autorizados.
